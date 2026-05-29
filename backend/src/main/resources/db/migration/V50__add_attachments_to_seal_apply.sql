-- Add attachments field to oa_seal_apply
ALTER TABLE oa_seal_apply ADD COLUMN attachments JSON NULL COMMENT '附件列表' AFTER outFlag;
