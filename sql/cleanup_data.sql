-- =============================================
-- 清理商品和订单数据脚本
-- =============================================

-- 1. 清理 campus_item 数据库中的商品数据
USE campus_item;

-- 删除商品图片（type=2 商品图片）
DELETE FROM idle_image WHERE type = 2;

-- 删除所有商品
DELETE FROM idle_product;

-- 重置自增ID
ALTER TABLE idle_product AUTO_INCREMENT = 1;
ALTER TABLE idle_image AUTO_INCREMENT = 1;

SELECT 'campus_item 商品数据清理完成' AS message;

-- =============================================

-- 2. 清理 campus_order 数据库中的订单数据
USE campus_order;

-- 删除所有订单
DELETE FROM order_info;

-- 删除商品副本数据
DELETE FROM idle_product;

-- 重置自增ID
ALTER TABLE order_info AUTO_INCREMENT = 1;
ALTER TABLE idle_product AUTO_INCREMENT = 1;

SELECT 'campus_order 订单数据清理完成' AS message;

-- =============================================

-- 3. 清理 campus_user 数据库中的购物车数据
USE campus_user;

-- 删除所有购物车记录
DELETE FROM idle_cart;
DELETE FROM idle_cart_item;

-- 重置自增ID
ALTER TABLE idle_cart AUTO_INCREMENT = 1;
ALTER TABLE idle_cart_item AUTO_INCREMENT = 1;

SELECT 'campus_user 购物车数据清理完成' AS message;

-- =============================================

SELECT '所有数据清理完成！' AS final_result;
