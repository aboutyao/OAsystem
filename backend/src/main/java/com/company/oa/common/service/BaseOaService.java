package com.company.oa.common.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.oa.audit.AuditService;
import com.company.oa.auth.AuthService;
import com.company.oa.auth.AuthUser;
import com.company.oa.common.api.PageResponse;
import com.company.oa.common.entity.VersionedEntity;
import com.company.oa.common.error.BusinessException;
import com.company.oa.common.error.ErrorCode;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public abstract class BaseOaService<T extends VersionedEntity> {
    protected final BaseMapper<T> mapper;
    protected final AuthService authService;
    protected final AuditService auditService;
    protected final PageUtils pageUtils;
    protected final EntityMapper entityMapper;

    protected BaseOaService(BaseMapper<T> mapper, AuthService authService,
                            AuditService auditService, PageUtils pageUtils, EntityMapper entityMapper) {
        this.mapper = mapper;
        this.authService = authService;
        this.auditService = auditService;
        this.pageUtils = pageUtils;
        this.entityMapper = entityMapper;
    }

    protected AuthUser currentUser() {
        return authService.currentUser();
    }

    protected void assertOwner(T entity, Function<T, Long> createdByGetter) {
        AuthUser user = currentUser();
        if (!user.permissions().contains("*") && !user.id().equals(createdByGetter.apply(entity))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此记录");
        }
    }

    protected void assertViewAllowed(T entity, Function<T, Long> createdByGetter) {
        AuthUser user = currentUser();
        if (!user.permissions().contains("*") && !user.id().equals(createdByGetter.apply(entity))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看此记录");
        }
    }

    protected <ID extends Serializable> PageResponse<Map<String, Object>> listByApplicant(
            long page, long size, Long applicantId,
            Function<T, Long> createdByGetter,
            Class<ID> idType
    ) {
        AuthUser user = currentUser();
        long[] ps = pageUtils.clamp(page, size);
        LambdaQueryWrapper<T> qw = new LambdaQueryWrapper<>();
        if (applicantId != null) {
            if (!user.permissions().contains("*") && !user.id().equals(applicantId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看他人申请");
            }
            qw.eq(createdByGetter::apply, applicantId);
        } else {
            qw.eq(createdByGetter::apply, user.id());
        }
        long total = mapper.selectCount(qw);
        qw.orderByDesc(VersionedEntity::getId)
                .last("limit " + ps[1] + " offset " + ((ps[0] - 1) * ps[1]));
        List<T> entities = mapper.selectList(qw);
        List<Map<String, Object>> items = new ArrayList<>();
        for (T e : entities) {
            items.add(entityMapper.toMap(e));
        }
        return new PageResponse<>(ps[0], ps[1], total, items);
    }

    @Transactional
    protected void updateStatus(Serializable id, String status, BiConsumer<T, String> statusSetter) {
        try {
            T entity = mapper.selectById((Serializable) id);
            if (entity != null) {
                statusSetter.accept(entity, status);
                entity.setUpdatedAt(LocalDateTime.now());
                mapper.updateById(entity);
            }
        } catch (Exception ignored) {}
    }
}
