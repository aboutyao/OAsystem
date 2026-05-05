package com.company.oa.meeting;

import com.company.oa.auth.AuthService;
import com.company.oa.auth.AuthUser;
import com.company.oa.common.api.PageResponse;
import com.company.oa.common.error.BusinessException;
import com.company.oa.common.error.ErrorCode;
import com.company.oa.common.service.SequenceService;
import com.company.oa.entity.meeting.MeetingBooking;
import com.company.oa.entity.meeting.MeetingRoom;
import com.company.oa.entity.system.SysConfig;
import com.company.oa.meeting.mapper.MeetingBookingMapper;
import com.company.oa.meeting.mapper.MeetingRoomMapper;
import com.company.oa.system.mapper.SysConfigMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MeetingService {
    public static final String ENABLED = "ENABLED";
    public static final String DISABLED = "DISABLED";
    private static final String BOOKED = "BOOKED";
    private static final String CANCELLED = "CANCELLED";

    private final MeetingRoomMapper roomMapper;
    private final MeetingBookingMapper bookingMapper;
    private final SysConfigMapper sysConfigMapper;
    private final AuthService authService;
    private final SequenceService sequenceService;
    private final ObjectMapper objectMapper;

    public MeetingService(
            MeetingRoomMapper roomMapper,
            MeetingBookingMapper bookingMapper,
            SysConfigMapper sysConfigMapper,
            AuthService authService,
            SequenceService sequenceService,
            ObjectMapper objectMapper
    ) {
        this.roomMapper = roomMapper;
        this.bookingMapper = bookingMapper;
        this.sysConfigMapper = sysConfigMapper;
        this.authService = authService;
        this.sequenceService = sequenceService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listRooms(long page, long size) {
        long[] ps = clampPage(page, size);
        LambdaQueryWrapper<MeetingRoom> qw = new LambdaQueryWrapper<>();
        long total = roomMapper.selectCount(qw);
        qw.select(MeetingRoom::getId, MeetingRoom::getRoomName, MeetingRoom::getLocation, MeetingRoom::getCapacity,
                        MeetingRoom::getEquipment, MeetingRoom::getStatus, MeetingRoom::getRemark,
                        MeetingRoom::getCreatedAt, MeetingRoom::getUpdatedAt)
                .orderByDesc(MeetingRoom::getId)
                .last("limit " + ps[1] + " offset " + ((ps[0] - 1) * ps[1]));
        List<MeetingRoom> rooms = roomMapper.selectList(qw);
        List<Map<String, Object>> items = new ArrayList<>();
        for (MeetingRoom r : rooms) {
            items.add(toMap(r));
        }
        return new PageResponse<>(ps[0], ps[1], total, items);
    }

    @Transactional
    public Map<String, Object> createRoom(MeetingDtos.RoomCreateRequest req) {
        long id = sequenceService.nextId("meeting_room");
        LocalDateTime now = LocalDateTime.now();
        MeetingRoom entity = new MeetingRoom();
        entity.setId(id);
        entity.setRoomName(req.roomName());
        entity.setLocation(req.location());
        entity.setCapacity(req.capacity());
        entity.setEquipment(req.equipment());
        entity.setStatus(req.status());
        entity.setRemark(req.remark());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        roomMapper.insert(entity);
        return roomDetail(id);
    }

    @Transactional
    public Map<String, Object> updateRoom(long id, MeetingDtos.RoomUpdateRequest req) {
        loadRoom(id);
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<MeetingRoom> uw = new LambdaUpdateWrapper<>();
        uw.eq(MeetingRoom::getId, id)
                .set(MeetingRoom::getRoomName, req.roomName())
                .set(MeetingRoom::getLocation, req.location())
                .set(MeetingRoom::getCapacity, req.capacity())
                .set(MeetingRoom::getEquipment, req.equipment())
                .set(MeetingRoom::getStatus, req.status())
                .set(MeetingRoom::getRemark, req.remark())
                .set(MeetingRoom::getUpdatedAt, now)
                .setSql("version = version + 1");
        roomMapper.update(null, uw);
        return roomDetail(id);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> roomDetail(long id) {
        return loadRoom(id);
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listBookings(long page, long size, Long roomId) {
        AuthUser user = authService.currentUser();
        long[] ps = clampPage(page, size);
        Long organizerId = null;
        if (!user.permissions().contains("*")) {
            organizerId = user.id();
        }
        long total = bookingMapper.countBookings(roomId, organizerId);
        long offset = (ps[0] - 1) * ps[1];
        List<Map<String, Object>> items = bookingMapper.selectBookingsWithRoom(roomId, organizerId, ps[1], offset);
        return new PageResponse<>(ps[0], ps[1], total, items);
    }

    @Transactional
    public Map<String, Object> createBooking(MeetingDtos.BookingCreateRequest req) {
        AuthUser user = authService.currentUser();
        Map<String, Object> room = loadRoom(req.roomId());
        if (!ENABLED.equals(String.valueOf(room.get("status")))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "会议室不可用");
        }
        if (!req.endAt().isAfter(req.startAt())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "结束时间须晚于开始时间");
        }
        LocalDateTime start = req.startAt();
        LocalDateTime end = req.endAt();
        assertNoOverlap(req.roomId(), start, end, null);
        long id = sequenceService.nextId("meeting_booking");
        LocalDateTime now = LocalDateTime.now();
        MeetingBooking entity = new MeetingBooking();
        entity.setId(id);
        entity.setRoomId(req.roomId());
        entity.setTitle(req.title());
        entity.setStartAt(start);
        entity.setEndAt(end);
        entity.setOrganizerId(user.id());
        entity.setParticipantCount(req.participantCount());
        entity.setStatus(BOOKED);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        bookingMapper.insert(entity);
        return bookingDetail(id);
    }

    @Transactional
    public Map<String, Object> cancelBooking(long id, MeetingDtos.BookingCancelRequest req) {
        AuthUser user = authService.currentUser();
        Map<String, Object> row = loadBooking(id);
        if (!BOOKED.equals(String.valueOf(row.get("status")))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅已预约可取消");
        }
        Object orgVal = row.get("organizerId");
        if (orgVal == null) {
            orgVal = row.get("organizer_id");
        }
        long orgId = ((Number) orgVal).longValue();
        if (!user.permissions().contains("*") && user.id() != orgId) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅组织人可取消");
        }
        LocalDateTime now = LocalDateTime.now();
        String reason = req == null || req.cancelReason() == null ? null : req.cancelReason();
        LambdaUpdateWrapper<MeetingBooking> uw = new LambdaUpdateWrapper<>();
        uw.eq(MeetingBooking::getId, id)
                .set(MeetingBooking::getStatus, CANCELLED)
                .set(MeetingBooking::getCancelReason, reason)
                .set(MeetingBooking::getUpdatedAt, now);
        bookingMapper.update(null, uw);
        return bookingDetail(id);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> availability(long roomId) {
        loadRoom(roomId);
        LocalDateTime from = LocalDateTime.now();
        LocalDateTime to = LocalDateTime.now().plusDays(7);
        LambdaQueryWrapper<MeetingBooking> qw = new LambdaQueryWrapper<>();
        qw.select(MeetingBooking::getStartAt, MeetingBooking::getEndAt, MeetingBooking::getTitle, MeetingBooking::getStatus)
                .eq(MeetingBooking::getRoomId, roomId)
                .eq(MeetingBooking::getStatus, BOOKED)
                .gt(MeetingBooking::getEndAt, from)
                .lt(MeetingBooking::getStartAt, to)
                .orderByAsc(MeetingBooking::getStartAt);
        List<MeetingBooking> bookings = bookingMapper.selectList(qw);
        List<Map<String, Object>> result = new ArrayList<>();
        for (MeetingBooking b : bookings) {
            result.add(toMap(b));
        }
        return result;
    }

    private void assertNoOverlap(long roomId, LocalDateTime start, LocalDateTime end, Long excludeBookingId) {
        long n = bookingMapper.countOverlapping(roomId, BOOKED, start, end, excludeBookingId);
        if (n > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "该时段已有预约");
        }
    }

    private Map<String, Object> loadRoom(long id) {
        MeetingRoom entity = roomMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "会议室不存在");
        }
        Map<String, Object> map = toMap(entity);
        map.remove("deleted");
        return new LinkedHashMap<>(map);
    }

    private Map<String, Object> loadBooking(long id) {
        MeetingBooking entity = bookingMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "预约不存在");
        }
        Map<String, Object> map = toMap(entity);
        map.remove("deleted");
        return new LinkedHashMap<>(map);
    }

    private Map<String, Object> bookingDetail(long id) {
        Map<String, Object> row = bookingMapper.selectBookingWithRoom(id);
        if (row == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "预约不存在");
        }
        return new LinkedHashMap<>(row);
    }

    private long[] clampPage(long page, long size) {
        int def = intConfig("paging.defaultSize", 20);
        int max = intConfig("paging.maxSize", 100);
        long p = page < 1 ? 1 : page;
        long s = size < 1 ? def : size;
        if (s > max) {
            s = max;
        }
        return new long[]{p, s};
    }

    private int intConfig(String key, int defaultValue) {
        LambdaQueryWrapper<SysConfig> qw = new LambdaQueryWrapper<>();
        qw.select(SysConfig::getConfigValue).eq(SysConfig::getConfigKey, key);
        List<SysConfig> configs = sysConfigMapper.selectList(qw);
        if (configs.isEmpty()) {
            return defaultValue;
        }
        return Integer.parseInt(configs.get(0).getConfigValue());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Object entity) {
        return objectMapper.convertValue(entity, Map.class);
    }
}
