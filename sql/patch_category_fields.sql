-- =============================================
-- 补齐 idle_category 缺失列 (status, update_time, is_deleted)
-- 对应 P1 中 common.entity.Category 新增的字段
-- 需要在所有包含 idle_category 表的库执行：campus_item, campus_admin, campus_order
-- =============================================

-- campus_item 库
USE campus_item;

-- 先判断列是否存在，不存在才添加（防止重复执行报错）
SET @dbname = DATABASE();
SET @tablename = 'idle_category';

SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'status');
SET @sql = IF(@col_exists = 0,
              'ALTER TABLE idle_category ADD COLUMN status TINYINT NOT NULL DEFAULT 1 COMMENT ''状态 1启用 0禁用'' AFTER sort_order',
              'SELECT ''status already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'update_time');
SET @sql = IF(@col_exists = 0,
              'ALTER TABLE idle_category ADD COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'' AFTER create_time',
              'SELECT ''update_time already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'is_deleted');
SET @sql = IF(@col_exists = 0,
              'ALTER TABLE idle_category ADD COLUMN is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT ''逻辑删除 0未删 1已删'' AFTER update_time',
              'SELECT ''is_deleted already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- campus_admin 库（如果 idle_category 存在也执行）
USE campus_admin;

SET @dbname = DATABASE();
SET @tablename = 'idle_category';

SET @table_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
                     WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename);
SET @sql = IF(@table_exists > 0,
              'SELECT ''idle_category exists in campus_admin, adding columns if needed''',
              'SELECT ''idle_category not in campus_admin''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'status');
SET @sql = IF(@table_exists > 0 AND @col_exists = 0,
              'ALTER TABLE idle_category ADD COLUMN status TINYINT NOT NULL DEFAULT 1 COMMENT ''状态 1启用 0禁用'' AFTER sort_order',
              'SELECT ''skip''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'update_time');
SET @sql = IF(@table_exists > 0 AND @col_exists = 0,
              'ALTER TABLE idle_category ADD COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'' AFTER create_time',
              'SELECT ''skip''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'is_deleted');
SET @sql = IF(@table_exists > 0 AND @col_exists = 0,
              'ALTER TABLE idle_category ADD COLUMN is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT ''逻辑删除 0未删 1已删'' AFTER update_time',
              'SELECT ''skip''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- campus_order 库（如果 idle_category 存在也执行）
USE campus_order;

SET @dbname = DATABASE();
SET @tablename = 'idle_category';

SET @table_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
                     WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename);
SET @sql = IF(@table_exists > 0,
              'SELECT ''idle_category exists in campus_order, adding columns if needed''',
              'SELECT ''idle_category not in campus_order''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'status');
SET @sql = IF(@table_exists > 0 AND @col_exists = 0,
              'ALTER TABLE idle_category ADD COLUMN status TINYINT NOT NULL DEFAULT 1 COMMENT ''状态 1启用 0禁用'' AFTER sort_order',
              'SELECT ''skip''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'update_time');
SET @sql = IF(@table_exists > 0 AND @col_exists = 0,
              'ALTER TABLE idle_category ADD COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'' AFTER create_time',
              'SELECT ''skip''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'is_deleted');
SET @sql = IF(@table_exists > 0 AND @col_exists = 0,
              'ALTER TABLE idle_category ADD COLUMN is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT ''逻辑删除 0未删 1已删'' AFTER update_time',
              'SELECT ''skip''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 修复 idle_category 历史数据：is_deleted 为 NULL 的改成 0（如果表有数据）
USE campus_item;
UPDATE idle_category SET is_deleted = 0 WHERE is_deleted IS NULL;
USE campus_admin;
UPDATE idle_category SET is_deleted = 0 WHERE is_deleted IS NULL;
USE campus_order;
UPDATE idle_category SET is_deleted = 0 WHERE is_deleted IS NULL;
