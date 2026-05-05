package com.company.oa.dashboard;

import com.company.oa.auth.AuthService;
import com.company.oa.auth.AuthUser;
import com.company.oa.message.MessageService;
import com.company.oa.notice.mapper.OaNoticeMapper;
import com.company.oa.workflow.mapper.WfCcRecordMapper;
import com.company.oa.workflow.mapper.WfProcessInstanceMapper;
import com.company.oa.workflow.mapper.WfTaskMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final WfTaskMapper wfTaskMapper;
    private final WfProcessInstanceMapper wfProcessInstanceMapper;
    private final WfCcRecordMapper wfCcRecordMapper;
    private final OaNoticeMapper oaNoticeMapper;
    private final AuthService authService;
    private final MessageService messageService;

    public DashboardService(WfTaskMapper wfTaskMapper,
                            WfProcessInstanceMapper wfProcessInstanceMapper,
                            WfCcRecordMapper wfCcRecordMapper,
                            OaNoticeMapper oaNoticeMapper,
                            AuthService authService,
                            MessageService messageService) {
        this.wfTaskMapper = wfTaskMapper;
        this.wfProcessInstanceMapper = wfProcessInstanceMapper;
        this.wfCcRecordMapper = wfCcRecordMapper;
        this.oaNoticeMapper = oaNoticeMapper;
        this.authService = authService;
        this.messageService = messageService;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> summary() {
        AuthUser user = authService.currentUser();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("todoCount", countTodo(user.id()));
        result.put("messageCount", messageService.countUnreadForUser(user.id()));
        result.put("startedCount", countStarted(user.id()));
        result.put("ccCount", countCc(user.id()));
        result.put("exceptionCount", countException(user));
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> todos(int limit) {
        AuthUser user = authService.currentUser();
        int n = clampLimit(limit, 5, 50);
        return wfTaskMapper.selectDashboardTodos(user.id(), n);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> myStarted(int limit) {
        AuthUser user = authService.currentUser();
        int n = clampLimit(limit, 5, 50);
        return wfProcessInstanceMapper.selectMyStarted(user.id(), n);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> ccToMe(int limit) {
        AuthUser user = authService.currentUser();
        int n = clampLimit(limit, 5, 50);
        return wfCcRecordMapper.selectDashboardCcToMe(user.id(), n);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> recentNotices(int limit) {
        int n = clampLimit(limit, 5, 50);
        return oaNoticeMapper.selectRecentNotices(n);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> quickActions() {
        AuthUser user = authService.currentUser();
        List<Map<String, Object>> all = new ArrayList<>();
        all.add(action("发起请假", "/oa/leaves/create", "leave:apply"));
        all.add(action("发起报销", "/oa/expenses/create", "expense:apply"));
        all.add(action("发起用章", "/oa/seals/create", "seal:apply"));
        all.add(action("发起采购", "/oa/purchases/create", "purchase:apply"));
        all.add(action("会议室预订", "/meetings/booking", "meeting:book"));
        all.add(action("我的合同", "/contracts", "contract:view"));
        if (user.permissions().contains("*")) {
            return all;
        }
        List<Map<String, Object>> visible = new ArrayList<>();
        for (Map<String, Object> a : all) {
            String perm = String.valueOf(a.get("requirePermission"));
            if (perm == null || perm.isBlank() || user.permissions().contains(perm)) {
                visible.add(a);
            }
        }
        return visible;
    }

    private static Map<String, Object> action(String label, String path, String requirePermission) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("label", label);
        m.put("path", path);
        m.put("requirePermission", requirePermission);
        return m;
    }

    private long countTodo(long userId) {
        Long n = wfTaskMapper.countTodoTasks(userId, "PENDING");
        return n == null ? 0L : n;
    }

    private long countStarted(long userId) {
        Long n = wfProcessInstanceMapper.countStartedByStarter(userId);
        return n == null ? 0L : n;
    }

    private long countCc(long userId) {
        Long n = wfCcRecordMapper.countUnreadByReceiver(userId);
        return n == null ? 0L : n;
    }

    private long countException(AuthUser user) {
        boolean superAdmin = user.permissions().contains("*");
        if (superAdmin) {
            Long n = wfProcessInstanceMapper.countExceptions();
            return n == null ? 0L : n;
        }
        Long n = wfProcessInstanceMapper.countExceptionByStarter(user.id());
        return n == null ? 0L : n;
    }

    private static int clampLimit(int limit, int defaultValue, int max) {
        if (limit < 1) {
            return defaultValue;
        }
        return Math.min(limit, max);
    }
}