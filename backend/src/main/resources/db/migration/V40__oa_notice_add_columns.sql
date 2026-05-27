-- Add missing columns to oa_notice table
SET @dbname = DATABASE();

-- Add expire_at if not exists
SELECT COUNT(*) INTO @col_exists FROM information_schema.columns
WHERE table_schema = @dbname AND table_name = 'oa_notice' AND column_name = 'expire_at';
SET @sql = IF(@col_exists = 0, 'ALTER TABLE oa_notice ADD COLUMN expire_at DATETIME AFTER withdraw_at', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add created_name_snapshot if not exists
SELECT COUNT(*) INTO @col_exists FROM information_schema.columns
WHERE table_schema = @dbname AND table_name = 'oa_notice' AND column_name = 'created_name_snapshot';
SET @sql = IF(@col_exists = 0, 'ALTER TABLE oa_notice ADD COLUMN created_name_snapshot VARCHAR(128) AFTER created_by', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
