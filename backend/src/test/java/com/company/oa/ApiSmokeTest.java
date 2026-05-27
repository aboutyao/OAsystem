package com.company.oa;

import com.company.oa.auth.AuthController;
import com.company.oa.auth.AuthService;
import com.company.oa.auth.AuthUser;
import com.company.oa.auth.JwtBlacklistService;
import com.company.oa.auth.JwtService;
import com.company.oa.auth.LoginResponse;
import com.company.oa.common.web.ApiResponseAdvice;
import com.company.oa.common.web.GlobalExceptionHandler;
import com.company.oa.common.web.RequestIdFilter;
import com.company.oa.oa.expense.ExpenseController;
import com.company.oa.oa.expense.ExpenseService;
import com.company.oa.oa.purchase.PurchaseController;
import com.company.oa.oa.purchase.PurchaseService;
import com.company.oa.common.api.PageResponse;
import com.company.oa.ops.OpsHealthController;
import com.company.oa.ops.OpsService;
import com.company.oa.org.mapper.UserMapper;
import com.company.oa.permission.cache.PermissionCacheService;
import com.company.oa.system.cache.SystemCacheService;
import com.company.oa.workflow.WorkflowController;
import com.company.oa.workflow.WorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        OpsHealthController.class,
        AuthController.class,
        WorkflowController.class,
        ExpenseController.class,
        PurchaseController.class
}, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
        com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import({
        ApiResponseAdvice.class,
        GlobalExceptionHandler.class,
        RequestIdFilter.class
})
class ApiSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private WorkflowService workflowService;

    @MockBean
    private ExpenseService expenseService;

    @MockBean
    private PurchaseService purchaseService;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private JwtBlacklistService jwtBlacklistService;

    @MockBean
    private PermissionCacheService permissionCacheService;

    @MockBean
    private SystemCacheService systemCacheService;

    @MockBean
    private OpsService opsService;

    @BeforeEach
    void setUp() {
        AuthUser user = new AuthUser(1L, "admin", "系统管理员", 2L, "总经办", List.of("SUPER_ADMIN"), List.of("*"));
        when(authService.login(any())).thenReturn(new LoginResponse("test-token", 7200, user, false, false));
        when(authService.currentUser()).thenReturn(user);
        Map<String, Object> menuRow = new LinkedHashMap<>();
        menuRow.put("id", 1L);
        menuRow.put("parentId", null);
        menuRow.put("menuCode", "dashboard");
        menuRow.put("menuName", "工作台");
        menuRow.put("routePath", "/dashboard");
        menuRow.put("sortOrder", 1);
        when(authService.menusForCurrentUser()).thenReturn(List.of(menuRow));
        when(workflowService.todoTasks(anyLong(), anyLong())).thenReturn(new PageResponse<>(
                1,
                20,
                1,
                List.of(Map.of(
                        "id", 80001L,
                        "taskId", 80001L,
                        "title", "张三的差旅费报销",
                        "nodeName", "直属上级审批",
                        "status", "PENDING"
                ))
        ));
        when(expenseService.submit(anyLong())).thenReturn(Map.of(
                "id", 1L,
                "status", "APPROVING",
                "currentNodeName", "直属上级审批"
        ));
        when(purchaseService.submit(anyLong())).thenReturn(Map.of(
                "id", 1L,
                "status", "APPROVING",
                "currentNodeName", "直属上级审批"
        ));
    }

    @Test
    void healthUsesUnifiedResponse() throws Exception {
        mockMvc.perform(get("/api/ops/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("SUCCESS")))
                .andExpect(jsonPath("$.data.status", is("UP")));
    }

    @Test
    void loginReturnsToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "admin123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("SUCCESS")))
                .andExpect(jsonPath("$.data.accessToken", is("test-token")));
    }

    @Test
    void todoEndpointIsAvailable() throws Exception {
        mockMvc.perform(get("/api/workflow/tasks/todo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("SUCCESS")))
                .andExpect(jsonPath("$.data.items[0].status", is("PENDING")));
    }

    @Test
    void expenseSubmitReturnsWorkflowState() throws Exception {
        mockMvc.perform(post("/api/oa/expenses/1/submit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("SUCCESS")))
                .andExpect(jsonPath("$.data.status", is("APPROVING")))
                .andExpect(jsonPath("$.data.currentNodeName", is("直属上级审批")));
    }

    @Test
    void purchaseSubmitReturnsWorkflowState() throws Exception {
        mockMvc.perform(post("/api/oa/purchases/1/submit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("SUCCESS")))
                .andExpect(jsonPath("$.data.status", is("APPROVING")))
                .andExpect(jsonPath("$.data.currentNodeName", is("直属上级审批")));
    }
}
