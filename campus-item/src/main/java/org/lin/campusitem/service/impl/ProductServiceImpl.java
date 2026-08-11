package org.lin.campusitem.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.lin.common.context.PageResult;
import org.lin.common.context.ProductInfo;
import org.lin.common.enums.ProductStatus;
import org.lin.common.enums.UserRole;
import org.lin.common.kafka.KafkaProducerService;
import org.lin.common.kafka.KafkaTopicConstants;
import org.lin.common.oss.OssUtil;
import org.lin.common.result.Result;
import org.lin.common.util.RedisUtils;
import org.lin.common.entity.Category;
import org.lin.common.entity.Image;
import org.lin.common.entity.Product;
import org.lin.common.entity.User;
import org.lin.campusitem.kafka.dto.ProductPublishPayload;
import org.lin.campusitem.kafka.dto.ViewCountPayload;
import org.lin.campusitem.mapper.CategoryMapper;
import org.lin.campusitem.mapper.ImageMapper;
import org.lin.campusitem.mapper.ProductMapper;
import org.lin.campusitem.mapper.UserMapper;
import org.lin.campusitem.service.ProductService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private ImageMapper imageMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OssUtil ossUtil;

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${campus.user.base-url:http://localhost:8082}")
    private String userServiceBaseUrl;

    /** 浏览量 Redis 计数 Key 前缀，配合 productId 使用 */
    private static final String VIEW_COUNT_KEY = "product:view:count:";

    /** 浏览量去重 Key 前缀，配合 productId:visitorId 使用，TTL 1 小时 */
    private static final String VIEW_DEDUP_KEY = "product:view:dedup:";

    /** 去重时间窗口（秒），同一访客 1 小时内重复访问不计数 */
    private static final long DEDUP_WINDOW_SECONDS = 3600;

    /** 商品详情缓存 Key 前缀 */
    private static final String PRODUCT_CACHE_KEY = "product:detail:";

    /** 缓存 TTL（分钟） */
    private static final long CACHE_TTL_MINUTES = 10;

    @Override
    public Result<PageResult<ProductInfo>> getProductList(Long current, Long size) {
        Page<Product> page = new Page<>(current, size);
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Product::getStatus, ProductStatus.ON_SALE.getCode())
                    .eq(Product::getIsDeleted, 0)
                    .orderByDesc(Product::getCreateTime);

        IPage<Product> resultPage = productMapper.selectPage(page, queryWrapper);
        return Result.success(convertToPageResult(resultPage));
    }

    @Override
    public Result<ProductInfo> getProductDetail(Long productId) {
        // 1. 先查缓存（含商品基本信息+图片+分类名，不含卖家信息）
        ProductInfo cached = getProductFromCache(productId);
        if (cached != null) {
            // 缓存命中，只需补充卖家信息
            Product product = productMapper.findByProductId(productId);
            if (product != null) {
                fillSellerInfo(cached, product.getPublishUserId());
            }
            return Result.success(cached);
        }

        // 2. 缓存未命中，查 DB
        Product product = productMapper.findByProductId(productId);
        if (product == null) {
            return Result.error(404, "商品不存在");
        }

        ProductInfo info = convertToProductInfo(product);
        info.setImages(getProductImageUrls(productId));
        fillCategoryName(info, product);

        if (info.getImages() != null && !info.getImages().isEmpty()) {
            info.setImageUrl(info.getImages().get(0));
        }

        // 3. 写入缓存（不含卖家信息，卖家信息每次实时查）
        cacheProduct(info);

        fillSellerInfo(info, product.getPublishUserId());

        return Result.success(info);
    }

    /**
     * 内部接口专用：只查商品基本信息+图片，不查卖家信息，避免嵌套跨服务调用。
     */
    @Override
    public Result<ProductInfo> getProductForInternal(Long productId) {
        // 1. 先查缓存
        ProductInfo cached = getProductFromCache(productId);
        if (cached != null && cached.getSellerId() != null) {
            // 缓存命中且关键字段完整
            return Result.success(cached);
        }
        if (cached != null) {
            // 缓存存在但关键字段缺失（如旧版本缓存无sellerId），清除后重查
            log.warn("[商品缓存] 缓存数据不完整 productId={} sellerId为空，重新查询DB", productId);
            try {
                redisUtils.delete(PRODUCT_CACHE_KEY + productId);
            } catch (Exception ignored) {
            }
        }

        // 2. 缓存未命中或数据不完整，查 DB
        Product product = productMapper.findByProductId(productId);
        if (product == null) {
            return Result.error(404, "商品不存在");
        }

        ProductInfo info = convertToProductInfo(product);
        info.setImages(getProductImageUrls(productId));
        fillCategoryName(info, product);

        if (info.getImages() != null && !info.getImages().isEmpty()) {
            info.setImageUrl(info.getImages().get(0));
        }

        // 3. 写入缓存
        cacheProduct(info);

        return Result.success(info);
    }

    @Override
    public Result<List<Category>> getCategories() {
        return Result.success(categoryMapper.findAllActive());
    }

    @Override
    public Result<PageResult<ProductInfo>> searchProducts(Integer categoryId, String keyword, Long current, Long size) {
        Page<Product> page = new Page<>(current, size);
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Product::getStatus, ProductStatus.ON_SALE.getCode())
                    .eq(Product::getIsDeleted, 0);

        if (categoryId != null) {
            queryWrapper.eq(Product::getCategoryId, categoryId);
        }

        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.like(Product::getTitle, keyword);
        }

        queryWrapper.orderByDesc(Product::getCreateTime);

        IPage<Product> resultPage = productMapper.selectPage(page, queryWrapper);
        return Result.success(convertToPageResult(resultPage));
    }

    @Override
    @Transactional
    public Result<?> publishProduct(Product product, List<String> images) {
        product.setStatus(ProductStatus.ON_SALE.getCode());
        product.setViewCount(0);
        product.setCreateTime(new Date());
        product.setUpdateTime(new Date());
        product.setIsDeleted(0);
        if (product.getQuantity() == null || product.getQuantity() < 1) {
            product.setQuantity(1);
        }

        // 主信息同步落库，保证立即可见
        productMapper.insert(product);

        // 图片记录 + 粉丝通知走 Kafka 异步处理，提升接口响应速度
        ProductPublishPayload payload = ProductPublishPayload.builder()
                .productId(product.getProductId())
                .publishUserId(product.getPublishUserId())
                .title(product.getTitle())
                .price(product.getPrice() != null ? product.getPrice().doubleValue() : null)
                .imageUrls(images)
                .hasImages(images != null && !images.isEmpty())
                .build();
        kafkaProducerService.sendAsync(KafkaTopicConstants.PRODUCT_PUBLISHED, "publish", payload);

        return Result.success();
    }

    @Override
    @Transactional
    public Result<?> publishProductWithImages(Product product, List<MultipartFile> files, Long userId) {
        product.setPublishUserId(userId);
        product.setStatus(ProductStatus.ON_SALE.getCode());
        product.setViewCount(0);
        product.setCreateTime(new Date());
        product.setUpdateTime(new Date());
        product.setIsDeleted(0);
        if (product.getQuantity() == null || product.getQuantity() < 1) {
            product.setQuantity(1);
        }

        // 主信息同步落库
        productMapper.insert(product);

        // OSS 上传必须同步（MultipartFile 文件流无法跨方法/事务传递），
        // 但 Image 表记录的落库改由 Kafka 消费者异步处理
        List<String> imageUrls = new ArrayList<>();
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) {
                    continue;
                }
                String imageUrl = ossUtil.upload(file, "products");
                imageUrls.add(imageUrl);
            }
        }

        // 投递事件：消费者异步落 Image 表 + 推送粉丝通知
        ProductPublishPayload payload = ProductPublishPayload.builder()
                .productId(product.getProductId())
                .publishUserId(userId)
                .title(product.getTitle())
                .price(product.getPrice() != null ? product.getPrice().doubleValue() : null)
                .imageUrls(imageUrls)
                .hasImages(!imageUrls.isEmpty())
                .build();
        kafkaProducerService.sendAsync(KafkaTopicConstants.PRODUCT_PUBLISHED, "publish", payload);

        return Result.success();
    }

    @Override
    @Transactional
    public Result<?> editProduct(Product product, List<String> images, Long userId) {
        Product existing = productMapper.findByProductId(product.getProductId());
        if (existing == null) {
            return Result.error(404, "商品不存在");
        }
        if (!existing.getPublishUserId().equals(userId)) {
            return Result.error(403, "无权修改此商品");
        }

        existing.setTitle(product.getTitle());
        existing.setDescription(product.getDescription());
        existing.setPrice(product.getPrice());
        existing.setOriginalPrice(product.getOriginalPrice());
        existing.setQuantity(product.getQuantity());
        existing.setTradeType(product.getTradeType());
        existing.setCategoryId(product.getCategoryId());
        existing.setUpdateTime(new Date());

        productMapper.updateById(existing);

        // 商品信息变更，清除缓存
        evictProductCache(product.getProductId());

        if (images != null) {
            imageMapper.deleteByTypeAndRelationId(Image.TYPE_PRODUCT, product.getProductId());

            int sortOrder = 0;
            for (String imageUrl : images) {
                Image image = new Image();
                image.setType(Image.TYPE_PRODUCT);
                image.setRelationId(product.getProductId());
                image.setImageUrl(imageUrl);
                image.setSortOrder(sortOrder++);
                image.setCreateTime(new Date());
                imageMapper.insert(image);
            }
        }

        return Result.success();
    }

    @Override
    @Transactional
    public Result<?> offShelfProduct(Long productId, Long userId) {
        Product product = productMapper.findByProductId(productId);
        if (product == null) {
            return Result.error(404, "商品不存在");
        }
        if (!product.getPublishUserId().equals(userId)) {
            return Result.error(403, "无权操作此商品");
        }

        product.setStatus(ProductStatus.OFF_SHELF.getCode());
        product.setUpdateTime(new Date());
        productMapper.updateById(product);

        // 商品下架，清除缓存
        evictProductCache(productId);

        return Result.success();
    }

    @Override
    @Transactional
    public Result<?> deleteProduct(Long productId, Long userId) {
        Product product = productMapper.findByProductId(productId);
        if (product == null) {
            return Result.error(404, "商品不存在");
        }
        if (!product.getPublishUserId().equals(userId)) {
            return Result.error(403, "无权删除此商品");
        }

        imageMapper.deleteByTypeAndRelationId(Image.TYPE_PRODUCT, productId);
        productMapper.deleteById(productId);

        // 商品删除，清除缓存
        evictProductCache(productId);

        return Result.success();
    }

    @Override
    public Result<PageResult<ProductInfo>> getMyProducts(Long userId, Integer status, Long current, Long size) {
        Page<Product> page = new Page<>(current, size);
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Product::getPublishUserId, userId)
                    .eq(Product::getIsDeleted, 0);

        if (status != null) {
            queryWrapper.eq(Product::getStatus, status);
        }

        queryWrapper.orderByDesc(Product::getCreateTime);

        IPage<Product> resultPage = productMapper.selectPage(page, queryWrapper);
        return Result.success(convertToPageResult(resultPage));
    }

    @Override
    @Transactional
    public Result<?> onShelfProduct(Long productId, Long userId) {
        Product product = productMapper.findByProductId(productId);
        if (product == null) {
            return Result.error(404, "商品不存在");
        }
        if (!product.getPublishUserId().equals(userId)) {
            return Result.error(403, "无权操作此商品");
        }

        product.setStatus(ProductStatus.ON_SALE.getCode());
        product.setUpdateTime(new Date());
        productMapper.updateById(product);

        // 商品上架，清除缓存
        evictProductCache(productId);

        return Result.success();
    }

    @Override
    public Result<PageResult<ProductInfo>> getProductsByUserId(Long userId, Long current, Long size) {
        Page<Product> page = new Page<>(current, size);
        IPage<Product> resultPage = productMapper.selectPageByUserId(page, userId);
        return Result.success(convertToPageResult(resultPage));
    }

    @Override
    public Result<?> incrementViewCount(Long productId, String visitorId) {
        // 基于 Redis 的 1 小时时间窗口去重：同一访客对同一商品 1 小时内只计一次
        String dedupKey = VIEW_DEDUP_KEY + productId + ":" + visitorId;
        boolean isNewView = redisUtils.tryLock(dedupKey, DEDUP_WINDOW_SECONDS);
        if (!isNewView) {
            // 去重窗口内的重复访问，不计浏览量
            return Result.success();
        }

        // Redis 实时计数，保证详情页接口响应速度
        String key = VIEW_COUNT_KEY + productId;
        redisUtils.incrementCount(key);

        // 投递事件到 Kafka，消费者批量聚合后落库 DB（避免每次浏览都打 DB）
        ViewCountPayload payload = ViewCountPayload.builder()
                .productId(productId)
                .count(1)
                .build();
        kafkaProducerService.sendAsync(KafkaTopicConstants.PRODUCT_VIEW_COUNT, "view", payload);

        return Result.success();
    }

    @Override
    public Result<List<ProductInfo>> getHotProducts() {
        List<Product> products = productMapper.findHotProducts();
        // 批量组装图片+分类，消除逐商品查询的 N+1
        return Result.success(convertToProductInfos(products));
    }

    @Override
    public Result<List<ProductInfo>> getProductsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.success(new ArrayList<>());
        }
        List<Product> products = productMapper.selectBatchIds(ids);
        // 批量组装图片+分类，消除逐商品查询的 N+1
        return Result.success(convertToProductInfos(products));
    }

    @Override
    public int reduceStock(Long productId, Integer quantity) {
        int affected = productMapper.reduceStock(productId, quantity);
        // 扣减后清除商品缓存，避免订单服务读到旧的库存数据
        if (affected > 0) {
            evictProductCache(productId);
        }
        return affected;
    }

    @Override
    public int restoreStock(Long productId, Integer quantity) {
        int affected = productMapper.restoreStock(productId, quantity);
        // 恢复后清除商品缓存，避免订单服务读到旧的库存数据
        if (affected > 0) {
            evictProductCache(productId);
        }
        return affected;
    }

    private PageResult<ProductInfo> convertToPageResult(IPage<Product> page) {
        PageResult<ProductInfo> result = new PageResult<>();
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        result.setTotal(page.getTotal());
        result.setPages(page.getPages());

        List<ProductInfo> records = convertToProductInfos(page.getRecords());
        result.setRecords(records);

        return result;
    }

    /**
     * 批量组装商品信息（图片 + 分类名），消除列表场景的 N+1 查询。
     * <p>
     * 原来每个商品都要单独查一次图片（getProductImageUrls）和一次分类（categoryMapper.selectById），
     * 列表页 N 个商品即 2N 条 SQL。这里分别用 1 条 IN 查询批量取回，再在内存中按 ID 归并。
     */
    private List<ProductInfo> convertToProductInfos(List<Product> products) {
        List<ProductInfo> infos = new ArrayList<>();
        if (products == null || products.isEmpty()) {
            return infos;
        }

        // 1. 批量查询商品图片
        List<Long> productIds = products.stream()
                .map(Product::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        Map<Long, List<String>> imageMap = new HashMap<>();
        if (!productIds.isEmpty()) {
            for (Image img : imageMapper.findByTypeAndRelationIds(Image.TYPE_PRODUCT, productIds)) {
                imageMap.computeIfAbsent(img.getRelationId(), k -> new ArrayList<>()).add(img.getImageUrl());
            }
        }

        // 2. 批量查询分类名
        List<Long> categoryIds = products.stream()
                .map(p -> p.getCategoryId() == null ? null : p.getCategoryId().longValue())
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> categoryNameMap = new HashMap<>();
        if (!categoryIds.isEmpty()) {
            for (Category category : categoryMapper.selectBatchIds(categoryIds)) {
                if (category.getCategoryId() != null) {
                    categoryNameMap.put(category.getCategoryId().longValue(), category.getCategoryName());
                }
            }
        }

        // 3. 组装
        for (Product product : products) {
            ProductInfo info = convertToProductInfo(product);
            if (product.getCategoryId() != null) {
                info.setCategoryName(categoryNameMap.get(product.getCategoryId().longValue()));
            }
            List<String> images = imageMap.getOrDefault(product.getProductId(), Collections.emptyList());
            info.setImages(images);
            if (!images.isEmpty()) {
                info.setImageUrl(images.get(0));
            }
            infos.add(info);
        }
        return infos;
    }

    private ProductInfo convertToProductInfo(Product product) {
        ProductInfo info = new ProductInfo();
        BeanUtils.copyProperties(product, info);
        info.setId(product.getProductId());
        info.setName(product.getTitle());
        info.setStock(product.getQuantity());
        // Product.publishUserId → ProductInfo.sellerId（字段名不同，BeanUtils无法自动映射）
        info.setSellerId(product.getPublishUserId());
        // 显式处理价格 BigDecimal → Double，避免 BeanUtils 跳过转换或 null 被设 0
        if (product.getPrice() != null) {
            info.setPrice(product.getPrice().doubleValue());
        }
        if (product.getOriginalPrice() != null) {
            info.setOriginalPrice(product.getOriginalPrice().doubleValue());
        }
        // 分类名由批量组装方法设置（convertToProductInfos），单条路径在调用方单独设置，
        // 这里不做分类查询，避免列表场景的 N+1。
        return info;
    }

    /**
     * 批量填充卖家信息（列表页用），消除逐行 {@link #fillSellerInfo} 的 N+1。
     * <p>
     * 一次批量查本地 sys_user 副本 + 一次批量统计在售商品数，仅对本地缺失的卖家走远程兜底。
     */
    private void fillSellers(List<ProductInfo> infos) {
        List<ProductInfo> need = infos.stream()
                .filter(info -> info.getSellerName() == null && info.getSellerId() != null)
                .collect(Collectors.toList());
        if (need.isEmpty()) {
            return;
        }
        Set<Long> userIds = need.stream()
                .map(ProductInfo::getSellerId)
                .collect(Collectors.toSet());

        // 1. 批量查本地副本
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (User u : userMapper.selectBatchIds(userIds)) {
                userMap.put(u.getUserId(), u);
            }
        }
        // 2. 批量统计每个卖家的在售商品数
        Map<Long, Integer> countMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (Map<String, Object> row : productMapper.countGroupByPublishUserId(userIds)) {
                Number uid = (Number) row.get("publish_user_id");
                Number cnt = (Number) row.get("cnt");
                if (uid != null && cnt != null) {
                    countMap.put(uid.longValue(), cnt.intValue());
                }
            }
        }
        // 3. 填充；本地缺失的卖家走远程兜底
        for (ProductInfo info : need) {
            User seller = userMap.get(info.getSellerId());
            if (seller != null) {
                info.setSellerNickname(seller.getNickname());
                info.setSellerName(seller.getNickname());
                info.setSellerPhone(seller.getPhone());
                info.setSellerAvatar(seller.getAvatar());
                info.setSellerProductCount(countMap.getOrDefault(info.getSellerId(), 0));
            } else {
                fillSellerInfo(info, info.getSellerId());
            }
        }
    }

    /**
     * 单条商品填充分类名（批量列表走 {@link #convertToProductInfos}，无需此方法）。
     */
    private void fillCategoryName(ProductInfo info, Product product) {
        if (product.getCategoryId() != null) {
            Category category = categoryMapper.selectById(product.getCategoryId());
            if (category != null) {
                info.setCategoryName(category.getCategoryName());
            }
        }
    }

    /**
     * 填充卖家信息：优先查本地 sys_user 副本，找不到则远程调用 campus-user 服务
     */
    private void fillSellerInfo(ProductInfo info, Long publishUserId) {
        if (publishUserId == null) {
            return;
        }

        // 1. 先查本地副本（快速路径）
        User seller = userMapper.findByUserId(publishUserId);
        if (seller != null) {
            info.setSellerId(seller.getUserId());
            info.setSellerNickname(seller.getNickname());
            info.setSellerName(seller.getNickname());
            info.setSellerPhone(seller.getPhone());
            info.setSellerAvatar(seller.getAvatar());
            info.setSellerProductCount(productMapper.countByPublishUserId(seller.getUserId()));
            return;
        }

        // 2. 本地没有，远程调用 campus-user 服务（兜底路径）
        try {
            String url = userServiceBaseUrl + "/user/info/" + publishUserId;
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
            if (resp != null && Integer.valueOf(200).equals(resp.get("code"))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) resp.get("data");
                if (data != null) {
                    info.setSellerId(((Number) data.getOrDefault("id", publishUserId)).longValue());
                    String nickname = (String) data.getOrDefault("nickname", "");
                    info.setSellerNickname(nickname);
                    info.setSellerName(nickname);
                    info.setSellerPhone((String) data.getOrDefault("phone", ""));
                    info.setSellerAvatar((String) data.getOrDefault("avatar", ""));
                    info.setSellerProductCount(productMapper.countByPublishUserId(publishUserId));

                    // 同步回本地副本，下次直接命中
                    syncUserToLocal(data);
                }
            }
        } catch (Exception e) {
            log.warn("获取卖家信息失败 userId={}, error={}", publishUserId, e.getMessage());
            // 远程也失败，仍然设置 sellerId 让前端至少能显示"未知用户"
            info.setSellerId(publishUserId);
        }
    }

    /**
     * 将 campus-user 返回的用户信息同步到本地 sys_user 副本
     */
    @SuppressWarnings("unchecked")
    private void syncUserToLocal(Map<String, Object> data) {
        try {
            User local = new User();
            local.setUserId(((Number) data.get("id")).longValue());
            local.setNickname((String) data.getOrDefault("nickname", ""));
            local.setAvatar((String) data.getOrDefault("avatar", ""));
            local.setPhone((String) data.getOrDefault("phone", ""));
            local.setEmail((String) data.getOrDefault("email", ""));
            local.setRole(data.get("role") != null ? ((Number) data.get("role")).intValue() : UserRole.USER.getCode());
            local.setStatus(data.get("status") != null ? ((Number) data.get("status")).intValue() : 1);
            local.setIsDeleted(0);
            // 用 insertOrUpdate 方式同步
            userMapper.insert(local);
        } catch (Exception e) {
            // 同步失败可能是主键冲突，忽略即可
            log.debug("同步用户到本地副本失败(忽略): {}", e.getMessage());
        }
    }

    // ==================== 缓存工具方法 ====================

    /**
     * 从 Redis 获取商品缓存（不含卖家信息）。
     * @return 缓存命中则返回 ProductInfo，否则返回 null
     */
    private ProductInfo getProductFromCache(Long productId) {
        try {
            Object cached = redisUtils.get(PRODUCT_CACHE_KEY + productId);
            if (cached instanceof ProductInfo) {
                return (ProductInfo) cached;
            }
            return null;
        } catch (Exception e) {
            log.warn("读取商品缓存失败 productId={}, error={}", productId, e.getMessage());
            return null;
        }
    }

    /**
     * 写入商品缓存，TTL 10 分钟。卖家信息不缓存（每次实时查）。
     */
    private void cacheProduct(ProductInfo info) {
        try {
            redisUtils.set(PRODUCT_CACHE_KEY + info.getId(), info, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("写入商品缓存失败 productId={}, error={}", info.getId(), e.getMessage());
        }
    }

    /**
     * 清除商品缓存（编辑/上架/下架/删除时调用）。
     */
    private void evictProductCache(Long productId) {
        try {
            redisUtils.delete(PRODUCT_CACHE_KEY + productId);
        } catch (Exception e) {
            log.warn("清除商品缓存失败 productId={}, error={}", productId, e.getMessage());
        }
    }

    private List<String> getProductImageUrls(Long productId) {
        List<Image> images = imageMapper.findByTypeAndRelationId(Image.TYPE_PRODUCT, productId);
        List<String> urls = new ArrayList<>();
        for (Image image : images) {
            urls.add(image.getImageUrl());
        }
        return urls;
    }

    @Override
    public long countAll() {
        return productMapper.countAll();
    }

    @Override
    public long countCategories() {
        return categoryMapper.countAll();
    }

    @Override
    public long countOnSale() {
        return productMapper.countOnSale();
    }

    @Override
    public Result<PageResult<ProductInfo>> adminGetProductList(String keyword, Long current, Long size, Integer status) {
        Page<Product> page = new Page<>(current, size);
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Product::getIsDeleted, 0);

        if (status != null) {
            queryWrapper.eq(Product::getStatus, status);
        }

        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.and(w -> w.like(Product::getTitle, keyword)
                    .or().like(Product::getDescription, keyword));
        }

        queryWrapper.orderByDesc(Product::getCreateTime);

        IPage<Product> resultPage = productMapper.selectPage(page, queryWrapper);
        PageResult<ProductInfo> result = convertToPageResult(resultPage);
        // 管理员列表补充卖家昵称（本地 sys_user 副本优先，兜底远程），批量填充避免逐行 SQL+远程调用
        fillSellers(result.getRecords());
        return Result.success(result);
    }
}