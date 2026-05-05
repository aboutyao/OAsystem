-- V28: 为所有继承 BaseEntity 的表补全 created_at 和 updated_at 列
-- BaseEntity 定义了 createdAt/updatedAt 字段，但部分早期建表脚本未包含这两列
-- 同时修复部分表缺少 DEFAULT CURRENT_TIMESTAMP 的问题

-- 1. 逐个表添加缺失的 created_at/updated_at 列（已有则跳过）
DROP PROCEDURE IF EXISTS add_columns_if_missing;

CREATE PROCEDURE add_columns_if_missing()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE tbl_name VARCHAR(128);
    DECLARE cur CURSOR FOR
        SELECT DISTINCT TABLE_NAME
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = 'oa_system'
          AND TABLE_TYPE = 'BASE TABLE'
          AND TABLE_NAME NOT LIKE 'flyway%'
          AND TABLE_NAME NOT LIKE 'ACT_%'
          AND TABLE_NAME NOT LIKE 'FLW_%'
          AND TABLE_NAME NOT LIKE 'liquibase%'
        ORDER BY TABLE_NAME;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO tbl_name;
        IF done THEN
            LEAVE read_loop;
        END IF;

        IF NOT EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = 'oa_system' AND TABLE_NAME = tbl_name AND COLUMN_NAME = 'created_at'
        ) THEN
            SET @sql = CONCAT('ALTER TABLE `', tbl_name, '` ADD COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP');
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        ELSEIF NOT EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = 'oa_system' AND TABLE_NAME = tbl_name
              AND COLUMN_NAME = 'created_at' AND COLUMN_DEFAULT IS NOT NULL
        ) THEN
            -- 列存在但缺少 DEFAULT，补充默认值
            SET @sql = CONCAT('ALTER TABLE `', tbl_name, '` MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP');
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;

        IF NOT EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = 'oa_system' AND TABLE_NAME = tbl_name AND COLUMN_NAME = 'updated_at'
        ) THEN
            SET @sql = CONCAT('ALTER TABLE `', tbl_name, '` ADD COLUMN updated_at datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP');
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;
    END LOOP;
    CLOSE cur;
END;

CALL add_columns_if_missing();
DROP PROCEDURE IF EXISTS add_columns_if_missing;

-- 2. 补全 oa_notice 表缺失的 notice_no 和 notice_type 列
DROP PROCEDURE IF EXISTS add_notice_no;
CREATE PROCEDURE add_notice_no()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = 'oa_system' AND TABLE_NAME = 'oa_notice' AND COLUMN_NAME = 'notice_no'
    ) THEN
        ALTER TABLE oa_notice ADD COLUMN notice_no varchar(64) NOT NULL DEFAULT '' AFTER id;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = 'oa_system' AND TABLE_NAME = 'oa_notice' AND COLUMN_NAME = 'notice_type'
    ) THEN
        ALTER TABLE oa_notice ADD COLUMN notice_type varchar(64) DEFAULT NULL AFTER category;
    END IF;
END;
CALL add_notice_no();
DROP PROCEDURE IF EXISTS add_notice_no;
