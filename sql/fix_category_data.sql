-- 修复 idle_category 表的中文乱码数据
-- 此脚本必须在 MySQL charset=utf8mb4 环境下执行

DELETE FROM idle_category;

INSERT INTO idle_category (category_id, category_name, category_desc, sort_order) VALUES
(1, '数码电子', '手机、电脑、相机等电子设备', 1),
(2, '书籍教材', '各类教材、课外书籍、小说等', 2),
(3, '生活用品', '寝室用品、日常用品等', 3),
(4, '服装鞋帽', '衣物、鞋子、帽子等', 4),
(5, '运动户外', '运动器材、户外用品等', 5),
(6, '其他', '其他类型的闲置物品', 6);
