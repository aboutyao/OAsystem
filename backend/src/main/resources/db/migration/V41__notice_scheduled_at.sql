-- Add scheduled_at column to oa_notice for scheduled publishing
SET @dbname = DATABASE();

SELECT COUNT(*) INTO @col_exists FROM information_schema.columns
WHERE table_schema = @dbname AND table_name = 'oa_notice' AND column_name = 'scheduled_at';
SET @sql = IF(@col_exists = 0, 'ALTER TABLE oa_notice ADD COLUMN scheduled_at DATETIME AFTER publish_at', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
