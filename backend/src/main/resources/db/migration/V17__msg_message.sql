CREATE TABLE msg_message (
    id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    message_type VARCHAR(32) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    business_type VARCHAR(64),
    business_id BIGINT,
    wf_instance_id BIGINT,
    read_status VARCHAR(32) NOT NULL,
    archive_status VARCHAR(32) NOT NULL,
    created_at DATETIME NOT NULL,
    read_at DATETIME,
    PRIMARY KEY (id)
);

CREATE INDEX idx_msg_message_receiver_read ON msg_message (receiver_id, read_status);
CREATE INDEX idx_msg_message_business ON msg_message (business_type, business_id);
CREATE INDEX idx_msg_message_created ON msg_message (created_at);

INSERT INTO perm_menu (
    id, parent_id, menu_code, menu_name, route_path, component, icon, sort_order, visible, status
) VALUES
    (21, NULL, 'messages', '消息中心', '/messages', 'common/PlaceholderView', NULL, 4, 1, 'ENABLED');

INSERT INTO perm_role_menu (id, role_id, menu_id) VALUES
    (33, 1, 21),
    (34, 3, 21);
