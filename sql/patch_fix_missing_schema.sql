-- =============================================
-- 校园闲置交易平台 - 数据库结构完整补丁
-- 将数据库中缺失的字段、缺失的表一次性补齐
-- =============================================

-- =============================================
-- 1. 已有表缺失字段（先改字段避免后续依赖失败）
-- =============================================

-- idle_product: 缺少 quantity, trade_type
ALTER TABLE idle_product
  ADD COLUMN quantity INT NOT NULL DEFAULT 1 COMMENT '商品数量/库存' AFTER original_price,
  ADD COLUMN trade_type TINYINT NOT NULL DEFAULT 0 COMMENT '交易类型 0全新 1二手' AFTER quantity;

-- idle_category: 缺少 category_desc, create_time
ALTER TABLE idle_category
  ADD COLUMN category_desc VARCHAR(255) DEFAULT NULL COMMENT '分类描述' AFTER category_name,
  ADD COLUMN create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间' AFTER sort_order;

-- user_notification: 缺少 product_id
ALTER TABLE user_notification
  ADD COLUMN product_id BIGINT DEFAULT NULL COMMENT '关联商品ID' AFTER type;

-- =============================================
-- 2. idle_product_image 表迁移为 idle_image（支持多种图片类型）
-- =============================================
-- 创建新表 idle_image（通用图片表：type=1头像 / 2商品 / 3系统）
CREATE TABLE IF NOT EXISTS idle_image (
  image_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  type TINYINT NOT NULL COMMENT '1头像 2商品图片 3系统图片',
  relation_id BIGINT NOT NULL COMMENT '关联ID（user_id/product_id/system_id）',
  image_url VARCHAR(512) NOT NULL COMMENT '图片URL',
  sort_order TINYINT NOT NULL DEFAULT 0 COMMENT '排序',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_type_relation (type, relation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图片通用表';

-- 迁移旧数据（product_id 作为 relation_id，type=2商品图片）
INSERT IGNORE INTO idle_image (type, relation_id, image_url, sort_order)
SELECT 2, product_id, image_url, COALESCE(sort_order, 0)
FROM idle_product_image;

-- 备份旧表（防止误删）
RENAME TABLE idle_product_image TO idle_product_image_backup;

-- =============================================
-- 3. 创建完全缺失的表
-- =============================================

-- 3.1 购物车主表 idle_cart
CREATE TABLE IF NOT EXISTS idle_cart (
  cart_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL COMMENT '所属用户ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车主表';

-- 3.2 购物车明细 idle_cart_item
CREATE TABLE IF NOT EXISTS idle_cart_item (
  item_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  cart_id BIGINT NOT NULL COMMENT '购物车ID',
  product_id BIGINT NOT NULL COMMENT '商品ID',
  quantity INT NOT NULL DEFAULT 1 COMMENT '数量',
  selected TINYINT NOT NULL DEFAULT 1 COMMENT '是否选中 1选中 0未选',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_cart (cart_id),
  KEY idx_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车商品明细';

-- 3.3 收货地址 sys_address
CREATE TABLE IF NOT EXISTS sys_address (
  address_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL COMMENT '用户ID',
  receiver_name VARCHAR(50) NOT NULL COMMENT '收货人姓名',
  receiver_phone VARCHAR(20) NOT NULL COMMENT '收货人电话',
  province VARCHAR(50) DEFAULT NULL COMMENT '省',
  city VARCHAR(50) DEFAULT NULL COMMENT '市',
  district VARCHAR(50) DEFAULT NULL COMMENT '区',
  detail_address VARCHAR(255) DEFAULT NULL COMMENT '详细地址',
  is_default TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认 0否 1是',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收货地址表';

-- 3.4 收藏 sys_favorite
CREATE TABLE IF NOT EXISTS sys_favorite (
  favorite_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL COMMENT '用户ID',
  product_id BIGINT NOT NULL COMMENT '商品ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  UNIQUE KEY uk_user_product (user_id, product_id),
  KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品收藏表';

-- 3.5 关注 sys_follow
CREATE TABLE IF NOT EXISTS sys_follow (
  follow_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL COMMENT '关注者用户ID',
  follow_user_id BIGINT NOT NULL COMMENT '被关注用户ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  UNIQUE KEY uk_user_follow (user_id, follow_user_id),
  KEY idx_user (user_id),
  KEY idx_follow (follow_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户关注表';

-- =============================================
-- 完成提示
-- =============================================
SELECT 'ALL DDL EXECUTED SUCCESSFULLY' AS result;
