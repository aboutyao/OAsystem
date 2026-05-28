package com.company.oa.workflow;

import com.company.oa.audit.AuditService;
import com.company.oa.auth.AuthService;
import com.company.oa.auth.AuthUser;
import com.company.oa.common.api.PageResponse;
import com.company.oa.common.error.BusinessException;
import com.company.oa.common.error.ErrorCode;
import com.company.oa.common.service.PaginationHelper;
import com.company.oa.common.service.SequenceService;
import com.company.oa.entity.wf.WfDelegation;
import com.company.oa.org.mapper.UserMapper;
import com.company.oa.workflow.mapper.WfDelegationMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WorkflowDelegationService {
    private static final String DELEGATION_ACTIVE = "ACTIVE";
    private static final String DELEGATION_CANCELLED = "CANCELLED";

    private final WfDelegationMapper delegationMapper;
    private final AuthService authService;
    private final AuditService auditService;
    private final SequenceService sequenceService;
    private final UserMapper userMapper;
    private final PaginationHelper paginationHelper;

    public WorkflowDelegationService(
            WfDelegationMapper delegationMapper,
            AuthService authService,
            AuditService auditService,
            SequenceService sequenceService,
            UserMapper userMapper,
            PaginationHelper paginationHelper
    ) {
        this.delegationMapper = delegationMapper;
        this.authService = authService;
        this.auditService = auditService;
        this.sequenceService = sequenceService;
        this.userMapper = userMapper;
        this.paginationHelper = paginationHelper;
    }

    @Transactional
    public Map<String, Object> createDelegation(WorkflowDtos.DelegateCreateRequest req) {
        AuthUser user = authService.currentUser();
        if (req.delegateeId() == null || req.delegateeId() == user.id()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "委托人不能为自己");
        }
        Long exists = userMapper.selectCount(
                new LambdaQueryWrapper<com.company.oa.entity.org.User>()
                        .eq(com.company.oa.entity.org.User::getId, req.delegateeId())
                        .eq(com.company.oa.entity.org.User::getDeleted, 0)
        );
        if (exists == null || exists == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "受托人不存在");
        }
        java.time.OffsetDateTime startAt = req.startAt();
        java.time.OffsetDateTime endAt = req.endAt();
        if (startAt == null || endAt == null || !endAt.isAfter(startAt)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "委托时间区间不合法");
        }
        LocalDateTime now = LocalDateTime.now();
        WfDelegation entity = new WfDelegation();
        entity.setId(sequenceService.nextId("wf_delegation"));
        entity.setDelegatorId(user.id());
        entity.setDelegateeId(req.delegateeId());
        entity.setBusinessScope(req.businessScope());
        entity.setStartAt(startAt.toLocalDateTime());
        entity.setEndAt(endAt.toLocalDateTime());
        entity.setStatus(DELEGATION_ACTIVE);
        entity.setReason(req.reason());
        entity.setCreatedAt(now);
        delegationMapper.insert(entity);
        auditService.safeRecordOperation(user.id(), "WF_DELEGATE_CREATE", "WF_DELEGATION", entity.getId(), AuditService.SUCCESS, null);
        return Map.of("id", entity.getId(), "status", DELEGATION_ACTIVE, "createdAt", now.toString());
    }

    @Transactional
    public Map<String, Object> cancelDelegation(long id) {
        AuthUser user = authService.currentUser();
        WfDelegation d = delegationMapper.selectById(id);
        if (d == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "委托不存在");
        }
        boolean superAdmin = user.permissions().contains("*");
        if (!superAdmin && !user.id().equals(d.getDelegatorId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅委托人或超级管理员可取消委托");
        }
        if (!DELEGATION_ACTIVE.equals(d.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅生效中的委托可取消");
        }
        LocalDateTime now = LocalDateTime.now();
        WfDelegation upd = new WfDelegation();
        upd.setId(id);
        upd.setStatus(DELEGATION_CANCELLED);
        upd.setCancelledAt(now);
        int n = delegationMapper.update(upd,
                new LambdaQueryWrapper<WfDelegation>()
                        .eq(WfDelegation::getId, id)
                        .eq(WfDelegation::getStatus, DELEGATION_ACTIVE)
        );
        if (n == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "委托状态已变更");
        }
        auditService.safeRecordOperation(user.id(), "WF_DELEGATE_CANCEL", "WF_DELEGATION", id, AuditService.SUCCESS, null);
        return Map.of("id", id, "status", DELEGATION_CANCELLED);
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listMyDelegations(long page, long size) {
        AuthUser user = authService.currentUser();
        long[] ps = paginationHelper.clamp(page, size);
        Long total = delegationMapper.countMyDelegations(user.id());
        long t = total == null ? 0L : total;
        List<Map<String, Object>> items = delegationMapper.selectMyDelegations(user.id(), ps[1], (ps[0] - 1) * ps[1]);
        return new PageResponse<>(ps[0], ps[1], t, items);
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listAllDelegations(long page, long size, String status) {
        long[] ps = paginationHelper.clamp(page, size);
        Long total = delegationMapper.countAllDelegations(status);
        long t = total == null ? 0L : total;
        List<Map<String, Object>> items = delegationMapper.selectAllDelegations(status, ps[1], (ps[0] - 1) * ps[1]);
        return new PageResponse<>(ps[0], ps[1], t, items);
    }
}
