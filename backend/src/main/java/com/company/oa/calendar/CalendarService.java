package com.company.oa.calendar;

import com.company.oa.meeting.mapper.MeetingBookingMapper;
import com.company.oa.oa.mapper.OaLeaveMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@Service
public class CalendarService {

    private final OaLeaveMapper oaLeaveMapper;
    private final MeetingBookingMapper meetingBookingMapper;

    public CalendarService(OaLeaveMapper oaLeaveMapper,
                           MeetingBookingMapper meetingBookingMapper) {
        this.oaLeaveMapper = oaLeaveMapper;
        this.meetingBookingMapper = meetingBookingMapper;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> teamLeaves(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDateTime startOfMonth = ym.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = ym.atEndOfMonth().atTime(23, 59, 59);
        return oaLeaveMapper.selectTeamLeavesByMonth(startOfMonth, endOfMonth);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> meetings(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDateTime startOfMonth = ym.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = ym.atEndOfMonth().atTime(23, 59, 59);
        return meetingBookingMapper.selectMeetingsByMonth(startOfMonth, endOfMonth);
    }
}
