package com.company.oa.asset;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.oa.asset.mapper.AssetInfoMapper;
import com.company.oa.asset.mapper.AssetRecordMapper;
import com.company.oa.audit.AuditService;
import com.company.oa.auth.AuthService;
import com.company.oa.auth.AuthUser;
import com.company.oa.common.api.PageResponse;
import com.company.oa.common.error.BusinessException;
import com.company.oa.common.error.ErrorCode;
import com.company.oa.common.service.OaUtils;
import com.company.oa.common.service.PaginationHelper;
import com.company.oa.common.service.SequenceService;
import com.company.oa.entity.asset.AssetInfo;
import com.company.oa.entity.asset.AssetRecord;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class AssetService {
    private static final String IDLE = "IDLE";
    private static final String IN_USE = "IN_USE";
    private static final String REPAIRING = "REPAIRING";
    private static final String SCRAPPED = "SCRAPPED";

    private static final String RECEIVE = "RECEIVE";
    private static final String RETURN = "RETURN";
    private static final String REPAIR = "REPAIR";
    private static final String SCRAP = "SCRAP";

    private final AssetInfoMapper assetInfoMapper;
    private final AssetRecordMapper assetRecordMapper;
    private final PaginationHelper paginationHelper;
    private final AuthService authService;
    private final AuditService auditService;
    private final SequenceService sequenceService;

    public AssetService(AssetInfoMapper assetInfoMapper, AssetRecordMapper assetRecordMapper,
                        PaginationHelper paginationHelper, AuthService authService,
                        AuditService auditService, SequenceService sequenceService) {
        this.assetInfoMapper = assetInfoMapper;
        this.assetRecordMapper = assetRecordMapper;
        this.paginationHelper = paginationHelper;
        this.authService = authService;
        this.auditService = auditService;
        this.sequenceService = sequenceService;
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> list(
            long page,
            long size,
            String assetCategory,
            String status,
            Long responsibleUserId,
            String keyword
    ) {
        AuthUser user = authService.currentUser();
        long[] ps = paginationHelper.clamp(page, size);
        // COUNT 查询：使用 eq(deleted) 在无别名的 BaseMapper 中工作
        LambdaQueryWrapper<AssetInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetInfo::getDeleted, 0);
        addConditions(wrapper, assetCategory, status, responsibleUserId, keyword, user);
        long total = Objects.requireNonNullElse(assetInfoMapper.selectCount(wrapper), 0L);
        // LIST 查询：使用 apply("a.deleted = 0") 配合表别名才能消歧义
        LambdaQueryWrapper<AssetInfo> listWrapper = new LambdaQueryWrapper<>();
        listWrapper.apply("a.deleted = 0");
        addConditions(listWrapper, assetCategory, status, responsibleUserId, keyword, user);
        listWrapper.orderByDesc(AssetInfo::getId);
        listWrapper.last("limit " + ps[1] + " offset " + (ps[0] - 1) * ps[1]);
        List<Map<String, Object>> items = assetInfoMapper.selectAssetList(listWrapper);
        return new PageResponse<>(ps[0], ps[1], total, items);
    }

    private void addConditions(LambdaQueryWrapper<AssetInfo> wrapper,
                                 String assetCategory, String status,
                                 Long responsibleUserId, String keyword,
                                 AuthUser user) {
        if (assetCategory != null && !assetCategory.isBlank()) {
            wrapper.eq(AssetInfo::getAssetCategory, assetCategory);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(AssetInfo::getStatus, status);
        }
        if (responsibleUserId != null) {
            wrapper.eq(AssetInfo::getResponsibleUserId, responsibleUserId);
        }
        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + keyword.trim() + "%";
            wrapper.and(w -> w.like(AssetInfo::getAssetNo, like).or().like(AssetInfo::getAssetName, like));
        }
        if (!user.permissions().contains("*")) {
            wrapper.eq(AssetInfo::getResponsibleUserId, user.id());
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detail(long id) {
        return loadAsset(id);
    }

    @Transactional
    public Map<String, Object> create(AssetDtos.AssetCreateRequest req) {
        long id = sequenceService.nextId("asset_info");
        try {
            AssetInfo entity = new AssetInfo();
            entity.setId(id);
            entity.setAssetNo(req.assetNo());
            entity.setAssetName(req.assetName());
            entity.setAssetCategory(req.assetCategory());
            entity.setModel(req.model());
            entity.setPurchaseDate(req.purchaseDate());
            entity.setPurchaseAmount(req.purchaseAmount());
            entity.setResponsibleUserId(req.responsibleUserId());
            entity.setDeptId(req.deptId());
            entity.setStatus(req.responsibleUserId() == null ? IDLE : IN_USE);
            entity.setRemark(req.remark());
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            assetInfoMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.CONFLICT, "资产编号已存在");
        }
        if (req.responsibleUserId() != null) {
            insertRecord(id, RECEIVE, null, req.responsibleUserId(), "建档领用");
        }
        return loadAsset(id);
    }

    @Transactional
    public Map<String, Object> update(long id, AssetDtos.AssetUpdateRequest req) {
        loadAsset(id);
        AssetInfo entity = new AssetInfo();
        entity.setAssetName(req.assetName());
        entity.setAssetCategory(req.assetCategory());
        entity.setModel(req.model());
        entity.setPurchaseDate(req.purchaseDate());
        entity.setPurchaseAmount(req.purchaseAmount());
        entity.setDeptId(req.deptId());
        entity.setRemark(req.remark());
        entity.setUpdatedAt(LocalDateTime.now());
        assetInfoMapper.update(entity, new LambdaQueryWrapper<AssetInfo>()
                .eq(AssetInfo::getId, id)
                .eq(AssetInfo::getDeleted, 0));
        return loadAsset(id);
    }

    @Transactional
    public Map<String, Object> receive(long id, AssetDtos.AssetActionRequest req) {
        AuthUser user = authService.currentUser();
        Map<String, Object> asset = loadAsset(id);
        assertStatus(asset, Set.of(IDLE), "仅闲置资产可领用");
        long target = req != null && req.targetUserId() != null ? req.targetUserId() : user.id();
        AssetInfo entity = new AssetInfo();
        entity.setStatus(IN_USE);
        entity.setResponsibleUserId(target);
        entity.setUpdatedAt(LocalDateTime.now());
        assetInfoMapper.update(entity, new LambdaQueryWrapper<AssetInfo>().eq(AssetInfo::getId, id));
        insertRecord(id, RECEIVE, null, target, req != null ? req.reason() : null);
        auditService.safeRecordOperation(user.id(), "ASSET_RECEIVE", "ASSET", id, AuditService.SUCCESS, null);
        return loadAsset(id);
    }

    @Transactional
    public Map<String, Object> returnAsset(long id, AssetDtos.AssetReasonRequest req) {
        Map<String, Object> asset = loadAsset(id);
        assertStatus(asset, Set.of(IN_USE, REPAIRING), "仅在用/维修资产可归还");
        Long from = OaUtils.toLong(asset.get("responsibleUserId"));
        AssetInfo entity = new AssetInfo();
        entity.setStatus(IDLE);
        entity.setResponsibleUserId(null);
        entity.setUpdatedAt(LocalDateTime.now());
        assetInfoMapper.update(entity, new LambdaQueryWrapper<AssetInfo>().eq(AssetInfo::getId, id));
        insertRecord(id, RETURN, from, null, req != null ? req.reason() : null);
        auditService.safeRecordOperation(authService.currentUser().id(),
                "ASSET_RETURN", "ASSET", id, AuditService.SUCCESS, null);
        return loadAsset(id);
    }

    @Transactional
    public Map<String, Object> repair(long id, AssetDtos.AssetReasonRequest req) {
        Map<String, Object> asset = loadAsset(id);
        assertStatus(asset, Set.of(IDLE, IN_USE), "仅闲置/在用资产可送修");
        Long current = OaUtils.toLong(asset.get("responsibleUserId"));
        AssetInfo entity = new AssetInfo();
        entity.setStatus(REPAIRING);
        entity.setUpdatedAt(LocalDateTime.now());
        assetInfoMapper.update(entity, new LambdaQueryWrapper<AssetInfo>().eq(AssetInfo::getId, id));
        insertRecord(id, REPAIR, current, current, req != null ? req.reason() : null);
        return loadAsset(id);
    }

    @Transactional
    public Map<String, Object> scrap(long id, AssetDtos.AssetReasonRequest req) {
        Map<String, Object> asset = loadAsset(id);
        if (SCRAPPED.equals(String.valueOf(asset.get("status")))) {
            throw new BusinessException(ErrorCode.CONFLICT, "资产已报废");
        }
        Long current = OaUtils.toLong(asset.get("responsibleUserId"));
        AssetInfo entity = new AssetInfo();
        entity.setStatus(SCRAPPED);
        entity.setResponsibleUserId(null);
        entity.setUpdatedAt(LocalDateTime.now());
        assetInfoMapper.update(entity, new LambdaQueryWrapper<AssetInfo>().eq(AssetInfo::getId, id));
        insertRecord(id, SCRAP, current, null, req != null ? req.reason() : null);
        auditService.safeRecordOperation(authService.currentUser().id(),
                "ASSET_SCRAP", "ASSET", id, AuditService.SUCCESS, null);
        return loadAsset(id);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> records(long id) {
        loadAsset(id);
        return assetRecordMapper.selectRecordsByAssetId(id);
    }

    private void insertRecord(long assetId, String type, Long fromUserId, Long toUserId, String reason) {
        AuthUser user = authService.currentUser();
        long id = sequenceService.nextId("asset_record");
        AssetRecord record = new AssetRecord();
        record.setId(id);
        record.setAssetId(assetId);
        record.setRecordType(type);
        record.setFromUserId(fromUserId);
        record.setToUserId(toUserId);
        record.setReason(reason);
        record.setOperatedBy(user.id());
        record.setOperatedAt(LocalDateTime.now());
        assetRecordMapper.insert(record);
    }

    private Map<String, Object> loadAsset(long id) {
        Map<String, Object> row = assetInfoMapper.selectAssetDetail(id);
        if (row == null || row.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "资产不存在");
        }
        return row;
    }

    private void assertStatus(Map<String, Object> asset, Set<String> allowed, String message) {
        if (!allowed.contains(String.valueOf(asset.get("status")))) {
            throw new BusinessException(ErrorCode.CONFLICT, message);
        }
    }

}
