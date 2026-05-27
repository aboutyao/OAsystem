package com.company.oa.workflow;

import com.company.oa.BaseSpringTest;
import com.company.oa.auth.AuthUser;
import com.company.oa.common.api.PageResponse;
import com.company.oa.common.error.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkflowServiceTest extends BaseSpringTest {

    @Autowired
    private WorkflowService service;

    @BeforeEach
    void setUp() {
        seedInstance();
    }

    private void setSecurityUser(AuthUser user) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void seedInstance() {
        Timestamp now = Timestamp.from(Instant.now());
        jdbc.update("delete from wf_cc_record where wf_instance_id = 100");
        jdbc.update("delete from wf_task where wf_instance_id = 100");
        jdbc.update("delete from wf_process_instance where id = 100");
        jdbc.update("delete from org_user where id = 2");
        jdbc.update("delete from wf_delegation");
        jdbc.update("""
                insert into org_user (
                    id, username, password_hash, employee_no, real_name, main_dept_id, manager_user_id,
                    employee_status, account_status, login_fail_count, deleted, created_at, updated_at
                ) values (?,?,?,?,?,?,?,?,?,?,?,?,?)
                """,
                2L, "user2", "x", "E0002", "User2", 2L, 1L,
                "ACTIVE", "ENABLED", 0, 0, now, now);
        jdbc.update("""
                insert into wf_process_instance (
                    id, process_instance_id, template_id, process_version_id, business_type, business_id, title,
                    starter_id, starter_name_snapshot, starter_dept_id, starter_dept_name_snapshot,
                    current_node_name, status, started_at, ended_at
                ) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,null)
                """,
                100L, "PROC-100", 1L, 1L, "GENERIC", 999L, "测试流程",
                1L, "系统管理员", 2L, "总经办",
                "经理审批", "APPROVING", now);
    }

    @Test
    void addCcAndCcToMeAndMarkRead() {
        setSecurityUser(new AuthUser(1L, "admin", "管理员", 2L, "总经办",
                List.of("SUPER_ADMIN"), List.of("*")));
        Map<String, Object> r = service.addCc(new WorkflowDtos.CcAddRequest(100L, List.of(2L), "请知悉"));
        assertThat(r.get("created")).isInstanceOf(List.class);
        @SuppressWarnings("unchecked")
        List<Long> created = (List<Long>) r.get("created");
        assertThat(created).hasSize(1);

        setSecurityUser(new AuthUser(2L, "user2", "User2", 2L, "总经办",
                List.of(), List.of()));
        PageResponse<Map<String, Object>> page = service.ccToMe(1, 20);
        assertThat(page.total()).isEqualTo(1);
        Long ccId = ((Number) page.items().get(0).get("id")).longValue();
        assertThat(page.items().get(0).get("readAt")).isNull();

        Map<String, Object> read = service.markCcRead(ccId);
        assertThat(read.get("readAt")).isNotNull();
    }

    @Test
    void onlyStarterOrAdminCanAddCc() {
        setSecurityUser(new AuthUser(99L, "stranger", "陌生", 2L, "总经办",
                List.of(), List.of()));
        assertThatThrownBy(() -> service.addCc(new WorkflowDtos.CcAddRequest(100L, List.of(2L), null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void createAndCancelDelegation() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Map<String, Object> d = service.createDelegation(new WorkflowDtos.DelegateCreateRequest(
                2L, now, now.plusDays(7), "GENERIC", "出差"
        ));
        long id = ((Number) d.get("id")).longValue();
        assertThat(d.get("status")).isEqualTo("ACTIVE");

        Map<String, Object> cancelled = service.cancelDelegation(id);
        assertThat(cancelled.get("status")).isEqualTo("CANCELLED");

        assertThatThrownBy(() -> service.cancelDelegation(id)).isInstanceOf(BusinessException.class);
    }

    @Test
    void invalidDelegationParams() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        assertThatThrownBy(() -> service.createDelegation(new WorkflowDtos.DelegateCreateRequest(
                1L, now, now.plusDays(1), null, null)))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.createDelegation(new WorkflowDtos.DelegateCreateRequest(
                2L, now.plusDays(2), now, null, null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void listMyDelegations() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        service.createDelegation(new WorkflowDtos.DelegateCreateRequest(
                2L, now, now.plusDays(3), null, "原因A"
        ));
        PageResponse<Map<String, Object>> page = service.listMyDelegations(1, 20);
        assertThat(page.total()).isEqualTo(1);
    }

    @Test
    void listExceptionsFindsApprovingWithoutPendingTasks() {
        PageResponse<Map<String, Object>> page = service.listExceptions(1, 20);
        assertThat(page.total()).isEqualTo(1);
        assertThat(page.items().get(0).get("reason")).isEqualTo("NO_PENDING_TASK");
    }
}
