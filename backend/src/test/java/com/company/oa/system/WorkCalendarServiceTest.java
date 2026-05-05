package com.company.oa.system;

import com.company.oa.BaseMySqlTest;
import com.company.oa.common.mapper.SysSequenceMapper;
import com.company.oa.common.service.SequenceService;
import com.company.oa.system.mapper.SysWorkCalendarMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class WorkCalendarServiceTest extends BaseMySqlTest {

    private WorkCalendarService service;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM sys_work_calendar");
        service = new WorkCalendarService(getMapper(SysWorkCalendarMapper.class),
                new SequenceService(getMapper(SysSequenceMapper.class)));
    }

    @Test
    void weekendsAreNotWorkdaysByDefault() {
        int n = service.countWorkdays(LocalDate.of(2026, 4, 20), LocalDate.of(2026, 4, 26));
        assertThat(n).isEqualTo(5);
    }

    @Test
    void holidayOverridesAreApplied() {
        service.upsert(new SystemDtos.WorkCalendarUpsertRequest(
                LocalDate.of(2026, 5, 1), "HOLIDAY", "劳动节"
        ));
        int n = service.countWorkdays(LocalDate.of(2026, 4, 27), LocalDate.of(2026, 5, 1));
        assertThat(n).isEqualTo(4);
    }

    @Test
    void adjustedWorkdayConvertsWeekendIntoWorkday() {
        service.upsert(new SystemDtos.WorkCalendarUpsertRequest(
                LocalDate.of(2026, 4, 25), "ADJUSTED_WORKDAY", "周六调休"
        ));
        int n = service.countWorkdays(LocalDate.of(2026, 4, 20), LocalDate.of(2026, 4, 26));
        assertThat(n).isEqualTo(6);
    }
}
