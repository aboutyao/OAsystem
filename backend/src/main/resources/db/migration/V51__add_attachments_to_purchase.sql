-- Add attachments field to oa_purchase
ALTER TABLE oa_purchase ADD COLUMN attachments JSON NULL COMMENT '附件列表' AFTER reason;
