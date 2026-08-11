-- =====================================================
-- 校园闲置物品交易平台 - 微服务数据库初始化脚本
-- 每个微服务独立数据库
-- =====================================================

-- =====================================================
-- 1. campus_auth 数据库（认证服务）
-- =====================================================
CREATE DATABASE IF NOT EXISTS campus_auth DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE campus_auth;

DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户主键ID',
    `password` varchar(128) NOT NULL COMMENT '加密密码',
    `nickname` varchar(32) NOT NULL COMMENT '用户昵称',
    `avatar` varchar(512) DEFAULT '' COMMENT '头像地址',
    `phone` varchar(11) DEFAULT '' COMMENT '手机号',
    `email` varchar(64) DEFAULT '' COMMENT '邮箱',
    `role` tinyint NOT NULL DEFAULT '1' COMMENT '用户角色：0-管理员 1-普通用户',
    `status` tinyint NOT NULL DEFAULT '1' COMMENT '账号状态：0-禁用 1-正常',
    `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip` varchar(32) DEFAULT '' COMMENT '最后登录IP',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `uk_phone` (`phone`),
    KEY `idx_role_status` (`role`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- =====================================================
-- 2. campus_user 数据库（用户服务）
-- =====================================================
CREATE DATABASE IF NOT EXISTS campus_user DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE campus_user;

DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户主键ID',
    `password` varchar(128) NOT NULL COMMENT '加密密码',
    `nickname` varchar(32) NOT NULL COMMENT '用户昵称',
    `avatar` varchar(512) DEFAULT '' COMMENT '头像地址',
    `phone` varchar(11) DEFAULT '' COMMENT '手机号',
    `email` varchar(64) DEFAULT '' COMMENT '邮箱',
    `role` tinyint NOT NULL DEFAULT '1' COMMENT '用户角色：0-管理员 1-普通用户',
    `status` tinyint NOT NULL DEFAULT '1' COMMENT '账号状态：0-禁用 1-正常',
    `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip` varchar(32) DEFAULT '' COMMENT '最后登录IP',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `uk_phone` (`phone`),
    KEY `idx_role_status` (`role`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

DROP TABLE IF EXISTS `sys_address`;
CREATE TABLE `sys_address` (
    `address_id` bigint NOT NULL AUTO_INCREMENT COMMENT '地址ID',
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `receiver_name` varchar(50) NOT NULL COMMENT '收货人姓名',
    `receiver_phone` varchar(20) NOT NULL COMMENT '收货人手机号',
    `province` varchar(50) DEFAULT NULL COMMENT '省份',
    `city` varchar(50) DEFAULT NULL COMMENT '城市',
    `district` varchar(50) DEFAULT NULL COMMENT '区/县',
    `detail_address` varchar(200) NOT NULL COMMENT '详细地址',
    `is_default` tinyint NOT NULL DEFAULT '0' COMMENT '是否默认地址',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
    PRIMARY KEY (`address_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户收货地址表';

DROP TABLE IF EXISTS `sys_follow`;
CREATE TABLE `sys_follow` (
    `follow_id` bigint NOT NULL AUTO_INCREMENT COMMENT '关注ID',
    `user_id` bigint NOT NULL COMMENT '用户ID（粉丝）',
    `follow_user_id` bigint NOT NULL COMMENT '被关注用户ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
    PRIMARY KEY (`follow_id`),
    UNIQUE KEY `uk_user_followed` (`user_id`,`follow_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='关注表';

DROP TABLE IF EXISTS `sys_favorite`;
CREATE TABLE `sys_favorite` (
    `favorite_id` bigint NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `product_id` bigint NOT NULL COMMENT '商品ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
    PRIMARY KEY (`favorite_id`),
    UNIQUE KEY `uk_user_product` (`user_id`,`product_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏表';

-- =====================================================
-- 3. campus_item 数据库（商品服务）
-- =====================================================
CREATE DATABASE IF NOT EXISTS campus_item DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE campus_item;

DROP TABLE IF EXISTS `idle_category`;
CREATE TABLE `idle_category` (
    `category_id` tinyint NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `category_name` varchar(32) NOT NULL COMMENT '分类名称',
    `sort_order` tinyint NOT NULL DEFAULT '0' COMMENT '排序顺序',
    `category_desc` varchar(500) DEFAULT NULL COMMENT '分类描述',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`category_id`),
    UNIQUE KEY `uk_category_name` (`category_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品分类表';

DROP TABLE IF EXISTS `idle_product`;
CREATE TABLE `idle_product` (
    `product_id` bigint NOT NULL AUTO_INCREMENT COMMENT '商品主键ID',
    `publish_user_id` bigint NOT NULL COMMENT '发布者用户ID',
    `title` varchar(64) NOT NULL COMMENT '商品标题',
    `description` text NOT NULL COMMENT '商品详情描述',
    `price` decimal(10,2) NOT NULL COMMENT '出售价格',
    `original_price` decimal(10,2) DEFAULT NULL COMMENT '原价',
    `quantity` int NOT NULL DEFAULT '1' COMMENT '商品数量',
    `trade_type` tinyint NOT NULL DEFAULT '0' COMMENT '交易方式：0-线上 1-线下自提',
    `category_id` tinyint NOT NULL COMMENT '商品分类ID',
    `status` tinyint NOT NULL DEFAULT '1' COMMENT '商品状态：0-下架 1-上架 2-已售出 3-违规',
    `view_count` int NOT NULL DEFAULT '0' COMMENT '浏览量',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
    PRIMARY KEY (`product_id`),
    KEY `idx_publish_user` (`publish_user_id`),
    KEY `idx_category_status` (`category_id`,`status`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='闲置商品表';

DROP TABLE IF EXISTS `idle_image`;
CREATE TABLE `idle_image` (
    `image_id` bigint NOT NULL AUTO_INCREMENT COMMENT '图片ID',
    `type` tinyint NOT NULL COMMENT '图片类型：1-用户头像 2-商品图片 3-系统图片',
    `relation_id` bigint DEFAULT NULL COMMENT '关联ID',
    `image_url` varchar(512) NOT NULL COMMENT '图片路径',
    `sort_order` tinyint NOT NULL DEFAULT '0' COMMENT '排序顺序',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`image_id`),
    KEY `idx_type` (`type`),
    KEY `idx_relation_id` (`relation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='统一图片表';

-- sys_user 副本（供商品服务查询发布者信息）
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户主键ID',
    `password` varchar(128) NOT NULL COMMENT '加密密码',
    `nickname` varchar(32) NOT NULL COMMENT '用户昵称',
    `avatar` varchar(512) DEFAULT '' COMMENT '头像地址',
    `phone` varchar(11) DEFAULT '' COMMENT '手机号',
    `email` varchar(64) DEFAULT '' COMMENT '邮箱',
    `role` tinyint NOT NULL DEFAULT '1' COMMENT '用户角色：0-管理员 1-普通用户',
    `status` tinyint NOT NULL DEFAULT '1' COMMENT '账号状态：0-禁用 1-正常',
    `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip` varchar(32) DEFAULT '' COMMENT '最后登录IP',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表（副本）';

-- =====================================================
-- 4. campus_order 数据库（订单服务）
-- =====================================================
CREATE DATABASE IF NOT EXISTS campus_order DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE campus_order;

DROP TABLE IF EXISTS `order_info`;
CREATE TABLE `order_info` (
    `order_id` bigint NOT NULL AUTO_INCREMENT COMMENT '订单主键ID',
    `order_no` varchar(32) NOT NULL COMMENT '订单编号',
    `product_id` bigint NOT NULL COMMENT '商品ID',
    `product_name` varchar(200) DEFAULT NULL COMMENT '商品名称快照',
    `product_image` varchar(512) DEFAULT NULL COMMENT '商品主图快照',
    `product_price` decimal(10,2) DEFAULT NULL COMMENT '商品单价快照',
    `buyer_id` bigint NOT NULL COMMENT '买家用户ID',
    `seller_id` bigint NOT NULL COMMENT '卖家用户ID',
    `order_amount` decimal(10,2) NOT NULL COMMENT '订单金额',
    `status` tinyint NOT NULL DEFAULT '0' COMMENT '订单状态：0-待支付 1-已支付 2-已发货 3-已完成 4-已取消',
    `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
    `cancel_time` datetime DEFAULT NULL COMMENT '取消时间',
    `remark` varchar(255) DEFAULT '' COMMENT '订单备注',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
    `ship_time` datetime DEFAULT NULL COMMENT '发货时间',
    `confirm_time` datetime DEFAULT NULL COMMENT '确认收货时间',
    `receiver_name` varchar(50) DEFAULT NULL COMMENT '收货人姓名',
    `receiver_phone` varchar(20) DEFAULT NULL COMMENT '收货人手机号',
    `receiver_address` varchar(500) DEFAULT NULL COMMENT '收货地址',
    `quantity` int NOT NULL DEFAULT '1' COMMENT '购买数量',
    PRIMARY KEY (`order_id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_buyer_id` (`buyer_id`),
    KEY `idx_seller_id` (`seller_id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_status_create_time` (`status`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易订单表';

-- idle_product 副本（供订单服务查询商品信息）
DROP TABLE IF EXISTS `idle_product`;
CREATE TABLE `idle_product` (
    `product_id` bigint NOT NULL AUTO_INCREMENT COMMENT '商品主键ID',
    `publish_user_id` bigint NOT NULL COMMENT '发布者用户ID',
    `title` varchar(64) NOT NULL COMMENT '商品标题',
    `description` text NOT NULL COMMENT '商品详情描述',
    `price` decimal(10,2) NOT NULL COMMENT '出售价格',
    `original_price` decimal(10,2) DEFAULT NULL COMMENT '原价',
    `quantity` int NOT NULL DEFAULT '1' COMMENT '商品数量',
    `trade_type` tinyint NOT NULL DEFAULT '0' COMMENT '交易方式',
    `category_id` tinyint NOT NULL COMMENT '商品分类ID',
    `status` tinyint NOT NULL DEFAULT '1' COMMENT '商品状态',
    `view_count` int NOT NULL DEFAULT '0' COMMENT '浏览量',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
    PRIMARY KEY (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='闲置商品表（副本）';

-- sys_user 副本（供订单服务查询买卖家信息）
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户主键ID',
    `password` varchar(128) NOT NULL COMMENT '加密密码',
    `nickname` varchar(32) NOT NULL COMMENT '用户昵称',
    `avatar` varchar(512) DEFAULT '' COMMENT '头像地址',
    `phone` varchar(11) DEFAULT '' COMMENT '手机号',
    `email` varchar(64) DEFAULT '' COMMENT '邮箱',
    `role` tinyint NOT NULL DEFAULT '1' COMMENT '用户角色',
    `status` tinyint NOT NULL DEFAULT '1' COMMENT '账号状态',
    `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip` varchar(32) DEFAULT '' COMMENT '最后登录IP',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表（副本）';

-- =====================================================
-- 5. campus_admin 数据库（管理服务）
-- =====================================================
CREATE DATABASE IF NOT EXISTS campus_admin DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE campus_admin;

DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户主键ID',
    `password` varchar(128) NOT NULL COMMENT '加密密码',
    `nickname` varchar(32) NOT NULL COMMENT '用户昵称',
    `avatar` varchar(512) DEFAULT '' COMMENT '头像地址',
    `phone` varchar(11) DEFAULT '' COMMENT '手机号',
    `email` varchar(64) DEFAULT '' COMMENT '邮箱',
    `role` tinyint NOT NULL DEFAULT '1' COMMENT '用户角色：0-管理员 1-普通用户',
    `status` tinyint NOT NULL DEFAULT '1' COMMENT '账号状态：0-禁用 1-正常',
    `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip` varchar(32) DEFAULT '' COMMENT '最后登录IP',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表（副本）';

DROP TABLE IF EXISTS `idle_product`;
CREATE TABLE `idle_product` (
    `product_id` bigint NOT NULL AUTO_INCREMENT COMMENT '商品主键ID',
    `publish_user_id` bigint NOT NULL COMMENT '发布者用户ID',
    `title` varchar(64) NOT NULL COMMENT '商品标题',
    `description` text NOT NULL COMMENT '商品详情描述',
    `price` decimal(10,2) NOT NULL COMMENT '出售价格',
    `original_price` decimal(10,2) DEFAULT NULL COMMENT '原价',
    `quantity` int NOT NULL DEFAULT '1' COMMENT '商品数量',
    `trade_type` tinyint NOT NULL DEFAULT '0' COMMENT '交易方式',
    `category_id` tinyint NOT NULL COMMENT '商品分类ID',
    `status` tinyint NOT NULL DEFAULT '1' COMMENT '商品状态',
    `view_count` int NOT NULL DEFAULT '0' COMMENT '浏览量',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
    PRIMARY KEY (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='闲置商品表（副本）';

DROP TABLE IF EXISTS `order_info`;
CREATE TABLE `order_info` (
    `order_id` bigint NOT NULL AUTO_INCREMENT COMMENT '订单主键ID',
    `order_no` varchar(32) NOT NULL COMMENT '订单编号',
    `product_id` bigint NOT NULL COMMENT '商品ID',
    `product_name` varchar(200) DEFAULT NULL COMMENT '商品名称快照',
    `product_image` varchar(512) DEFAULT NULL COMMENT '商品主图快照',
    `product_price` decimal(10,2) DEFAULT NULL COMMENT '商品单价快照',
    `buyer_id` bigint NOT NULL COMMENT '买家用户ID',
    `seller_id` bigint NOT NULL COMMENT '卖家用户ID',
    `order_amount` decimal(10,2) NOT NULL COMMENT '订单金额',
    `status` tinyint NOT NULL DEFAULT '0' COMMENT '订单状态',
    `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
    `cancel_time` datetime DEFAULT NULL COMMENT '取消时间',
    `remark` varchar(255) DEFAULT '' COMMENT '订单备注',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
    `ship_time` datetime DEFAULT NULL COMMENT '发货时间',
    `confirm_time` datetime DEFAULT NULL COMMENT '确认收货时间',
    `receiver_name` varchar(50) DEFAULT NULL COMMENT '收货人姓名',
    `receiver_phone` varchar(20) DEFAULT NULL COMMENT '收货人手机号',
    `receiver_address` varchar(500) DEFAULT NULL COMMENT '收货地址',
    `quantity` int NOT NULL DEFAULT '1' COMMENT '购买数量',
    PRIMARY KEY (`order_id`),
    UNIQUE KEY `uk_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易订单表（副本）';

DROP TABLE IF EXISTS `idle_image`;
CREATE TABLE `idle_image` (
    `image_id` bigint NOT NULL AUTO_INCREMENT COMMENT '图片ID',
    `type` tinyint NOT NULL COMMENT '图片类型',
    `relation_id` bigint DEFAULT NULL COMMENT '关联ID',
    `image_url` varchar(512) NOT NULL COMMENT '图片路径',
    `sort_order` tinyint NOT NULL DEFAULT '0' COMMENT '排序顺序',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`image_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图片表（副本）';

DROP TABLE IF EXISTS `idle_category`;
CREATE TABLE `idle_category` (
    `category_id` tinyint NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `category_name` varchar(32) NOT NULL COMMENT '分类名称',
    `sort_order` tinyint NOT NULL DEFAULT '0' COMMENT '排序顺序',
    `category_desc` varchar(500) DEFAULT NULL COMMENT '分类描述',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`category_id`),
    UNIQUE KEY `uk_category_name` (`category_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品分类表（副本）';

-- =====================================================
-- 初始化数据（插入到 campus_item 的分类表）
-- =====================================================
USE campus_item;
INSERT INTO `idle_category` (`category_id`, `category_name`, `category_desc`, `sort_order`) VALUES
(1, '数码电子', '手机、电脑、相机等电子设备', 1),
(2, '书籍教材', '各类教材、课外书籍、小说等', 2),
(3, '生活用品', '寝室用品、日常用品等', 3),
(4, '服装鞋帽', '衣物、鞋子、帽子等', 4),
(5, '运动户外', '运动器材、户外用品等', 5),
(6, '其他', '其他类型的闲置物品', 6);

-- 同步分类数据到 campus_admin
USE campus_admin;
INSERT INTO `idle_category` (`category_id`, `category_name`, `category_desc`, `sort_order`) VALUES
(1, '数码电子', '手机、电脑、相机等电子设备', 1),
(2, '书籍教材', '各类教材、课外书籍、小说等', 2),
(3, '生活用品', '寝室用品、日常用品等', 3),
(4, '服装鞋帽', '衣物、鞋子、帽子等', 4),
(5, '运动户外', '运动器材、户外用品等', 5),
(6, '其他', '其他类型的闲置物品', 6);

-- =====================================================
-- 创建测试用户（所有数据库同步）
-- 密码: 123456 (MD5加密)
-- =====================================================

-- campus_auth
USE campus_auth;
INSERT INTO `sys_user` (`user_id`, `password`, `nickname`, `phone`, `role`, `status`) VALUES
(1, 'e10adc3949ba59abbe56e057f20f883e', 'admin', '13800000000', 0, 1),
(2, 'e10adc3949ba59abbe56e057f20f883e', 'testuser', '13800000001', 1, 1);

-- campus_user
USE campus_user;
INSERT INTO `sys_user` (`user_id`, `password`, `nickname`, `phone`, `role`, `status`) VALUES
(1, 'e10adc3949ba59abbe56e057f20f883e', 'admin', '13800000000', 0, 1),
(2, 'e10adc3949ba59abbe56e057f20f883e', 'testuser', '13800000001', 1, 1);

-- campus_item
USE campus_item;
INSERT INTO `sys_user` (`user_id`, `password`, `nickname`, `phone`, `role`, `status`) VALUES
(1, 'e10adc3949ba59abbe56e057f20f883e', 'admin', '13800000000', 0, 1),
(2, 'e10adc3949ba59abbe56e057f20f883e', 'testuser', '13800000001', 1, 1);

-- campus_order
USE campus_order;
INSERT INTO `sys_user` (`user_id`, `password`, `nickname`, `phone`, `role`, `status`) VALUES
(1, 'e10adc3949ba59abbe56e057f20f883e', 'admin', '13800000000', 0, 1),
(2, 'e10adc3949ba59abbe56e057f20f883e', 'testuser', '13800000001', 1, 1);

-- campus_admin
USE campus_admin;
INSERT INTO `sys_user` (`user_id`, `password`, `nickname`, `phone`, `role`, `status`) VALUES
(1, 'e10adc3949ba59abbe56e057f20f883e', 'admin', '13800000000', 0, 1),
(2, 'e10adc3949ba59abbe56e057f20f883e', 'testuser', '13800000001', 1, 1);
