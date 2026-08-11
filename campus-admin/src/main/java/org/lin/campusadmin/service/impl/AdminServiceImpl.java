package org.lin.campusadmin.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.lin.common.context.AdminProductVO;
import org.lin.common.context.PageResult;
import org.lin.common.context.ProductInfo;
import org.lin.common.result.Result;
import org.lin.common.entity.Category;
import org.lin.common.entity.Image;
import org.lin.common.entity.Product;
import org.lin.common.entity.User;
import org.lin.campusadmin.mapper.CategoryMapper;
import org.lin.campusadmin.mapper.ImageMapper;
import org.lin.campusadmin.mapper.OrderMapper;
import org.lin.campusadmin.mapper.ProductMapper;
import org.lin.campusadmin.mapper.UserMapper;
import org.lin.campusadmin.service.AdminService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ImageMapper imageMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${campus.auth.service-url:http://localhost:8081}")
    private String authServiceUrl;

    @Value("${campus.item.service-url:http://localhost:8083}")
    private String itemServiceUrl;

    @Value("${campus.order.service-url:http://localhost:8084}")
    private String orderServiceUrl;

    @Override
    @SuppressWarnings("unchecked")
    public Result<List<Category>> getCategoryList() {
        // 优先从 item 服务取分类（campus_item 为前台展示源，管理端以它为准）
        try {
            Map<String, Object> response = restTemplate.getForObject(itemServiceUrl + "/item/internal/categories", Map.class);
            if (isResponseOk(response)) {
                List<Map<String, Object>> records = (List<Map<String, Object>>) response.get("data");
                List<Category> list = new ArrayList<>();
                if (records != null) {
                    for (Map<String, Object> r : records) {
                        Category c = new Category();
                        c.setCategoryId(toInteger(r.get("categoryId")));
                        c.setCategoryName((String) r.getOrDefault("categoryName", ""));
                        c.setCategoryDesc((String) r.getOrDefault("categoryDesc", ""));
                        c.setSortOrder(toInteger(r.get("sortOrder")));
                        c.setStatus(toInteger(r.get("status")));
                        c.setCreateTime(parseDateTime(r.get("createTime")));
                        c.setUpdateTime(parseDateTime(r.get("updateTime")));
                        c.setIsDeleted(0);
                        list.add(c);
                    }
                }
                return Result.success(list);
            }
            log.warn("商品服务分类接口响应异常: {}", response);
        } catch (Exception e) {
            log.warn("调用商品服务分类接口失败，使用本地分类: {}", e.getMessage());
        }
        return Result.success(categoryMapper.findAll());
    }

    @Override
    public Result<PageResult<User>> getUserList(String keyword, Long current, Long size) {
        Page<User> page = new Page<>(current, size);
        IPage<User> resultPage;
        if (keyword != null && !keyword.isEmpty()) {
            resultPage = userMapper.selectPageByKeyword(page, keyword);
        } else {
            resultPage = userMapper.selectPage(page, null);
        }
        PageResult<User> result = new PageResult<>();
        BeanUtils.copyProperties(resultPage, result);
        return Result.success(result);
    }

    @Override
    @Transactional
    public Result<?> updateUserStatus(Long userId, Integer status) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        user.setStatus(status);
        user.setUpdateTime(new Date());
        userMapper.updateById(user);
        return Result.success();
    }

    @Override
    @Transactional
    public Result<?> updateProduct(Long productId, Product product) {
        Product existing = productMapper.selectById(productId);
        if (existing == null) {
            return Result.error(404, "商品不存在");
        }
        existing.setTitle(product.getTitle());
        existing.setDescription(product.getDescription());
        existing.setPrice(product.getPrice());
        existing.setOriginalPrice(product.getOriginalPrice());
        existing.setQuantity(product.getQuantity());
        existing.setTradeType(product.getTradeType());
        existing.setCategoryId(product.getCategoryId());
        existing.setStatus(product.getStatus());
        existing.setUpdateTime(new Date());
        productMapper.updateById(existing);
        return Result.success();
    }

    @Override
    @Transactional
    public Result<?> updateCategory(Integer categoryId, Category category) {
        // 优先写 item 服务（campus_item 为前台展示源）
        try {
            Map<String, Object> response = restTemplate.exchange(
                    itemServiceUrl + "/item/internal/category/" + categoryId,
                    HttpMethod.PUT, jsonEntity(category), Map.class).getBody();
            if (isResponseOk(response)) {
                return Result.success();
            }
            log.warn("商品服务分类更新响应异常: {}", response);
            return Result.error(500, "更新分类失败：" + (response != null ? response.get("message") : "未知错误"));
        } catch (Exception e) {
            log.warn("调用商品服务分类更新失败，回退本地: {}", e.getMessage());
        }
        Category existing = categoryMapper.selectById(categoryId);
        if (existing == null) {
            return Result.error(404, "分类不存在");
        }
        existing.setCategoryName(category.getCategoryName());
        existing.setCategoryDesc(category.getCategoryDesc());
        existing.setSortOrder(category.getSortOrder());
        existing.setStatus(category.getStatus());
        existing.setUpdateTime(new Date());
        categoryMapper.updateById(existing);
        return Result.success();
    }

    @Override
    @Transactional
    public Result<?> addCategory(Category category) {
        // 优先写 item 服务
        try {
            Map<String, Object> response = restTemplate.postForObject(
                    itemServiceUrl + "/item/internal/category", jsonEntity(category), Map.class);
            if (isResponseOk(response)) {
                return Result.success();
            }
            log.warn("商品服务分类新增响应异常: {}", response);
            return Result.error(500, "新增分类失败：" + (response != null ? response.get("message") : "未知错误"));
        } catch (Exception e) {
            log.warn("调用商品服务分类新增失败，回退本地: {}", e.getMessage());
        }
        category.setStatus(1);
        if (category.getSortOrder() == null) {
            category.setSortOrder(0);
        }
        category.setCreateTime(new Date());
        category.setUpdateTime(new Date());
        category.setIsDeleted(0);
        categoryMapper.insert(category);
        return Result.success();
    }

    @Override
    @Transactional
    public Result<?> deleteCategory(Integer categoryId) {
        // 优先删 item 服务
        try {
            Map<String, Object> response = restTemplate.exchange(
                    itemServiceUrl + "/item/internal/category/" + categoryId,
                    HttpMethod.DELETE, null, Map.class).getBody();
            if (isResponseOk(response)) {
                return Result.success();
            }
            log.warn("商品服务分类删除响应异常: {}", response);
            return Result.error(500, "删除分类失败：" + (response != null ? response.get("message") : "未知错误"));
        } catch (Exception e) {
            log.warn("调用商品服务分类删除失败，回退本地: {}", e.getMessage());
        }
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            return Result.error(404, "分类不存在");
        }
        categoryMapper.deleteById(categoryId);
        return Result.success();
    }

    /** 构造 JSON 请求体（RestTemplate 发送 PUT/POST）。 */
    private HttpEntity<Category> jsonEntity(Category category) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(category, headers);
    }

    @Override
    public Result<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();

        // 预先初始化所有字段默认值，确保前端始终能拿到完整字段
        stats.put("userCount", 0);
        stats.put("todayUserCount", 0);
        stats.put("productCount", 0);
        stats.put("categoryCount", 0);
        stats.put("onSaleCount", 0);
        stats.put("orderCount", 0);
        stats.put("todayOrderCount", 0);
        stats.put("todayAmount", BigDecimal.ZERO);
        stats.put("totalAmount", BigDecimal.ZERO);

        // 从各微服务获取统计数据（带降级兜底）
        fetchAuthStats(stats);
        fetchItemStats(stats);
        fetchOrderStats(stats);

        log.info("管理员统计数据聚合完成: {}", stats);
        return Result.success(stats);
    }

    /**
     * 判断微服务响应是否成功。
     * 兼容 code 字段被 Jackson 反序列化为 Integer / Long / 其他 Number 类型的情况。
     */
    private boolean isResponseOk(Map<String, Object> response) {
        if (response == null) return false;
        Object codeObj = response.get("code");
        if (codeObj instanceof Number) {
            return ((Number) codeObj).intValue() == 200;
        }
        // 兜底：尝试字符串比较
        return codeObj != null && "200".equals(codeObj.toString());
    }

    @SuppressWarnings("unchecked")
    private void fetchAuthStats(Map<String, Object> stats) {
        String url = authServiceUrl + "/auth/internal/stats";
        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            log.info("调用认证服务统计接口: url={}, response={}", url, response);
            if (isResponseOk(response)) {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                if (data != null) {
                    stats.put("userCount", data.getOrDefault("userCount", 0));
                    stats.put("todayUserCount", data.getOrDefault("todayUserCount", 0));
                } else {
                    log.warn("认证服务统计响应 data 为空");
                }
            } else {
                log.warn("认证服务统计响应 code 非200: {}", response);
            }
        } catch (Exception e) {
            log.warn("获取认证服务统计数据失败: url={}, error={}", url, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void fetchItemStats(Map<String, Object> stats) {
        String url = itemServiceUrl + "/item/internal/stats";
        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            log.info("调用商品服务统计接口: url={}, response={}", url, response);
            if (isResponseOk(response)) {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                if (data != null) {
                    stats.put("productCount", data.getOrDefault("productCount", 0));
                    stats.put("categoryCount", data.getOrDefault("categoryCount", 0));
                    stats.put("onSaleCount", data.getOrDefault("onSaleCount", 0));
                } else {
                    log.warn("商品服务统计响应 data 为空");
                }
            } else {
                log.warn("商品服务统计响应 code 非200: {}", response);
            }
        } catch (Exception e) {
            log.warn("获取商品服务统计数据失败: url={}, error={}", url, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void fetchOrderStats(Map<String, Object> stats) {
        String url = orderServiceUrl + "/api/orders/internal/stats";
        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            log.info("调用订单服务统计接口: url={}, response={}", url, response);
            if (isResponseOk(response)) {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                if (data != null) {
                    stats.put("orderCount", data.getOrDefault("orderCount", 0));
                    stats.put("todayOrderCount", data.getOrDefault("todayOrderCount", 0));
                    stats.put("todayAmount", data.getOrDefault("todayAmount", BigDecimal.ZERO));
                    stats.put("totalAmount", data.getOrDefault("totalAmount", BigDecimal.ZERO));
                } else {
                    log.warn("订单服务统计响应 data 为空");
                }
            } else {
                log.warn("订单服务统计响应 code 非200: {}", response);
            }
        } catch (Exception e) {
            log.warn("获取订单服务统计数据失败: url={}, error={}", url, e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result<?> updateUserRole(Long userId, Integer role) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        user.setRole(role);
        user.setUpdateTime(new Date());
        userMapper.updateById(user);
        return Result.success();
    }

    @Override
    public Result<PageResult<AdminProductVO>> getProductList(String keyword, Long current, Long size, Integer status) {
        // 优先调用 item 服务获取真实数据
        try {
            StringBuilder urlBuilder = new StringBuilder(itemServiceUrl)
                    .append("/item/internal/list?current=").append(current)
                    .append("&size=").append(size);
            if (keyword != null && !keyword.isEmpty()) {
                urlBuilder.append("&keyword=").append(keyword);
            }
            if (status != null) {
                urlBuilder.append("&status=").append(status);
            }
            String url = urlBuilder.toString();
            log.info("调用商品服务内部列表接口: {}", url);

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (isResponseOk(response)) {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                if (data != null) {
                    PageResult<AdminProductVO> result = new PageResult<>();
                    result.setCurrent(((Number) data.getOrDefault("current", 1)).longValue());
                    result.setSize(((Number) data.getOrDefault("size", 10)).longValue());
                    result.setTotal(((Number) data.getOrDefault("total", 0)).longValue());
                    result.setPages(((Number) data.getOrDefault("pages", 0)).longValue());

                    List<Map<String, Object>> records = (List<Map<String, Object>>) data.get("records");
                    List<AdminProductVO> voList = new ArrayList<>();
                    if (records != null) {
                        for (Map<String, Object> record : records) {
                            AdminProductVO vo = new AdminProductVO();
                            vo.setId(toLong(record.get("id")));
                            vo.setProductId(toLong(record.get("id")));
                            vo.setProductName((String) record.getOrDefault("title", ""));
                            vo.setTitle((String) record.getOrDefault("title", ""));
                            vo.setDescription((String) record.getOrDefault("description", ""));
                            vo.setPrice(toBigDecimal(record.get("price")));
                            vo.setOriginalPrice(toBigDecimal(record.get("originalPrice")));
                            vo.setQuantity(toInteger(record.get("quantity")));
                            vo.setStatus(toInteger(record.get("status")));
                            vo.setSellerId(toLong(record.get("sellerId")));
                            vo.setSellerName((String) record.getOrDefault("sellerName", ""));
                            vo.setViewCount(toInteger(record.get("viewCount")));
                            vo.setTradeType(toInteger(record.get("tradeType")));
                            vo.setCategoryName((String) record.getOrDefault("categoryName", ""));

                            Object imagesObj = record.get("images");
                            if (imagesObj instanceof List) {
                                List<String> images = new ArrayList<>();
                                for (Object img : (List<?>) imagesObj) {
                                    if (img != null) images.add(img.toString());
                                }
                                vo.setImages(images);
                                if (!images.isEmpty()) {
                                    vo.setProductImage(images.get(0));
                                }
                            } else {
                                vo.setImages(new ArrayList<>());
                            }

                            vo.setCreateTime(parseDateTime(record.get("createTime")));

                            voList.add(vo);
                        }
                    }
                    result.setRecords(voList);
                    log.info("商品列表获取成功: total={}, records={}", result.getTotal(), voList.size());
                    return Result.success(result);
                }
            }
            log.warn("商品服务列表接口响应异常: {}", response);
        } catch (Exception e) {
            log.warn("调用商品服务列表接口失败，尝试本地查询: {}", e.getMessage());
        }

        // 降级：查询本地数据库
        return getProductListFromLocal(keyword, current, size);
    }

    private Result<PageResult<AdminProductVO>> getProductListFromLocal(String keyword, Long current, Long size) {
        Page<Product> page = new Page<>(current, size);
        IPage<Product> resultPage;
        if (keyword != null && !keyword.isEmpty()) {
            resultPage = productMapper.selectPageByKeyword(page, keyword);
        } else {
            resultPage = productMapper.selectPage(page, null);
        }

        PageResult<AdminProductVO> result = new PageResult<>();
        BeanUtils.copyProperties(resultPage, result);

        List<AdminProductVO> records = new ArrayList<>();
        for (Product product : resultPage.getRecords()) {
            AdminProductVO vo = new AdminProductVO();
            vo.setId(product.getProductId());
            vo.setProductId(product.getProductId());
            vo.setProductName(product.getTitle());
            vo.setTitle(product.getTitle());
            vo.setDescription(product.getDescription());
            vo.setPrice(product.getPrice());
            vo.setOriginalPrice(product.getOriginalPrice());
            vo.setQuantity(product.getQuantity());
            vo.setStatus(product.getStatus());
            vo.setCreateTime(product.getCreateTime());
            vo.setSellerId(product.getPublishUserId());
            vo.setViewCount(product.getViewCount());
            vo.setTradeType(product.getTradeType());

            Category category = categoryMapper.selectById(product.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getCategoryName());
            }

            if (product.getPublishUserId() != null) {
                User seller = userMapper.selectById(product.getPublishUserId());
                if (seller != null) {
                    vo.setSellerName(seller.getNickname() != null ? seller.getNickname() : seller.getPhone());
                }
            }

            Image image = imageMapper.findFirstByTypeAndRelationId(Image.TYPE_PRODUCT, product.getProductId());
            if (image != null) {
                vo.setProductImage(image.getImageUrl());
                vo.setImages(java.util.List.of(image.getImageUrl()));
            } else {
                vo.setImages(new ArrayList<>());
            }

            records.add(vo);
        }
        result.setRecords(records);

        return Result.success(result);
    }

    private Long toLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number) return ((Number) obj).longValue();
        return Long.parseLong(obj.toString());
    }

    private Integer toInteger(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number) return ((Number) obj).intValue();
        return Integer.parseInt(obj.toString());
    }

    private BigDecimal toBigDecimal(Object obj) {
        if (obj == null) return null;
        if (obj instanceof BigDecimal) return (BigDecimal) obj;
        if (obj instanceof Number) return BigDecimal.valueOf(((Number) obj).doubleValue());
        return new BigDecimal(obj.toString());
    }

    /**
     * 安全解析内部接口返回的时间字段。
     * 下游服务把 java.util.Date 序列化为 epoch 毫秒数字，也可能是带 T/Z 的字符串；
     * 解析失败返回 null，避免单个字段异常拖垮整页数据。
     */
    private Date parseDateTime(Object value) {
        if (value == null) return null;
        try {
            if (value instanceof Number) {
                return new Date(((Number) value).longValue());
            }
            if (value instanceof Date) {
                return (Date) value;
            }
            String s = value.toString().trim();
            if (s.isEmpty()) {
                return null;
            }
            return new Date(s.replace("T", " ").replace("Z", ""));
        } catch (Exception e) {
            log.warn("解析时间失败: {}", value);
            return null;
        }
    }

    @Override
    @Transactional
    public Result<?> deleteProduct(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            return Result.error(404, "商品不存在");
        }
        imageMapper.deleteByTypeAndRelationId(Image.TYPE_PRODUCT, productId);
        productMapper.deleteById(productId);
        return Result.success();
    }
}