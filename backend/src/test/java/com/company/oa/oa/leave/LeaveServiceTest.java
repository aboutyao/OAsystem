package com.company.oa.oa.leave;

import com.company.oa.BaseSpringTest;
import com.company.oa.common.error.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LeaveServiceTest extends BaseSpringTest {

    @Autowired
    private LeaveService leaveService;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM oa_leave WHERE created_by = 1 AND leave_type = 'TEST_LEAVE'");
    }

    @Test
    void createSubmitWithdraw_setsWithdrawnAndClearsWorkflowIds() {
        Map<String, Object> created = leaveService.create(new LeaveDtos.LeaveCreateRequest(
                "ANNUAL",
                LocalDateTime.parse("2026-04-28T09:00:00"),
                LocalDateTime.parse("2026-04-28T18:00:00"),
                BigDecimal.valueOf(8),
                BigDecimal.ONE,
                "休假",
                null
        ));
        long id = ((Number) created.get("id")).longValue();
        assertThat(created.get("status")).isEqualTo("DRAFT");

        Map<String, Object> submitted = leaveService.submit(id);
        assertThat(submitted.get("status")).isEqualTo("APPROVING");
        assertThat(submitted.get("currentNodeName")).isNotNull();

        leaveService.withdrawLeave(id);

        Map<String, Object> after = leaveService.detail(id);
        assertThat(after.get("status")).isEqualTo("WITHDRAWN");
        assertThat(after.get("wfInstanceId")).isNull();
        assertThat(after.get("processInstanceId")).isNull();
    }

    @Test
    void cancelFromDraft_setsCancelledWithoutWorkflowCall() {
        Map<String, Object> created = leaveService.create(new LeaveDtos.LeaveCreateRequest(
                "SICK",
                LocalDateTime.parse("2026-05-01T09:00:00"),
                LocalDateTime.parse("2026-05-01T12:00:00"),
                BigDecimal.valueOf(3),
                new BigDecimal("0.375"),
                "就医",
                null
        ));
        long id = ((Number) created.get("id")).longValue();

        Map<String, Object> cancelled = leaveService.cancelLeave(id);
        assertThat(cancelled.get("status")).isEqualTo("CANCELLED");
    }

    @Test
    void submitTwiceOnSameLeave_fails() {
        Map<String, Object> created = leaveService.create(new LeaveDtos.LeaveCreateRequest(
                "PERSONAL",
                LocalDateTime.parse("2026-06-01T09:00:00"),
                LocalDateTime.parse("2026-06-01T10:00:00"),
                BigDecimal.ONE,
                new BigDecimal("0.125"),
                null,
                null
        ));
        long id = ((Number) created.get("id")).longValue();
        leaveService.submit(id);

        assertThatThrownBy(() -> leaveService.submit(id))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("草稿");
    }
}
