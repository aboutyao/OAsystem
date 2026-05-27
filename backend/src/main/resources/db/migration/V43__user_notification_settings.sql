-- Create user notification settings table
CREATE TABLE IF NOT EXISTS user_notification_settings (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    enable_email TINYINT NOT NULL DEFAULT 1,
    enable_sse TINYINT NOT NULL DEFAULT 1,
    enable_dnd TINYINT NOT NULL DEFAULT 0,
    dnd_start VARCHAR(5) DEFAULT NULL,
    dnd_end VARCHAR(5) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX uk_notification_settings_user (user_id)
);
