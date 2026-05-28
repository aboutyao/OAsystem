package com.company.oa.oa;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.oa.entity.oa.OaEditHistory;
import com.company.oa.oa.mapper.OaEditHistoryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EditHistoryService {

    private static final Logger log = LoggerFactory.getLogger(EditHistoryService.class);

    private final OaEditHistoryMapper editHistoryMapper;

    public EditHistoryService(OaEditHistoryMapper editHistoryMapper) {
        this.editHistoryMapper = editHistoryMapper;
    }

    /**
     * Save a snapshot of the entity state before an edit.
     *
     * @param entityType entity type identifier (e.g. OA_LEAVE, OA_EXPENSE)
     * @param entityId   entity primary key
     * @param snapshot   the full entity state as a Map (will be serialized to JSON by caller)
     * @param snapshotJson pre-serialized JSON string of the entity before edit
     * @param editedBy   user ID of the person performing the edit
     */
    @Transactional
    public void saveSnapshot(String entityType, Long entityId, String snapshotJson, Long editedBy) {
        try {
            OaEditHistory history = new OaEditHistory();
            history.setEntityType(entityType);
            history.setEntityId(entityId);
            history.setSnapshotJson(snapshotJson);
            history.setEditedBy(editedBy);
            history.setEditedAt(LocalDateTime.now());
            editHistoryMapper.insert(history);
        } catch (Exception e) {
            log.warn("Failed to save edit history for {} {}: {}", entityType, entityId, e.getMessage());
        }
    }

    /**
     * Return all edit history snapshots for a given entity, ordered from newest to oldest.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getHistory(String entityType, Long entityId) {
        LambdaQueryWrapper<OaEditHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OaEditHistory::getEntityType, entityType);
        wrapper.eq(OaEditHistory::getEntityId, entityId);
        wrapper.orderByDesc(OaEditHistory::getEditedAt);
        wrapper.orderByDesc(OaEditHistory::getId);

        List<OaEditHistory> records = editHistoryMapper.selectList(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();
        for (OaEditHistory record : records) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", record.getId());
            item.put("entityType", record.getEntityType());
            item.put("entityId", record.getEntityId());
            item.put("snapshotJson", record.getSnapshotJson());
            item.put("editedBy", record.getEditedBy());
            item.put("editedAt", record.getEditedAt() == null ? null : record.getEditedAt().toString());
            result.add(item);
        }
        return result;
    }
}
