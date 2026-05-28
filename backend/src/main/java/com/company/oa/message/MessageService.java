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

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

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

        // Check DND settings — skip push notifications during quiet hours
        if (isDndActive(receiverId)) {
            return;
        }

        // Push real-time notification via SSE
        notificationSseController.notifyUser(receiverId, messageType, title, content);
    }

    /**
     * Sends a contextual notification with urgency scoring and actionable metadata.
     *
     * <p>The {@code context} map may contain any of the following optional keys:
     * <ul>
     *   <li>{@code slaDeadline} (String or LocalDateTime) – SLA deadline for the workflow task</li>
     *   <li>{@code amount} (Number) – monetary amount or impact value</li>
     *   <li>{@code requesterId} (Long) – ID of the original requester</li>
     *   <li>{@code wfInstanceId} (Long) – workflow instance ID</li>
     *   <li>{@code businessType} (String) – business type code</li>
     * </ul>
     *
     * <p>The method computes and attaches the following context fields:
     * <ul>
     *   <li>{@code urgency} – URGENT / HIGH / MEDIUM / LOW</li>
     *   <li>{@code slaRemaining} – hours until SLA breach (null if no deadline)</li>
     *   <li>{@code suggestedAction} – "立即处理" / "今日处理" / "本周处理"</li>
     *   <li>{@code relatedCount} – number of similar pending items for the same receiver</li>
     * </ul>
     */
    @Transactional
    public void sendContextualNotification(long receiverId, String messageType, String title, String content,
                                           String businessType, Long businessId, Long wfInstanceId,
                                           Map<String, Object> context) {
        // 1. Compute urgency from context
        String urgency = computeUrgency(context);
        Double slaRemaining = computeSlaRemaining(context);
        String suggestedAction = resolveSuggestedAction(urgency);
        int relatedCount = countRelatedPendingItems(receiverId, messageType);

        // 2. Build enriched context JSON
        Map<String, Object> enrichedContext = new LinkedHashMap<>(context != null ? context : Map.of());
        enrichedContext.put("urgency", urgency);
        enrichedContext.put("slaRemaining", slaRemaining);
        enrichedContext.put("suggestedAction", suggestedAction);
        enrichedContext.put("relatedCount", relatedCount);

        String contextJson;
        try {
            contextJson = new ObjectMapper().writeValueAsString(enrichedContext);
        } catch (Exception e) {
            contextJson = "{}";
        }

        // 3. Persist message with context
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
        entity.setContextJson(contextJson);
        messageMapper.insert(entity);

        // 4. Check DND settings — skip push notifications during quiet hours
        if (isDndActive(receiverId)) {
            return;
        }

        // 5. Push real-time notification via SSE
        notificationSseController.notifyUser(receiverId, messageType, title, content);
    }

    // ─── Urgency Scoring Helpers ───────────────────────────────────────

    /**
     * Calculates urgency level based on SLA deadline proximity, amount impact, and requester history.
     */
    private String computeUrgency(Map<String, Object> context) {
        if (context == null) return "LOW";

        int score = 0;

        // SLA deadline proximity
        Double slaRemaining = computeSlaRemaining(context);
        if (slaRemaining != null) {
            if (slaRemaining < 4) score += 40;       // URGENT tier
            else if (slaRemaining < 12) score += 25;  // HIGH tier
            else if (slaRemaining < 24) score += 10;  // MEDIUM tier
        }

        // Amount / impact
        Object amountObj = context.get("amount");
        if (amountObj instanceof Number amount) {
            double val = amount.doubleValue();
            if (val >= 50000) score += 30;
            else if (val >= 10000) score += 20;
            else if (val >= 5000) score += 10;
        }

        // First-time requester heuristic: if requesterId is present but no history flag is set,
        // we check if the requester has sent fewer than 2 prior messages (lightweight signal)
        Object requesterIdObj = context.get("requesterId");
        if (requesterIdObj instanceof Number requesterId) {
            Long priorCount = messageMapper.selectCount(
                    new LambdaQueryWrapper<MsgMessage>()
                            .eq(MsgMessage::getReceiverId, receiverId(context))
                            .eq(MsgMessage::getBusinessType, context.getOrDefault("businessType", ""))
                            .ne(MsgMessage::getReceiverId, requesterId.longValue()));
            // If very few messages exist for this business type, treat as first-time signal
            if (priorCount != null && priorCount <= 1) {
                score += 15;
            }
        }

        if (score >= 60) return "URGENT";
        if (score >= 35) return "HIGH";
        if (score >= 15) return "MEDIUM";
        return "LOW";
    }

    /**
     * Returns hours remaining until SLA breach, or null if no deadline is set.
     */
    private Double computeSlaRemaining(Map<String, Object> context) {
        if (context == null) return null;
        Object slaObj = context.get("slaDeadline");
        if (slaObj == null) return null;

        LocalDateTime slaDeadline = null;
        if (slaObj instanceof LocalDateTime ldt) {
            slaDeadline = ldt;
        } else {
            try {
                slaDeadline = LocalDateTime.parse(String.valueOf(slaObj));
            } catch (Exception ignore) {
                return null;
            }
        }

        long hours = ChronoUnit.HOURS.between(LocalDateTime.now(), slaDeadline);
        return (double) hours;
    }

    /**
     * Resolves the suggested action text based on urgency level.
     */
    private String resolveSuggestedAction(String urgency) {
        return switch (urgency) {
            case "URGENT" -> "立即处理";
            case "HIGH" -> "今日处理";
            default -> "本周处理";
        };
    }

    /**
     * Counts how many unread, non-archived messages of the same type exist for the receiver.
     */
    private int countRelatedPendingItems(long receiverId, String messageType) {
        Long count = messageMapper.selectCount(
                new LambdaQueryWrapper<MsgMessage>()
                        .eq(MsgMessage::getReceiverId, receiverId)
                        .eq(MsgMessage::getMessageType, messageType)
                        .eq(MsgMessage::getReadStatus, UNREAD)
                        .eq(MsgMessage::getArchiveStatus, NORMAL));
        return count == null ? 0 : count.intValue();
    }

    private long receiverId(Map<String, Object> context) {
        return context.get("requesterId") instanceof Number n ? n.longValue() : 0L;
    }

    /**
     * Checks whether the given user has DND (Do Not Disturb) enabled and
     * the current time falls within the configured quiet-time window.
     * Handles cross-midnight ranges (e.g. 22:00 – 07:00).
     */
    private boolean isDndActive(long userId) {
        var settings = settingsMapper.selectOne(
                new LambdaQueryWrapper<com.company.oa.entity.message.UserNotificationSettings>()
                        .eq(com.company.oa.entity.message.UserNotificationSettings::getUserId, userId)
        );
        if (settings == null || !Boolean.TRUE.equals(settings.getEnableDnd())) {
            return false;
        }

        String startStr = settings.getDndStart();
        String endStr = settings.getDndEnd();
        if (startStr == null || endStr == null || startStr.isBlank() || endStr.isBlank()) {
            return false;
        }

        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
            LocalTime start = LocalTime.parse(startStr.trim(), fmt);
            LocalTime end = LocalTime.parse(endStr.trim(), fmt);
            LocalTime now = LocalTime.now();

            if (start.isBefore(end) || start.equals(end)) {
                // Normal range: e.g. 09:00 – 18:00
                return !now.isBefore(start) && !now.isAfter(end);
            } else {
                // Cross-midnight range: e.g. 22:00 – 07:00
                return !now.isBefore(start) || !now.isAfter(end);
            }
        } catch (Exception e) {
            // Malformed time strings — treat DND as inactive
            return false;
        }
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
        map.put("contextJson", m.getContextJson());
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
