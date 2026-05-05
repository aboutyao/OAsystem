CREATE TABLE meeting_room (
    id BIGINT NOT NULL,
    room_name VARCHAR(128) NOT NULL,
    location VARCHAR(255),
    capacity INT NOT NULL DEFAULT 0,
    equipment VARCHAR(500),
    status VARCHAR(32) NOT NULL,
    remark VARCHAR(500),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE INDEX idx_meeting_room_status ON meeting_room (status);

CREATE TABLE meeting_booking (
    id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    start_at DATETIME NOT NULL,
    end_at DATETIME NOT NULL,
    organizer_id BIGINT NOT NULL,
    participant_count INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    cancel_reason VARCHAR(500),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_meeting_booking_room_time ON meeting_booking (room_id, start_at, end_at);
CREATE INDEX idx_meeting_booking_org ON meeting_booking (organizer_id);
CREATE INDEX idx_meeting_booking_status ON meeting_booking (status);

INSERT INTO perm_menu (
    id, parent_id, menu_code, menu_name, route_path, component, icon, sort_order, visible, status
) VALUES
    (18, NULL, 'meetings', '会议室', '/meetings', 'common/PlaceholderView', NULL, 55, 1, 'ENABLED');

INSERT INTO perm_role_menu (id, role_id, menu_id) VALUES
    (27, 1, 18),
    (28, 3, 18);
