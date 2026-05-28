package com.company.oa.message;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.oa.audit.AuditService;
import com.company.oa.auth.AuthService;
import com.company.oa.auth.AuthUser;
import com.company.oa.common.api.PageResponse;
import com.company.oa.common.error.BusinessException;
import com.company.oa.common.error.ErrorCode;
import com.company.oa.common.service.PaginationHelper;
import com.company.oa.entity.msg.MsgMessage;
import com.company.oa.common.service.SequenceService;
import com.company.oa.message.mapper.MsgMessageMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MessageService {
    public static final String UNREAD = "UNREAD";
    public static final String READ = "READ";
    public static final String NORMAL = "NORMAL";
    public static final String ARCHIVED = "ARCHIVED";

    private final MsgMessageMapper messageMapper;
    private final PaginationHelper paginationHelper;
    private final AuthService authService;
    private final AuditService auditService;
    private final SequenceService sequenceService;
    private final NotificationSseController notificationSseController;
    private final com.company.oa.message.mapper.UserNotificationSettingsMapper settingsMapper;

    public MessageService(MsgMessageMapper messageMapper, PaginationHelper paginationHelper,
                          AuthService authService, AuditService auditService,
                          SequenceService sequenceService,
                          NotificationSseController notificationSseController,
                          com.company.oa.message.mapper.UserNotificationSettingsMapper settingsMapper) {
        this.messageMapper = messageMapper;
        this.paginationHelper = paginationHelper;
        this.authService = authService;
        this.auditService = auditService;
        this.sequenceService = sequenceService;
        this.notificationSseController = notificationSseController;
        this.settingsMapper = settingsMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> list(long page, long size, String readStatus, String archiveStatus) {
        AuthUser user = authService.currentUser();
        long[] ps = paginationHelper.clamp(page, size);

        LambdaQueryWrapper<MsgMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MsgMessage::getReceiverId, user.id());
        if (readStatus != null && !readStatus.isBlank()) {
            wrapper.eq(MsgMessage::getReadStatus, readStatus);
        }
        if (archiveStatus != null && !archiveStatus.isBlank()) {
            wrapper.eq(MsgMessage::getArchiveStatus, archiveStatus);
        }

        long total = messageMapper.selectCount(wrapper);

        wrapper.orderByDesc(MsgMessage::getCreatedAt);
        wrapper.orderByDesc(MsgMessage::getId);
        Page<MsgMessage> pageParam = new Page<>(ps[0], ps[1]);
        List<MsgMessage> records = messageMapper.selectPage(pageParam, wrapper).getRecords();

        List<Map<String, Object>> items = records.stream()
                .map(this::toMap)
                .collect(Collectors.toList());
        return new PageResponse<>(ps[0], ps[1], total, items);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> unreadCount() {
        AuthUser user = authService.currentUser();
        Long n = messageMapper.selectCount(
                new LambdaQueryWrapper<MsgMessage>()
                        .eq(MsgMessage::getReceiverId, user.id())
                        .eq(MsgMessage::getReadStatus, UNREAD)
                        .eq(MsgMessage::getArchiveStatus, NORMAL));
        return Map.of("count", n == null ? 0L : n);
    }

    @Transactional
    public Map<String, Object> markRead(long id) {
        AuthUser user = authService.currentUser();
        loadOwned(id, user.id());
        int rows = messageMapper.update(null,
                new LambdaUpdateWrapper<MsgMessage>()
                        .eq(MsgMessage::getId, id)
                        .eq(MsgMessage::getReceiverId, user.id())
                        .set(MsgMessage::getReadStatus, READ)
                        .set(MsgMessage::getReadAt, LocalDateTime.now()));
        if (rows != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "消息不存在");
        }
        return Map.of("id", id, "readStatus", READ);
    }

    @Transactional
    public Map<String, Object> batchRead(MessageDtos.BatchReadRequest req) {
        AuthUser user = authService.currentUser();
        if (req.ids().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择消息");
        }
        messageMapper.update(null,
                new LambdaUpdateWrapper<MsgMessage>()
                        .eq(MsgMessage::getReceiverId, user.id())
                        .in(MsgMessage::getId, req.ids())
                        .set(MsgMessage::getReadStatus, READ)
                        .set(MsgMessage::getReadAt, LocalDateTime.now()));
        return Map.of("updated", req.ids().size());
    }

    /**
     * Returns messages grouped by messageType with unread counts per group,
     * plus the paginated message list across all types.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getGroupedMessages(long userId, int page, int size) {
        // 1. Fetch all unread, non-archived messages for the user
        List<MsgMessage> allUnread = messageMapper.selectList(
                new LambdaQueryWrapper<MsgMessage>()
                        .eq(MsgMessage::getReceiverId, userId)
                        .eq(MsgMessage::getReadStatus, UNREAD)
                        .eq(MsgMessage::getArchiveStatus, NORMAL)
                        .orderByDesc(MsgMessage::getCreatedAt));

        // 2. Group by messageType and build summary
        Map<String, List<MsgMessage>> grouped = allUnread.stream()
                .collect(Collectors.groupingBy(
                        MsgMessage::getMessageType,
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<Map<String, Object>> groups = new ArrayList<>();
        for (Map.Entry<String, List<MsgMessage>> entry : grouped.entrySet()) {
            Map<String, Object> g = new LinkedHashMap<>();
            g.put("messageType", entry.getKey());
            g.put("unreadCount", entry.getValue().size());
            groups.add(g);
        }

        // 3. Paginated flat list of ALL messages (not just unread)
        long[] ps = paginationHelper.clamp(page, size);
        LambdaQueryWrapper<MsgMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MsgMessage::getReceiverId, userId);
        long total = messageMapper.selectCount(wrapper);
        wrapper.orderByDesc(MsgMessage::getCreatedAt);
        wrapper.orderByDesc(MsgMessage::getId);
        Page<MsgMessage> pageParam = new Page<>(ps[0], ps[1]);
        List<Map<String, Object>> items = messageMapper.selectPage(pageParam, wrapper)
                .getRecords().stream()
                .map(this::toMap)
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("groups", groups);
        result.put("totalUnread", allUnread.size());
        result.put("page", ps[0]);
        result.put("size", ps[1]);
        result.put("total", total);
        result.put("items", items);
        return result;
    }

    /**
     * Marks all unread, non-archived messages for the given user as read.
     */
    @Transactional
    public Map<String, Object> markAllAsRead(long userId) {
        int rows = messageMapper.update(null,
                new LambdaUpdateWrapper<MsgMessage>()
                        .eq(MsgMessage::getReceiverId, userId)
                        .eq(MsgMessage::getReadStatus, UNREAD)
                        .eq(MsgMessage::getArchiveStatus, NORMAL)
                        .set(MsgMessage::getReadStatus, READ)
                        .set(MsgMessage::getReadAt, LocalDateTime.now()));
        return Map.of("updated", rows);
    }

    @Transactional
    public Map<String, Object> archive(long id) {
        AuthUser user = authService.currentUser();
        loadOwned(id, user.id());
        int rows = messageMapper.update(null,
                new LambdaUpdateWrapper<MsgMessage>()
                        .eq(MsgMessage::getId, id)
                        .eq(MsgMessage::getReceiverId, user.id())
                        .set(MsgMessage::getArchiveStatus, ARCHIVED));
        if (rows != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "消息不存在");
        }
        auditService.safeRecordOperation(user.id(), "MSG_ARCHIVE", "MESSAGE", id, AuditService.SUCCESS, null);
        return Map.of("id", id, "archiveStatus", ARCHIVED);
    }

    @Transactional
    public void delete(long id) {
        AuthUser user = authService.currentUser();
        loadOwned(id, user.id());
        int rows = messageMapper.delete(
                new LambdaQueryWrapper<MsgMessage>()
                        .eq(MsgMessage::getId, id)
                        .eq(MsgMessage::getReceiverId, user.id()));
        if (rows != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "消息不存在");
        }
    }

    @Transactional(readOnly = true)
    public long countUnreadForUser(long userId) {
        Long n = messageMapper.selectCount(
                new LambdaQueryWrapper<MsgMessage>()
                        .eq(MsgMessage::getReceiverId, userId)
                        .eq(MsgMessage::getReadStatus, UNREAD)
                        .eq(MsgMessage::getArchiveStatus, NORMAL));
        return n == null ? 0L : n;
    }

    @Transactional
    public void send(long receiverId, String messageType, String title, String content,
                     String businessType, Long businessId, Long wfInstanceId) {
        MsgMessage entity = new MsgMessage();
        entity.setId(sequenceService.nextId("msg_message"));
        entity.setReceiverId(receiverId);
        entity.setMessageType(messageType);
        entity.setTitle(title);
        entity.setContent(content);
        entity.setBusinessType(businessType);
        entity.setBusinessId(businessId);
        entity.setWfInstanceId(wfInstanceId);
        entity.setReadStatus(UNREAD);
        entity.setArchiveStatus(NORMAL);
        entity.setReadAt(null);
        messageMapper.insert(entity);

        // Push real-time notification via SSE
        notificationSseController.notifyUser(receiverId, messageType, title, content);
    }

    private void loadOwned(long id, long receiverId) {
        Long count = messageMapper.selectCount(
                new LambdaQueryWrapper<MsgMessage>()
                        .eq(MsgMessage::getId, id)
                        .eq(MsgMessage::getReceiverId, receiverId));
        if (count == null || count == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "消息不存在");
        }
    }

    private Map<String, Object> toMap(MsgMessage m) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", m.getId());
        map.put("receiverId", m.getReceiverId());
        map.put("messageType", m.getMessageType());
        map.put("title", m.getTitle());
        map.put("content", m.getContent());
        map.put("businessType", m.getBusinessType());
        map.put("businessId", m.getBusinessId());
        map.put("wfInstanceId", m.getWfInstanceId());
        map.put("readStatus", m.getReadStatus());
        map.put("archiveStatus", m.getArchiveStatus());
        map.put("createdAt", m.getCreatedAt() == null ? null : m.getCreatedAt().toString());
        map.put("readAt", m.getReadAt() == null ? null : m.getReadAt().toString());
        return map;
    }

    // ─── Notification Settings ──────────────────────────────────────────

    public Map<String, Object> getNotificationSettings() {
        AuthUser user = authService.currentUser();
        var settings = settingsMapper.selectOne(
                new LambdaQueryWrapper<com.company.oa.entity.message.UserNotificationSettings>()
                        .eq(com.company.oa.entity.message.UserNotificationSettings::getUserId, user.id())
        );
        if (settings == null) {
            Map<String, Object> defaults = new LinkedHashMap<>();
            defaults.put("enableEmail", true);
            defaults.put("enableSse", true);
            defaults.put("enableDnd", false);
            defaults.put("dndStart", null);
            defaults.put("dndEnd", null);
            return defaults;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enableEmail", settings.getEnableEmail());
        result.put("enableSse", settings.getEnableSse());
        result.put("enableDnd", settings.getEnableDnd());
        result.put("dndStart", settings.getDndStart());
        result.put("dndEnd", settings.getDndEnd());
        return result;
    }

    @Transactional
    public Map<String, Object> updateNotificationSettings(Map<String, Object> body) {
        AuthUser user = authService.currentUser();
        var existing = settingsMapper.selectOne(
                new LambdaQueryWrapper<com.company.oa.entity.message.UserNotificationSettings>()
                        .eq(com.company.oa.entity.message.UserNotificationSettings::getUserId, user.id())
        );

        if (existing == null) {
            existing = new com.company.oa.entity.message.UserNotificationSettings();
            existing.setId(sequenceService.nextId("user_notification_settings"));
            existing.setUserId(user.id());
            existing.setEnableEmail(true);
            existing.setEnableSse(true);
            existing.setEnableDnd(false);
            settingsMapper.insert(existing);
        }

        if (body.containsKey("enableEmail")) {
            existing.setEnableEmail(Boolean.TRUE.equals(body.get("enableEmail")));
        }
        if (body.containsKey("enableSse")) {
            existing.setEnableSse(Boolean.TRUE.equals(body.get("enableSse")));
        }
        if (body.containsKey("enableDnd")) {
            existing.setEnableDnd(Boolean.TRUE.equals(body.get("enableDnd")));
        }
        if (body.containsKey("dndStart")) {
            existing.setDndStart(body.get("dndStart") != null ? String.valueOf(body.get("dndStart")) : null);
        }
        if (body.containsKey("dndEnd")) {
            existing.setDndEnd(body.get("dndEnd") != null ? String.valueOf(body.get("dndEnd")) : null);
        }
        existing.setUpdatedAt(LocalDateTime.now());
        settingsMapper.updateById(existing);

        return getNotificationSettings();
    }

}
