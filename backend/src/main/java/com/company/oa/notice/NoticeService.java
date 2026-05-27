package com.company.oa.notice;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.oa.audit.AuditService;
import com.company.oa.auth.AuthService;
import com.company.oa.auth.AuthUser;
import com.company.oa.common.api.PageResponse;
import com.company.oa.common.error.BusinessException;
import com.company.oa.common.error.ErrorCode;
import com.company.oa.entity.notice.OaNotice;
import com.company.oa.entity.notice.OaNoticeRead;
import com.company.oa.entity.system.SysConfig;
import com.company.oa.common.service.SequenceService;
import com.company.oa.message.MessageService;
import com.company.oa.notice.mapper.OaNoticeMapper;
import com.company.oa.notice.mapper.OaNoticeReadMapper;
import com.company.oa.org.mapper.UserMapper;
import com.company.oa.system.mapper.SysConfigMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class NoticeService {
    private static final String DRAFT = "DRAFT";
    private static final String PUBLISHED = "PUBLISHED";
    private static final String WITHDRAWN = "WITHDRAWN";
    private static final String ARCHIVED = "ARCHIVED";

    private final OaNoticeMapper noticeMapper;
    private final OaNoticeReadMapper noticeReadMapper;
    private final UserMapper userMapper;
    private final SysConfigMapper sysConfigMapper;
    private final AuthService authService;
    private final AuditService auditService;
    private final SequenceService sequenceService;
    private final MessageService messageService;

    public NoticeService(OaNoticeMapper noticeMapper, OaNoticeReadMapper noticeReadMapper,
                         UserMapper userMapper, SysConfigMapper sysConfigMapper,
                         AuthService authService, AuditService auditService,
                         SequenceService sequenceService,
                         MessageService messageService) {
        this.noticeMapper = noticeMapper;
        this.noticeReadMapper = noticeReadMapper;
        this.userMapper = userMapper;
        this.sysConfigMapper = sysConfigMapper;
        this.authService = authService;
        this.auditService = auditService;
        this.sequenceService = sequenceService;
        this.messageService = messageService;
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> list(long page, long size, Boolean mine, String status, String category) {
        AuthUser user = authService.currentUser();
        long[] ps = clampPage(page, size);
        boolean superUser = user.permissions().contains("*");

        LambdaQueryWrapper<OaNotice> wrapper = new LambdaQueryWrapper<>();
        if (Boolean.TRUE.equals(mine)) {
            wrapper.eq(OaNotice::getCreatedBy, user.id());
            if (status != null && !status.isBlank()) {
                wrapper.eq(OaNotice::getStatus, status);
            }
        } else if (superUser) {
            if (status != null && !status.isBlank()) {
                wrapper.eq(OaNotice::getStatus, status);
            }
        } else {
            wrapper.eq(OaNotice::getStatus, PUBLISHED);
            wrapper.isNotNull(OaNotice::getPublishAt);
            wrapper.le(OaNotice::getPublishAt, LocalDateTime.now());
        }
        if (category != null && !category.isBlank()) {
            wrapper.eq(OaNotice::getCategory, category);
        }

        long total = noticeMapper.selectCount(wrapper);

        wrapper.orderByDesc(OaNotice::getTopFlag);
        wrapper.orderByDesc(OaNotice::getPublishAt);
        wrapper.orderByDesc(OaNotice::getId);
        Page<OaNotice> pageParam = new Page<>(ps[0], ps[1]);
        List<OaNotice> records = noticeMapper.selectPage(pageParam, wrapper).getRecords();

        List<Map<String, Object>> items = records.stream()
                .map(this::toListMap)
                .collect(Collectors.toList());
        return new PageResponse<>(ps[0], ps[1], total, items);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detail(long id) {
        Map<String, Object> row = loadNotice(id);
        assertReadable(row);
        return row;
    }

    @Transactional
    public Map<String, Object> create(NoticeDtos.NoticeCreateRequest req) {
        AuthUser user = authService.currentUser();
        String cat = req.category() == null || req.category().isBlank() ? "GENERAL" : req.category();
        String scope = req.publishScopeType() == null || req.publishScopeType().isBlank() ? "ALL" : req.publishScopeType();
        int top = Math.min(1, Math.max(0, req.topFlag()));

        OaNotice entity = new OaNotice();
        entity.setId(sequenceService.nextId("oa_notice"));
        entity.setTitle(req.title());
        entity.setContent(req.content());
        entity.setCategory(cat);
        entity.setPublishScopeType(scope);
        entity.setTopFlag(top);
        entity.setStatus(DRAFT);
        entity.setCreatedBy(user.id());
        noticeMapper.insert(entity);

        return detail(entity.getId());
    }

    @Transactional
    public Map<String, Object> update(long id, NoticeDtos.NoticeUpdateRequest req) {
        Map<String, Object> row = loadNotice(id);
        assertOwner(row);
        if (!DRAFT.equals(String.valueOf(row.get("status")))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅草稿可编辑");
        }
        String cat = req.category() == null || req.category().isBlank() ? "GENERAL" : req.category();
        String scope = req.publishScopeType() == null || req.publishScopeType().isBlank() ? "ALL" : req.publishScopeType();
        int top = Math.min(1, Math.max(0, req.topFlag()));

        noticeMapper.update(null,
                new LambdaUpdateWrapper<OaNotice>()
                        .eq(OaNotice::getId, id)
                        .set(OaNotice::getTitle, req.title())
                        .set(OaNotice::getContent, req.content())
                        .set(OaNotice::getCategory, cat)
                        .set(OaNotice::getPublishScopeType, scope)
                        .set(OaNotice::getTopFlag, top)
                        .set(OaNotice::getUpdatedAt, LocalDateTime.now())
                        .setSql("version = version + 1"));
        return detail(id);
    }

    @Transactional
    public Map<String, Object> publish(long id) {
        Map<String, Object> row = loadNotice(id);
        assertOwner(row);
        if (!DRAFT.equals(String.valueOf(row.get("status")))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅草稿可发布");
        }
        LocalDateTime now = LocalDateTime.now();
        noticeMapper.update(null,
                new LambdaUpdateWrapper<OaNotice>()
                        .eq(OaNotice::getId, id)
                        .set(OaNotice::getStatus, PUBLISHED)
                        .set(OaNotice::getPublishAt, now)
                        .set(OaNotice::getWithdrawAt, null)
                        .set(OaNotice::getUpdatedAt, now)
                        .setSql("version = version + 1"));
        auditService.safeRecordOperation(authService.currentUser().id(),
                "NOTICE_PUBLISH", "NOTICE", id, AuditService.SUCCESS, null);

        // 发布后自动给所有激活用户发消息中心通知
        notifyAllUsers(String.valueOf(row.get("title")), id);

        return detail(id);
    }

    private void notifyAllUsers(String title, long noticeId) {
        // 查询所有启用的非离职用户
        List<Long> userIds = userMapper.selectList(
                new LambdaQueryWrapper<com.company.oa.entity.org.User>()
                        .eq(com.company.oa.entity.org.User::getAccountStatus, "ENABLED")
                        .ne(com.company.oa.entity.org.User::getEmployeeStatus, "RESIGNED")
                        .select(com.company.oa.entity.org.User::getId)
        ).stream().map(com.company.oa.entity.org.User::getId).toList();

        String msg = "新公告: " + title;
        for (Long uid : userIds) {
            messageService.send(uid, "NOTICE", msg, msg, "NOTICE", noticeId, null);
        }
    }

    @Transactional
    public Map<String, Object> withdraw(long id) {
        Map<String, Object> row = loadNotice(id);
        assertOwner(row);
        if (!PUBLISHED.equals(String.valueOf(row.get("status")))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅已发布可撤回");
        }
        LocalDateTime now = LocalDateTime.now();
        noticeMapper.update(null,
                new LambdaUpdateWrapper<OaNotice>()
                        .eq(OaNotice::getId, id)
                        .set(OaNotice::getStatus, WITHDRAWN)
                        .set(OaNotice::getWithdrawAt, now)
                        .set(OaNotice::getUpdatedAt, now)
                        .setSql("version = version + 1"));
        auditService.safeRecordOperation(authService.currentUser().id(),
                "NOTICE_WITHDRAW", "NOTICE", id, AuditService.SUCCESS, null);
        return detail(id);
    }

    @Transactional
    public Map<String, Object> markRead(long id) {
        Map<String, Object> row = loadNotice(id);
        assertReadable(row);
        AuthUser user = authService.currentUser();
        LocalDateTime now = LocalDateTime.now();

        OaNoticeRead existing = noticeReadMapper.selectOne(
                new LambdaQueryWrapper<OaNoticeRead>()
                        .eq(OaNoticeRead::getNoticeId, id)
                        .eq(OaNoticeRead::getUserId, user.id()));
        if (existing == null) {
            OaNoticeRead readRecord = new OaNoticeRead();
            readRecord.setId(sequenceService.nextId("oa_notice_read"));
            readRecord.setNoticeId(id);
            readRecord.setUserId(user.id());
            readRecord.setReadAt(now);
            readRecord.setConfirmed(0);
            noticeReadMapper.insert(readRecord);
        } else {
            noticeReadMapper.update(null,
                    new LambdaUpdateWrapper<OaNoticeRead>()
                            .eq(OaNoticeRead::getId, existing.getId())
                            .set(OaNoticeRead::getReadAt, now));
        }
        return Map.of("noticeId", id, "read", true);
    }

    @Transactional
    public Map<String, Object> confirmRead(long id) {
        markRead(id);
        AuthUser user = authService.currentUser();
        noticeReadMapper.update(null,
                new LambdaUpdateWrapper<OaNoticeRead>()
                        .eq(OaNoticeRead::getNoticeId, id)
                        .eq(OaNoticeRead::getUserId, user.id())
                        .set(OaNoticeRead::getConfirmed, 1));
        return Map.of("noticeId", id, "confirmed", true);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> readStats(long id) {
        Map<String, Object> row = loadNotice(id);
        assertReadable(row);

        long rc = noticeReadMapper.selectCount(
                new LambdaQueryWrapper<OaNoticeRead>().eq(OaNoticeRead::getNoticeId, id));
        long confirmedCount = noticeReadMapper.selectCount(
                new LambdaQueryWrapper<OaNoticeRead>()
                        .eq(OaNoticeRead::getNoticeId, id)
                        .eq(OaNoticeRead::getConfirmed, 1));
        long totalUsers = userMapper.selectCount(new LambdaQueryWrapper<>());
        long unread = Math.max(0, totalUsers - rc);
        return Map.of(
                "noticeId", id,
                "readCount", rc,
                "confirmedCount", confirmedCount,
                "unreadCount", unread
        );
    }

    private Map<String, Object> loadNotice(long id) {
        OaNotice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "公告不存在");
        }
        return toDetailMap(notice);
    }

    private Map<String, Object> toListMap(OaNotice n) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", n.getId());
        map.put("title", n.getTitle());
        map.put("category", n.getCategory());
        map.put("topFlag", n.getTopFlag());
        map.put("publishAt", n.getPublishAt() == null ? null : n.getPublishAt().toString());
        map.put("status", n.getStatus());
        map.put("createdBy", n.getCreatedBy());
        map.put("createdAt", n.getCreatedAt() == null ? null : n.getCreatedAt().toString());
        map.put("updatedAt", n.getUpdatedAt() == null ? null : n.getUpdatedAt().toString());
        return map;
    }

    private Map<String, Object> toDetailMap(OaNotice n) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", n.getId());
        map.put("title", n.getTitle());
        map.put("content", n.getContent());
        map.put("category", n.getCategory());
        map.put("publishScopeType", n.getPublishScopeType());
        map.put("topFlag", n.getTopFlag());
        map.put("topUntil", n.getTopUntil() == null ? null : n.getTopUntil().toString());
        map.put("publishAt", n.getPublishAt() == null ? null : n.getPublishAt().toString());
        map.put("withdrawAt", n.getWithdrawAt() == null ? null : n.getWithdrawAt().toString());
        map.put("status", n.getStatus());
        map.put("createdBy", n.getCreatedBy());
        map.put("createdAt", n.getCreatedAt() == null ? null : n.getCreatedAt().toString());
        map.put("updatedAt", n.getUpdatedAt() == null ? null : n.getUpdatedAt().toString());
        map.put("version", n.getVersion());
        return map;
    }

    private void assertOwner(Map<String, Object> row) {
        AuthUser user = authService.currentUser();
        long owner = ((Number) row.get("createdBy")).longValue();
        if (!user.permissions().contains("*") && !user.id().equals(owner)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此公告");
        }
    }

    private void assertReadable(Map<String, Object> row) {
        AuthUser user = authService.currentUser();
        String st = String.valueOf(row.get("status"));
        if (PUBLISHED.equals(st)) {
            return;
        }
        if (user.permissions().contains("*")) {
            return;
        }
        long owner = ((Number) row.get("createdBy")).longValue();
        if (user.id() == owner && (DRAFT.equals(st) || WITHDRAWN.equals(st) || ARCHIVED.equals(st))) {
            return;
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看此公告");
    }

    private long[] clampPage(long page, long size) {
        int def = intConfig("paging.defaultSize", 20);
        int max = intConfig("paging.maxSize", 100);
        long p = page < 1 ? 1 : page;
        long s = size < 1 ? def : size;
        if (s > max) {
            s = max;
        }
        return new long[]{p, s};
    }

    private int intConfig(String key, int defaultValue) {
        SysConfig config = sysConfigMapper.selectOne(
                new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, key));
        if (config == null || config.getConfigValue() == null) {
            return defaultValue;
        }
        return Integer.parseInt(config.getConfigValue());
    }
}
