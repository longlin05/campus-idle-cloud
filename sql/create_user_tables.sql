-- campus_user 数据库补建表（Cart / CartItem / Notification）
-- 字段名 create_time / update_time 与 campus-user entity 的 createTime / updateTime 对应

CREATE TABLE IF NOT EXISTS `idle_cart` (
  `cart_id` bigint NOT NULL AUTO_INCREMENT COMMENT '购物车ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`cart_id`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='购物车表';

CREATE TABLE IF NOT EXISTS `idle_cart_item` (
  `item_id` bigint NOT NULL AUTO_INCREMENT COMMENT '购物车项ID',
  `cart_id` bigint NOT NULL COMMENT '购物车ID',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `quantity` int DEFAULT 1 COMMENT '商品数量',
  `selected` tinyint DEFAULT 1 COMMENT '是否选中：0-未选中 1-选中',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`item_id`),
  UNIQUE KEY `uk_cart_product` (`cart_id`,`product_id`),
  KEY `idx_cart_id` (`cart_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='购物车商品项表';

CREATE TABLE IF NOT EXISTS `user_notification` (
  `notification_id` bigint NOT NULL AUTO_INCREMENT COMMENT '通知主键ID',
  `receiver_id` bigint NOT NULL COMMENT '接收人用户ID',
  `sender_id` bigint DEFAULT 0 COMMENT '发送人用户ID，0为系统发送',
  `batch_no` varchar(64) DEFAULT NULL COMMENT '发送批次号（同一批发送共享，用于管理端列表去重）',
  `title` varchar(200) DEFAULT NULL COMMENT '通知标题',
  `content` text NOT NULL COMMENT '通知内容/消息正文',
  `type` tinyint NOT NULL DEFAULT 1 COMMENT '通知类型：0-系统 1-订单 2-互动 3-私信',
  `product_id` bigint DEFAULT NULL COMMENT '关联商品ID（可选）',
  `is_read` tinyint NOT NULL DEFAULT 0 COMMENT '是否已读：0-未读 1-已读',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`notification_id`),
  KEY `idx_receiver_is_read` (`receiver_id`,`is_read`),
  KEY `idx_receiver_create_time` (`receiver_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户站内信表';
