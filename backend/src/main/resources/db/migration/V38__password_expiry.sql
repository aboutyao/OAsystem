ALTER TABLE org_user ADD COLUMN password_expires_at DATETIME DEFAULT NULL COMMENT '密码过期时间';
