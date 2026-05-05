package com.company.oa.meeting;

import com.company.oa.common.api.PageResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/meetings")
public class MeetingController {
    private final MeetingService meetingService;

    public MeetingController(MeetingService meetingService) {
        this.meetingService = meetingService;
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/rooms")
    public PageResponse<Map<String, Object>> rooms(@RequestParam(defaultValue = "1") long page, @RequestParam(defaultValue = "20") long size) {
        return meetingService.listRooms(page, size);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PostMapping("/rooms")
    public Map<String, Object> createRoom(@Valid @RequestBody MeetingDtos.RoomCreateRequest request) {
        return meetingService.createRoom(request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PutMapping("/rooms/{id}")
    public Map<String, Object> updateRoom(@PathVariable long id, @Valid @RequestBody MeetingDtos.RoomUpdateRequest request) {
        return meetingService.updateRoom(id, request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/bookings")
    public PageResponse<Map<String, Object>> bookings(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) Long roomId
    ) {
        return meetingService.listBookings(page, size, roomId);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PostMapping("/bookings")
    public Map<String, Object> createBooking(@Valid @RequestBody MeetingDtos.BookingCreateRequest request) {
        return meetingService.createBooking(request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PostMapping("/bookings/{id}/cancel")
    public Map<String, Object> cancelBooking(
            @PathVariable long id,
            @RequestBody(required = false) MeetingDtos.BookingCancelRequest body
    ) {
        return meetingService.cancelBooking(id, body != null ? body : new MeetingDtos.BookingCancelRequest(null));
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/rooms/{id}/availability")
    public List<Map<String, Object>> availability(@PathVariable long id) {
        return meetingService.availability(id);
    }
}
