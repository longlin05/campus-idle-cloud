package org.lin.campusadmin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.lin.common.context.OrderListItemVO;
import org.lin.common.context.PageResult;
import org.lin.common.jwt.JwtAuth;
import org.lin.common.result.Result;
import org.lin.common.entity.OrderInfo;
import org.lin.common.entity.Product;
import org.lin.common.entity.User;
import org.lin.common.enums.NotificationType;
import org.lin.common.enums.OrderStatus;
import org.lin.common.enums.ProductStatus;
import org.lin.common.enums.UserRole;
import org.lin.common.kafka.KafkaProducerService;
import org.lin.common.kafka.KafkaTopicConstants;
import org.lin.common.kafka.dto.NotificationSendPayload;
import org.lin.campusadmin.mapper.OrderMapper;
import org.lin.campusadmin.mapper.ProductMapper;
import org.lin.campusadmin.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/admin")
public class AdminExtraController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired(required = false)
    private KafkaProducerService kafkaProducerService;

    @Value("${campus.order.service-url:http://localhost:8084}")
    private String orderServiceUrl;

    @JwtAuth(admin = true)
    @DeleteMapping("/users/{userId}")
    public Result<?> deleteUser(@PathVariable Long userId) {
        User u = userMapper.selectById(userId);
        if (u == null) return Result.error(404, "用户不存在");
        userMapper.deleteById(userId);
        return Result.success("删除成功");
    }

    @JwtAuth(admin = true)
    @GetMapping("/orders")
    public Result<?> getOrderList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam Long current,
            @RequestParam Long size) {
        // 优先调用 order 服务获取真实全量数据（campus_order 库为准）
        try {
            StringBuilder urlBuilder = new StringBuilder(orderServiceUrl)
                    .append("/api/orders/internal/list?current=").append(current)
                    .append("&size=").append(size);
            if (keyword != null && !keyword.isEmpty()) {
                urlBuilder.append("&keyword=").append(keyword);
            }
            if (status != null) {
                urlBuilder.append("&status=").append(status);
            }
            String url = urlBuilder.toString();
            log.info("调用订单服务内部列表接口: {}", url);

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (isResponseOk(response)) {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                if (data != null) {
                    PageResult<OrderListItemVO> result = new PageResult<>();
                    result.setCurrent(((Number) data.getOrDefault("current", 1)).longValue());
                    result.setSize(((Number) data.getOrDefault("size", 10)).longValue());
                    result.setTotal(((Number) data.getOrDefault("total", 0)).longValue());
                    result.setPages(((Number) data.getOrDefault("pages", 0)).longValue());

                    List<Map<String, Object>> records = (List<Map<String, Object>>) data.get("records");
                    List<OrderListItemVO> voList = new ArrayList<>();
                    if (records != null) {
                        for (Map<String, Object> record : records) {
                            OrderListItemVO vo = new OrderListItemVO();
                            vo.setOrderId(toLong(record.get("orderId")));
                            vo.setOrderNo((String) record.getOrDefault("orderNo", ""));
                            vo.setOrderAmount(toBigDecimal(record.get("orderAmount")));
                            vo.setStatus(toInteger(record.get("status")));
                            vo.setStatusName((String) record.getOrDefault("statusName", ""));
                            vo.setProductId(toLong(record.get("productId")));
                            vo.setProductName((String) record.getOrDefault("productName", ""));
                            vo.setProductImage((String) record.getOrDefault("productImage", ""));
                            vo.setQuantity(toInteger(record.get("quantity")));
                            vo.setCreateTime(parseDateTime(record.get("createTime")));
                            vo.setPayTime(parseDateTime(record.get("payTime")));
                            vo.setShipTime(parseDateTime(record.get("shipTime")));
                            vo.setBuyerId(toLong(record.get("buyerId")));
                            vo.setSellerId(toLong(record.get("sellerId")));
                            voList.add(vo);
                        }
                    }
                    result.setRecords(voList);
                    log.info("订单列表获取成功: total={}, records={}", result.getTotal(), voList.size());
                    return Result.success(result);
                }
            }
            log.warn("订单服务列表接口响应异常: {}", response);
        } catch (Exception e) {
            log.warn("调用订单服务列表接口失败，尝试本地查询: {}", e.getMessage());
        }

        // 降级：查询本地 campus_admin 副本
        LambdaQueryWrapper<OrderInfo> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            w.and(ww -> ww.like(OrderInfo::getOrderNo, keyword).or().like(OrderInfo::getProductName, keyword));
        }
        if (status != null) w.eq(OrderInfo::getStatus, status);
        w.orderByDesc(OrderInfo::getCreateTime);
        Page<OrderInfo> page = orderMapper.selectPage(new Page<>(current, size), w);
        PageResult<OrderInfo> pr = new PageResult<>();
        pr.setCurrent(page.getCurrent());
        pr.setSize(page.getSize());
        pr.setTotal(page.getTotal());
        pr.setPages(page.getPages());
        pr.setRecords(page.getRecords());
        return Result.success(pr);
    }

    /** 判断微服务响应是否成功，兼容 code 为 Number / String。 */
    private boolean isResponseOk(Map<String, Object> response) {
        if (response == null) return false;
        Object codeObj = response.get("code");
        if (codeObj instanceof Number) {
            return ((Number) codeObj).intValue() == 200;
        }
        return codeObj != null && "200".equals(codeObj.toString());
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

    /** 兼容 epoch 毫秒数字 / 带 T、Z 的字符串两种时间序列化格式，解析失败返回 null。 */
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
            if (s.isEmpty()) return null;
            return new Date(s.replace("T", " ").replace("Z", ""));
        } catch (Exception e) {
            log.warn("解析时间失败: {}", value);
            return null;
        }
    }

    @JwtAuth(admin = true)
    @PutMapping("/products/{productId}/online")
    public Result<?> productOnline(@PathVariable Long productId) {
        Product p = productMapper.selectById(productId);
        if (p == null) return Result.error(404, "商品不存在");
        p.setStatus(ProductStatus.ON_SALE.getCode());
        p.setUpdateTime(new Date());
        productMapper.updateById(p);
        return Result.success("已上架");
    }

    @JwtAuth(admin = true)
    @PutMapping("/products/{productId}/offline")
    public Result<?> productOffline(@PathVariable Long productId) {
        Product p = productMapper.selectById(productId);
        if (p == null) return Result.error(404, "商品不存在");
        p.setStatus(ProductStatus.ADMIN_OFF_SHELF.getCode());
        p.setUpdateTime(new Date());
        productMapper.updateById(p);
        return Result.success("已下架");
    }

    @JwtAuth(admin = true)
    @PostMapping("/message/send")
    public Result<?> sendMessage(@RequestBody Map<String, Object> body) {
        String title = (String) body.getOrDefault("title", "系统通知");
        String content = (String) body.get("content");
        if (content == null || content.trim().isEmpty()) return Result.businessError("消息内容不能为空");

        if (kafkaProducerService == null) {
            return Result.businessError("系统消息功能未启用（服务端 Kafka 未开启，lin.kafka.enabled=false）");
        }

        // targetType: 0=全体用户 1=指定用户；消息类型统一为系统通知，保证用户端消息中心可见
        int targetType = body.get("targetType") instanceof Number
                ? ((Number) body.get("targetType")).intValue() : 0;
        Integer type = NotificationType.SYSTEM.getCode();
        Date now = new Date();
        // 本次发送的批次号：同一批所有接收行共享，管理端列表据此去重显示一条
        String batchNo = java.util.UUID.randomUUID().toString().replace("-", "");
        List<Long> receiverIds = new ArrayList<>();

        if (targetType == 1) {
            Object userIdObj = body.get("userId");
            if (userIdObj == null) return Result.businessError("请指定用户ID");
            Long uid = userIdObj instanceof Number
                    ? ((Number) userIdObj).longValue()
                    : Long.parseLong(userIdObj.toString());
            receiverIds.add(uid);
        } else {
            // 全体用户：从本地 sys_user 副本取全部正常用户
            LambdaQueryWrapper<User> w = new LambdaQueryWrapper<>();
            w.select(User::getUserId).eq(User::getStatus, 1);
            for (User u : userMapper.selectList(w)) {
                if (u.getUserId() != null) receiverIds.add(u.getUserId());
            }
        }

        if (receiverIds.isEmpty()) {
            return Result.businessError("暂无接收用户");
        }

        for (Long receiverId : receiverIds) {
            NotificationSendPayload payload = NotificationSendPayload.builder()
                    .receiverId(receiverId)
                    .senderId(0L)
                    .batchNo(batchNo)
                    .title(title)
                    .content(content)
                    .type(type)
                    .createTime(now)
                    .build();
            kafkaProducerService.sendAsync(KafkaTopicConstants.NOTIFICATION_SEND, "admin-send", payload);
        }
        log.info("[系统消息] 投递 {} 条 targetType={} batchNo={} title={}", receiverIds.size(), targetType, batchNo, title);
        return Result.success("发送成功，共投递 " + receiverIds.size() + " 条");
    }

    @JwtAuth(admin = true)
    @DeleteMapping("/message/{id}")
    public Result<?> deleteMessage(@PathVariable Long id) {
        return Result.success("删除成功（通知在 campus-user 服务，此处占位）");
    }

    @JwtAuth(admin = true)
    @GetMapping("/system/admins")
    public Result<PageResult<Object>> getAdminList(
            @RequestParam(required = false) String keyword,
            @RequestParam Long current,
            @RequestParam Long size) {
        LambdaQueryWrapper<User> w = new LambdaQueryWrapper<>();
        w.eq(User::getRole, UserRole.ADMIN.getCode());
        if (keyword != null && !keyword.trim().isEmpty()) {
            w.and(ww -> ww.like(User::getNickname, keyword).or().like(User::getPhone, keyword));
        }
        w.orderByDesc(User::getCreateTime);
        Page<User> page = userMapper.selectPage(new Page<>(current, size), w);
        PageResult<Object> pr = new PageResult<>();
        pr.setCurrent(page.getCurrent());
        pr.setSize(page.getSize());
        pr.setTotal(page.getTotal());
        pr.setPages(page.getPages());
        pr.setRecords(new ArrayList<>(page.getRecords()));
        return Result.success(pr);
    }

    @JwtAuth(admin = true)
    @PostMapping("/system/admin")
    public Result<?> createAdmin(@RequestBody Map<String, Object> body) {
        return Result.businessError("请通过更新用户角色接口将普通用户设为管理员（PUT /admin/users/{userId}/role?role=0）");
    }

    @JwtAuth(admin = true)
    @PutMapping("/system/admin/{id}")
    public Result<?> updateAdmin(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return Result.success("更新成功（占位）");
    }

    @JwtAuth(admin = true)
    @PutMapping("/system/admin/{id}/enable")
    public Result<?> enableAdmin(@PathVariable Long id) {
        User u = userMapper.selectById(id);
        if (u == null) return Result.error(404, "用户不存在");
        u.setStatus(1);
        u.setUpdateTime(new Date());
        userMapper.updateById(u);
        return Result.success("已启用");
    }

    @JwtAuth(admin = true)
    @PutMapping("/system/admin/{id}/disable")
    public Result<?> disableAdmin(@PathVariable Long id) {
        User u = userMapper.selectById(id);
        if (u == null) return Result.error(404, "用户不存在");
        u.setStatus(0);
        u.setUpdateTime(new Date());
        userMapper.updateById(u);
        return Result.success("已禁用");
    }

    @JwtAuth(admin = true)
    @DeleteMapping("/system/admin/{id}")
    public Result<?> removeAdmin(@PathVariable Long id) {
        User u = userMapper.selectById(id);
        if (u == null) return Result.error(404, "用户不存在");
        u.setRole(UserRole.USER.getCode());
        u.setUpdateTime(new Date());
        userMapper.updateById(u);
        return Result.success("已将该用户降级为普通用户");
    }
}
