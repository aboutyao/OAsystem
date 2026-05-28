package com.company.oa.meeting;

import com.company.oa.BaseSpringTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MeetingServiceTest extends BaseSpringTest {

    @Autowired
    private MeetingService meetingService;

    @Test
    void createAndListRooms() {
        var req = new MeetingDtos.RoomCreateRequest(
                "测试会议室", "A栋3楼", 10, "投影仪", "测试用会议室", "ENABLED"
        );
        var created = meetingService.createRoom(req);
        assertThat(created.get("roomName")).isEqualTo("测试会议室");
        assertThat(created.get("capacity")).isEqualTo(10);

        var rooms = meetingService.listRooms(1, 20);
        assertThat(rooms.total()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void updateRoom() {
        var req = new MeetingDtos.RoomCreateRequest(
                "更新前会议室", "B栋1楼", 5, "白板", null, "ENABLED"
        );
        var created = meetingService.createRoom(req);
        long id = ((Number) created.get("id")).longValue();

        var updateReq = new MeetingDtos.RoomUpdateRequest(
                "更新后会议室", "B栋2楼", 8, "投影仪+白板", "已更新", "ENABLED"
        );
        var updated = meetingService.updateRoom(id, updateReq);
        assertThat(updated.get("roomName")).isEqualTo("更新后会议室");
        assertThat(updated.get("capacity")).isEqualTo(8);
    }

    @Test
    void createBooking() {
        // Create a room first
        var roomReq = new MeetingDtos.RoomCreateRequest(
                "预约测试会议室", "C栋1楼", 20, "全套设备", null, "ENABLED"
        );
        var room = meetingService.createRoom(roomReq);
        long roomId = ((Number) room.get("id")).longValue();

        LocalDateTime now = LocalDateTime.now();
        var bookingReq = new MeetingDtos.BookingCreateRequest(
                roomId, "项目评审会", now.plusDays(1).withHour(10).withMinute(0),
                now.plusDays(1).withHour(11).withMinute(0), 15
        );
        var booking = meetingService.createBooking(bookingReq);
        assertThat(booking.get("title")).isEqualTo("项目评审会");
        assertThat(booking.get("status")).isEqualTo("BOOKED");
    }

    @Test
    void roomAvailability() {
        var roomReq = new MeetingDtos.RoomCreateRequest(
                "可用性测试会议室", "D栋1楼", 10, null, null, "ENABLED"
        );
        var room = meetingService.createRoom(roomReq);
        long roomId = ((Number) room.get("id")).longValue();

        var availability = meetingService.availability(roomId);
        assertThat(availability).isNotNull();
    }
}
