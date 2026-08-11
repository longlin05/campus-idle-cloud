-- =============================================
-- user_notification 增加发送批次号 batch_no
-- 用途：管理员"全体用户/批量"发送时，同一批投递的所有行共享同一 batch_no，
--       管理端系统消息列表按批次去重显示（一次发送只显示一条），
--       删除时按批次整批逻辑删除。
-- 说明：旧数据 batch_no 为 NULL，管理端列表按 (type,title,content) 分组兼容，
--       不影响用户端消息中心的单条已读/删除。
-- =============================================

USE campus_user;

SET @col_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'campus_user' AND TABLE_NAME = 'user_notification' AND COLUMN_NAME = 'batch_no'
);

SET @ddl = IF(@col_exists = 0,
  'ALTER TABLE user_notification ADD COLUMN batch_no VARCHAR(64) DEFAULT NULL COMMENT ''发送批次号（同批发送共享，用于管理端去重）'' AFTER sender_id',
  'SELECT ''batch_no column already exists''');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'user_notification.batch_no 迁移完成' AS message;
