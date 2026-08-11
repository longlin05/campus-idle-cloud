-- =============================================
-- 迁移脚本：为 order_info 表添加商品快照字段
-- 适用于已存在的数据库，无需重建表
-- =============================================

-- 1. campus_order 数据库
USE campus_order;

ALTER TABLE `order_info`
    ADD COLUMN `product_name` varchar(200) DEFAULT NULL COMMENT '商品名称快照' AFTER `product_id`,
    ADD COLUMN `product_image` varchar(512) DEFAULT NULL COMMENT '商品主图快照' AFTER `product_name`,
    ADD COLUMN `product_price` decimal(10,2) DEFAULT NULL COMMENT '商品单价快照' AFTER `product_image`;

-- 2. 回填历史订单的商品快照（从 idle_product 副本表关联更新）
UPDATE `order_info` o
LEFT JOIN `idle_product` p ON o.product_id = p.product_id
SET
    o.product_name = IFNULL(o.product_name, p.title),
    o.product_price = IFNULL(o.product_price, p.price),
    o.product_image = IFNULL(o.product_image, (
        SELECT i.image_url FROM idle_image i
        WHERE i.type = 1 AND i.relation_id = o.product_id
        ORDER BY i.sort_order ASC LIMIT 1
    ))
WHERE o.product_name IS NULL;

SELECT 'campus_order: order_info 快照字段迁移完成' AS message;

-- 3. 如果 campus_idle 数据库也有 order_info 副本表，同步迁移
--（根据 init.sql 第二段，campus_idle 库可能也有 order_info 表）
USE campus_idle;

-- 仅在表存在时执行（安全处理）
SET @table_exists = (SELECT COUNT(*) FROM information_schema.tables
    WHERE table_schema = 'campus_idle' AND table_name = 'order_info');

SET @sql = IF(@table_exists > 0,
    'ALTER TABLE order_info ADD COLUMN IF NOT EXISTS product_name varchar(200) DEFAULT NULL COMMENT ''商品名称快照'' AFTER product_id',
    'SELECT ''campus_idle: order_info 表不存在，跳过'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(@table_exists > 0,
    'ALTER TABLE order_info ADD COLUMN IF NOT EXISTS product_image varchar(512) DEFAULT NULL COMMENT ''商品主图快照'' AFTER product_name',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(@table_exists > 0,
    'ALTER TABLE order_info ADD COLUMN IF NOT EXISTS product_price decimal(10,2) DEFAULT NULL COMMENT ''商品单价快照'' AFTER product_image',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT '迁移完成' AS final_result;
