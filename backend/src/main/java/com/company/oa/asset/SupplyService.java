package com.company.oa.asset;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.company.oa.asset.mapper.AssetSupplyMapper;
import com.company.oa.asset.mapper.AssetSupplyRecordMapper;
import com.company.oa.auth.AuthService;
import com.company.oa.auth.AuthUser;
import com.company.oa.common.api.PageResponse;
import com.company.oa.common.error.BusinessException;
import com.company.oa.common.error.ErrorCode;
import com.company.oa.common.service.PaginationHelper;
import com.company.oa.common.service.SequenceService;
import com.company.oa.entity.asset.AssetSupply;
import com.company.oa.entity.asset.AssetSupplyRecord;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class SupplyService {
    private static final String ENABLED = "ENABLED";

    private static final String IN = "IN";
    private static final String OUT = "OUT";
    private static final String RETURN = "RETURN";
    private static final String ADJUST = "ADJUST";

    private final AssetSupplyMapper assetSupplyMapper;
    private final AssetSupplyRecordMapper assetSupplyRecordMapper;
    private final PaginationHelper paginationHelper;
    private final AuthService authService;
    private final SequenceService sequenceService;

    public SupplyService(AssetSupplyMapper assetSupplyMapper, AssetSupplyRecordMapper assetSupplyRecordMapper,
                         PaginationHelper paginationHelper, AuthService authService, SequenceService sequenceService) {
        this.assetSupplyMapper = assetSupplyMapper;
        this.assetSupplyRecordMapper = assetSupplyRecordMapper;
        this.paginationHelper = paginationHelper;
        this.authService = authService;
        this.sequenceService = sequenceService;
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> list(long page, long size, String category, String status, String keyword) {
        long[] ps = paginationHelper.clamp(page, size);
        LambdaQueryWrapper<AssetSupply> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetSupply::getDeleted, 0);
        if (category != null && !category.isBlank()) {
            wrapper.eq(AssetSupply::getCategory, category);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(AssetSupply::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + keyword.trim() + "%";
            wrapper.and(w -> w.like(AssetSupply::getSupplyCode, like).or().like(AssetSupply::getSupplyName, like));
        }
        long total = Objects.requireNonNullElse(assetSupplyMapper.selectCount(wrapper), 0L);
        wrapper.orderByDesc(AssetSupply::getId);
        wrapper.last("limit " + ps[1] + " offset " + (ps[0] - 1) * ps[1]);
        List<AssetSupply> entities = assetSupplyMapper.selectList(wrapper);
        List<Map<String, Object>> items = new ArrayList<>();
        for (AssetSupply e : entities) {
            items.add(entityToMap(e));
        }
        return new PageResponse<>(ps[0], ps[1], total, items);
    }

    @Transactional
    public Map<String, Object> create(SupplyDtos.SupplyCreateRequest req) {
        long id = sequenceService.nextId("asset_supply");
        try {
            AssetSupply entity = new AssetSupply();
            entity.setId(id);
            entity.setSupplyCode(req.supplyCode());
            entity.setSupplyName(req.supplyName());
            entity.setCategory(req.category());
            entity.setUnit(req.unit());
            entity.setStockQuantity(BigDecimal.ZERO);
            entity.setWarningQuantity(req.warningQuantity());
            entity.setStatus(ENABLED);
            entity.setRemark(req.remark());
            entity.setCreatedAt(java.time.LocalDateTime.now());
            entity.setUpdatedAt(java.time.LocalDateTime.now());
            assetSupplyMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.CONFLICT, "用品编号已存在");
        }
        return loadSupply(id);
    }

    @Transactional
    public Map<String, Object> update(long id, SupplyDtos.SupplyUpdateRequest req) {
        loadSupply(id);
        AssetSupply entity = new AssetSupply();
        entity.setSupplyName(req.supplyName());
        entity.setCategory(req.category());
        entity.setUnit(req.unit());
        entity.setWarningQuantity(req.warningQuantity());
        entity.setStatus(req.status());
        entity.setRemark(req.remark());
        assetSupplyMapper.update(entity, new LambdaQueryWrapper<AssetSupply>()
                .eq(AssetSupply::getId, id)
                .eq(AssetSupply::getDeleted, 0));
        return loadSupply(id);
    }

    @Transactional
    public Map<String, Object> stockIn(long id, SupplyDtos.SupplyMovementRequest req) {
        loadSupply(id);
        int rows = assetSupplyMapper.update(null, new LambdaUpdateWrapper<AssetSupply>()
                .eq(AssetSupply::getId, id)
                .eq(AssetSupply::getDeleted, 0)
                .setSql("stock_quantity = stock_quantity + " + req.quantity()));
        if (rows != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "入库失败，请重试");
        }
        insertRecord(id, IN, req.quantity(), null, req.reason());
        return loadSupply(id);
    }

    @Transactional
    public Map<String, Object> stockOut(long id, SupplyDtos.SupplyMovementRequest req) {
        AuthUser user = authService.currentUser();
        loadSupply(id);
        int rows = assetSupplyMapper.update(null, new LambdaUpdateWrapper<AssetSupply>()
                .eq(AssetSupply::getId, id)
                .eq(AssetSupply::getDeleted, 0)
                .ge(AssetSupply::getStockQuantity, req.quantity())
                .setSql("stock_quantity = stock_quantity - " + req.quantity()));
        if (rows != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "库存不足");
        }
        Long target = req.userId() != null ? req.userId() : user.id();
        insertRecord(id, OUT, req.quantity(), target, req.reason());
        return loadSupply(id);
    }

    @Transactional
    public Map<String, Object> returnSupply(long id, SupplyDtos.SupplyMovementRequest req) {
        AuthUser user = authService.currentUser();
        loadSupply(id);
        int rows = assetSupplyMapper.update(null, new LambdaUpdateWrapper<AssetSupply>()
                .eq(AssetSupply::getId, id)
                .eq(AssetSupply::getDeleted, 0)
                .setSql("stock_quantity = stock_quantity + " + req.quantity()));
        if (rows != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "退回失败，请重试");
        }
        Long target = req.userId() != null ? req.userId() : user.id();
        insertRecord(id, RETURN, req.quantity(), target, req.reason());
        return loadSupply(id);
    }

    @Transactional
    public Map<String, Object> adjust(long id, SupplyDtos.SupplyAdjustRequest req) {
        Map<String, Object> supply = loadSupply(id);
        BigDecimal current = (BigDecimal) supply.get("stockQuantity");
        if (current == null) {
            current = BigDecimal.ZERO;
        }
        BigDecimal delta = req.quantity().subtract(current);
        AssetSupply entity = new AssetSupply();
        entity.setStockQuantity(req.quantity());
        entity.setUpdatedAt(java.time.LocalDateTime.now());
        int rows = assetSupplyMapper.update(entity, new LambdaQueryWrapper<AssetSupply>()
                .eq(AssetSupply::getId, id)
                .eq(AssetSupply::getDeleted, 0));
        if (rows != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "调整失败，请重试");
        }
        insertRecord(id, ADJUST, delta, null, req.reason());
        return loadSupply(id);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> records(Long supplyId) {
        LambdaQueryWrapper<AssetSupplyRecord> wrapper = new LambdaQueryWrapper<>();
        if (supplyId != null) {
            wrapper.eq(AssetSupplyRecord::getSupplyId, supplyId);
        }
        return assetSupplyRecordMapper.selectRecordsWithJoins(wrapper);
    }

    private void insertRecord(long supplyId, String type, BigDecimal quantity, Long userId, String reason) {
        AuthUser user = authService.currentUser();
        long id = sequenceService.nextId("asset_supply_record");
        AssetSupplyRecord record = new AssetSupplyRecord();
        record.setId(id);
        record.setSupplyId(supplyId);
        record.setRecordType(type);
        record.setQuantity(quantity);
        record.setUserId(userId);
        record.setReason(reason);
        record.setOperatedBy(user.id());
        record.setOperatedAt(java.time.LocalDateTime.now());
        assetSupplyRecordMapper.insert(record);
    }

    private Map<String, Object> loadSupply(long id) {
        AssetSupply entity = assetSupplyMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用品不存在");
        }
        return entityToMap(entity);
    }

    private Map<String, Object> entityToMap(AssetSupply entity) {
        java.util.LinkedHashMap<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("id", entity.getId());
        map.put("supplyCode", entity.getSupplyCode());
        map.put("supplyName", entity.getSupplyName());
        map.put("category", entity.getCategory());
        map.put("unit", entity.getUnit());
        map.put("stockQuantity", entity.getStockQuantity());
        map.put("warningQuantity", entity.getWarningQuantity());
        map.put("status", entity.getStatus());
        map.put("remark", entity.getRemark());
        map.put("createdAt", entity.getCreatedAt());
        map.put("updatedAt", entity.getUpdatedAt());
        map.put("version", entity.getVersion());
        return map;
    }

}
