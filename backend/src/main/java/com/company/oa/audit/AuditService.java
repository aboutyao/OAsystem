package com.company.oa.audit;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.oa.audit.mapper.AuditLoginLogMapper;
import com.company.oa.audit.mapper.AuditOperationLogMapper;
import com.company.oa.common.api.PageResponse;
import com.company.oa.entity.audit.AuditLoginLog;
import com.company.oa.entity.audit.AuditOperationLog;
import com.company.oa.common.service.OaUtils;
import com.company.oa.common.service.PaginationHelper;
import com.company.oa.common.service.SequenceService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AuditService {
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";

    private final AuditLoginLogMapper loginLogMapper;
    private final AuditOperationLogMapper operationLogMapper;
    private final PaginationHelper paginationHelper;
    private final SequenceService sequenceService;

    public AuditService(AuditLoginLogMapper loginLogMapper,
                        AuditOperationLogMapper operationLogMapper,
                        PaginationHelper paginationHelper,
                        SequenceService sequenceService) {
        this.loginLogMapper = loginLogMapper;
        this.operationLogMapper = operationLogMapper;
        this.paginationHelper = paginationHelper;
        this.sequenceService = sequenceService;
    }

    @Transactional
    public void recordLoginSuccess(Long userId, String username, String ip, String userAgent) {
        AuditLoginLog entity = new AuditLoginLog();
        entity.setId(sequenceService.nextId("audit_login_log"));
        entity.setUserId(userId);
        entity.setUsername(username);
        entity.setIpAddress(OaUtils.truncate(ip, 64));
        entity.setUserAgent(OaUtils.truncate(userAgent, 500));
        entity.setLoginResult(SUCCESS);
        entity.setFailReason(null);
        entity.setLoggedAt(LocalDateTime.now());
        loginLogMapper.insert(entity);
    }

    @Transactional
    public void recordLoginFailure(String username, String reason, String ip, String userAgent) {
        AuditLoginLog entity = new AuditLoginLog();
        entity.setId(sequenceService.nextId("audit_login_log"));
        entity.setUserId(null);
        entity.setUsername(username);
        entity.setIpAddress(OaUtils.truncate(ip, 64));
        entity.setUserAgent(OaUtils.truncate(userAgent, 500));
        entity.setLoginResult(FAILED);
        entity.setFailReason(OaUtils.truncate(reason, 500));
        entity.setLoggedAt(LocalDateTime.now());
        loginLogMapper.insert(entity);
    }

    /**
     * 业务侧调用：在主流程内 try/catch 包裹，确保审计写入异常不影响业务事务。
     */
    public void safeRecordOperation(
            Long operatorId,
            String operationType,
            String businessType,
            Long businessId,
            String result,
            String errorMessage
    ) {
        try {
            recordOperation(null, operatorId, operationType, businessType, businessId,
                    null, null, null, result, errorMessage, null);
        } catch (Exception ignore) {
            // 审计写入失败不应阻塞业务
        }
    }

    @Transactional
    public void recordOperation(
            String requestId,
            Long operatorId,
            String operationType,
            String businessType,
            Long businessId,
            String requestMethod,
            String requestUri,
            String requestParams,
            String result,
            String errorMessage,
            String ip
    ) {
        AuditOperationLog entity = new AuditOperationLog();
        entity.setId(sequenceService.nextId("audit_operation_log"));
        entity.setRequestId(OaUtils.truncate(requestId, 64));
        entity.setOperatorId(operatorId);
        entity.setOperationType(OaUtils.truncate(operationType, 64));
        entity.setBusinessType(OaUtils.truncate(businessType, 64));
        entity.setBusinessId(businessId);
        entity.setRequestMethod(OaUtils.truncate(requestMethod, 16));
        entity.setRequestUri(OaUtils.truncate(requestUri, 500));
        entity.setRequestParams(requestParams);
        entity.setResult(result == null ? FAILED : result);
        entity.setErrorMessage(OaUtils.truncate(errorMessage, 1000));
        entity.setIpAddress(OaUtils.truncate(ip, 64));
        entity.setOperatedAt(LocalDateTime.now());
        operationLogMapper.insert(entity);
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listLoginLogs(long page, long size, String username, String result) {
        long[] ps = paginationHelper.clamp(page, size);

        LambdaQueryWrapper<AuditLoginLog> wrapper = new LambdaQueryWrapper<>();
        if (username != null && !username.isBlank()) {
            wrapper.like(AuditLoginLog::getUsername, username.trim());
        }
        if (result != null && !result.isBlank()) {
            wrapper.eq(AuditLoginLog::getLoginResult, result);
        }

        long total = loginLogMapper.selectCount(wrapper);

        wrapper.orderByDesc(AuditLoginLog::getLoggedAt);
        wrapper.orderByDesc(AuditLoginLog::getId);
        Page<AuditLoginLog> pageParam = new Page<>(ps[0], ps[1]);
        List<AuditLoginLog> records = loginLogMapper.selectPage(pageParam, wrapper).getRecords();

        List<Map<String, Object>> items = records.stream()
                .map(this::toMap)
                .collect(Collectors.toList());
        return new PageResponse<>(ps[0], ps[1], total, items);
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listOperationLogs(
            long page, long size, String businessType, String result, Long operatorId
    ) {
        long[] ps = paginationHelper.clamp(page, size);

        LambdaQueryWrapper<AuditOperationLog> wrapper = new LambdaQueryWrapper<>();
        if (businessType != null && !businessType.isBlank()) {
            wrapper.eq(AuditOperationLog::getBusinessType, businessType);
        }
        if (result != null && !result.isBlank()) {
            wrapper.eq(AuditOperationLog::getResult, result);
        }
        if (operatorId != null) {
            wrapper.eq(AuditOperationLog::getOperatorId, operatorId);
        }

        long total = operationLogMapper.selectCount(wrapper);

        wrapper.orderByDesc(AuditOperationLog::getOperatedAt);
        wrapper.orderByDesc(AuditOperationLog::getId);
        Page<AuditOperationLog> pageParam = new Page<>(ps[0], ps[1]);
        List<AuditOperationLog> records = operationLogMapper.selectPage(pageParam, wrapper).getRecords();

        List<Map<String, Object>> items = records.stream()
                .map(this::toMap)
                .collect(Collectors.toList());
        return new PageResponse<>(ps[0], ps[1], total, items);
    }

    private Map<String, Object> toMap(Object obj) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (obj instanceof AuditLoginLog log) {
            map.put("id", log.getId());
            map.put("userId", log.getUserId());
            map.put("username", log.getUsername());
            map.put("ipAddress", log.getIpAddress());
            map.put("userAgent", log.getUserAgent());
            map.put("loginResult", log.getLoginResult());
            map.put("failReason", log.getFailReason());
            map.put("loggedAt", log.getLoggedAt() == null ? null : log.getLoggedAt().toString());
        } else if (obj instanceof AuditOperationLog log) {
            map.put("id", log.getId());
            map.put("requestId", log.getRequestId());
            map.put("operatorId", log.getOperatorId());
            map.put("operationType", log.getOperationType());
            map.put("businessType", log.getBusinessType());
            map.put("businessId", log.getBusinessId());
            map.put("requestMethod", log.getRequestMethod());
            map.put("requestUri", log.getRequestUri());
            map.put("requestParams", log.getRequestParams());
            map.put("result", log.getResult());
            map.put("errorMessage", log.getErrorMessage());
            map.put("ipAddress", log.getIpAddress());
            map.put("operatedAt", log.getOperatedAt() == null ? null : log.getOperatedAt().toString());
        }
        return map;
    }

}
