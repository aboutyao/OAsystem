package com.company.oa.common.error;

/**
 * 统一错误码定义
 * 格式: 模块_功能_错误类型
 */
public final class ErrorCode {

    // ==================== 通用错误码 ====================
    public static final String BAD_REQUEST = "BAD_REQUEST";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String CONFLICT = "CONFLICT";
    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    public static final String TOO_MANY_REQUESTS = "TOO_MANY_REQUESTS";

    // ==================== 认证模块 ====================
    public static final String USER_ACCOUNT_LOCKED = "USER_ACCOUNT_LOCKED";
    public static final String USER_BAD_CREDENTIALS = "USER_BAD_CREDENTIALS";
    public static final String USER_PASSWORD_EXPIRED = "USER_PASSWORD_EXPIRED";
    public static final String USER_2FA_REQUIRED = "USER_2FA_REQUIRED";
    public static final String USER_2FA_INVALID = "USER_2FA_INVALID";
    public static final String TOKEN_EXPIRED = "TOKEN_EXPIRED";
    public static final String TOKEN_INVALID = "TOKEN_INVALID";

    // ==================== 组织模块 ====================
    public static final String DEPT_NOT_FOUND = "DEPT_NOT_FOUND";
    public static final String DEPT_HAS_CHILDREN = "DEPT_HAS_CHILDREN";
    public static final String DEPT_HAS_USERS = "DEPT_HAS_USERS";
    public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
    public static final String USER_ALREADY_EXISTS = "USER_ALREADY_EXISTS";

    // ==================== 权限模块 ====================
    public static final String ROLE_NOT_FOUND = "ROLE_NOT_FOUND";
    public static final String ROLE_IN_USE = "ROLE_IN_USE";
    public static final String MENU_NOT_FOUND = "MENU_NOT_FOUND";
    public static final String PERMISSION_DENIED = "PERMISSION_DENIED";

    // ==================== OA 模块 ====================
    public static final String LEAVE_NOT_FOUND = "LEAVE_NOT_FOUND";
    public static final String LEAVE_BALANCE_INSUFFICIENT = "LEAVE_BALANCE_INSUFFICIENT";
    public static final String EXPENSE_NOT_FOUND = "EXPENSE_NOT_FOUND";
    public static final String PURCHASE_NOT_FOUND = "PURCHASE_NOT_FOUND";
    public static final String SEAL_NOT_FOUND = "SEAL_NOT_FOUND";

    // ==================== 工作流模块 ====================
    public static final String WF_INSTANCE_NOT_FOUND = "WF_INSTANCE_NOT_FOUND";
    public static final String WF_TASK_NOT_FOUND = "WF_TASK_NOT_FOUND";
    public static final String WF_INSTANCE_CANNOT_WITHDRAW = "WF_INSTANCE_CANNOT_WITHDRAW";
    public static final String WF_INSTANCE_CANNOT_TERMINATE = "WF_INSTANCE_CANNOT_TERMINATE";
    public static final String WF_DELEGATION_CONFLICT = "WF_DELEGATION_CONFLICT";

    // ==================== 合同模块 ====================
    public static final String CONTRACT_NOT_FOUND = "CONTRACT_NOT_FOUND";
    public static final String CONTRACT_EXPIRED = "CONTRACT_EXPIRED";
    public static final String CONTRACT_STATUS_INVALID = "CONTRACT_STATUS_INVALID";

    // ==================== 文件模块 ====================
    public static final String FILE_NOT_FOUND = "FILE_NOT_FOUND";
    public static final String FILE_TOO_LARGE = "FILE_TOO_LARGE";
    public static final String FILE_TYPE_NOT_ALLOWED = "FILE_TYPE_NOT_ALLOWED";

    // ==================== 系统模块 ====================
    public static final String CONFIG_NOT_FOUND = "CONFIG_NOT_FOUND";
    public static final String DICT_NOT_FOUND = "DICT_NOT_FOUND";
    public static final String SEQUENCE_ERROR = "SEQUENCE_ERROR";

    // ==================== 预算模块 ====================
    public static final String BUDGET_NOT_FOUND = "BUDGET_NOT_FOUND";
    public static final String BUDGET_EXCEEDED = "BUDGET_EXCEEDED";

    private ErrorCode() {
    }
}
