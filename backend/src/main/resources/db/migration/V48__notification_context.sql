-- Add context_json column to msg_message for contextual notifications (urgency, SLA, suggested action, etc.)
ALTER TABLE msg_message ADD COLUMN context_json TEXT NULL;
