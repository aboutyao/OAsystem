-- Add parent_record_id to wf_task_record for comment reply threading
ALTER TABLE wf_task_record ADD COLUMN parent_record_id BIGINT DEFAULT NULL;

CREATE INDEX idx_wf_task_record_parent_record_id ON wf_task_record (parent_record_id);
