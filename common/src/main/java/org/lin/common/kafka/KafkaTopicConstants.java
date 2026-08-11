package org.lin.common.kafka;

/**
 * Kafka Topic 常量定义。
 * <p>
 * 统一管理所有业务 Topic 名称，避免字符串散落在各服务代码中造成拼写不一致。
 * 命名规范：业务域.动作，使用小写英文。
 */
public final class KafkaTopicConstants {

    private KafkaTopicConstants() {}

    // ============ 商品域 ============
    /** 商品发布事件（主信息已落库，异步处理图片/通知等副作用） */
    public static final String PRODUCT_PUBLISHED = "product.published";
    /** 商品浏览量统计（Redis 计数后批量落库） */
    public static final String PRODUCT_VIEW_COUNT = "product.view.count";

    // ============ 订单域 ============
    /** 订单创建事件（Redis 预扣库存后异步创建订单） */
    public static final String ORDER_CREATED = "order.created";
    /** 订单状态变更事件（支付/发货/确认/取消，用于推送通知） */
    public static final String ORDER_STATUS_CHANGED = "order.status.changed";

    // ============ 聊天/通知域 ============
    /** 聊天消息事件（Redis 实时缓存后异步落库） */
    public static final String CHAT_MESSAGE = "chat.message";
    /** 系统通知事件（异步写库 + 推送） */
    public static final String NOTIFICATION_SEND = "notification.send";

    // ============ 死信队列 ============
    /** 死信 Topic 后缀，消费失败重试耗尽后投递，格式如 product.published.dlq */
    public static final String DLQ_SUFFIX = ".dlq";
}
