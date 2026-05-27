package com.company.oa.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.oa.common.api.PageResponse;
import com.company.oa.common.error.BusinessException;
import com.company.oa.common.error.ErrorCode;
import com.company.oa.common.service.PaginationHelper;
import com.company.oa.common.service.SequenceService;
import com.company.oa.entity.system.SysConfig;
import com.company.oa.entity.system.SysDictItem;
import com.company.oa.entity.system.SysDictType;
import com.company.oa.system.cache.SystemCacheService;
import com.company.oa.system.mapper.SysConfigMapper;
import com.company.oa.system.mapper.SysDictItemMapper;
import com.company.oa.system.mapper.SysDictTypeMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SystemService {
    private final SysConfigMapper configMapper;
    private final SysDictTypeMapper dictTypeMapper;
    private final SysDictItemMapper dictItemMapper;
    private final SequenceService sequenceService;
    private final SystemCacheService cacheService;
    private final PaginationHelper paginationHelper;
    private final ObjectMapper objectMapper;

    public SystemService(SysConfigMapper configMapper,
                         SysDictTypeMapper dictTypeMapper,
                         SysDictItemMapper dictItemMapper,
                         SequenceService sequenceService,
                         SystemCacheService cacheService,
                         PaginationHelper paginationHelper) {
        this.configMapper = configMapper;
        this.dictTypeMapper = dictTypeMapper;
        this.dictItemMapper = dictItemMapper;
        this.sequenceService = sequenceService;
        this.cacheService = cacheService;
        this.paginationHelper = paginationHelper;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Object entity) {
        return objectMapper.convertValue(entity, Map.class);
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> dictTypes(long page, long size) {
        long[] ps = paginationHelper.clamp(page, size);
        Page<SysDictType> pageObj = new Page<>(ps[0], ps[1]);
        dictTypeMapper.selectPage(pageObj, new LambdaQueryWrapper<SysDictType>()
                .orderByAsc(SysDictType::getId));
        List<Map<String, Object>> items = pageObj.getRecords().stream()
                .map(this::toMap)
                .collect(Collectors.toList());
        return new PageResponse<>(ps[0], ps[1], pageObj.getTotal(), items);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> dictItemsByTypeCode(String dictCode) {
        List<Map<String, Object>> cached = cacheService.getDictItems(dictCode);
        if (cached != null) {
            return cached;
        }
        SysDictType type = dictTypeMapper.selectOne(
                new LambdaQueryWrapper<SysDictType>().eq(SysDictType::getDictCode, dictCode));
        if (type == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "字典类型不存在");
        }
        List<SysDictItem> items = dictItemMapper.selectList(
                new LambdaQueryWrapper<SysDictItem>()
                        .eq(SysDictItem::getDictTypeId, type.getId())
                        .orderByAsc(SysDictItem::getSortOrder)
                        .orderByAsc(SysDictItem::getId));
        List<Map<String, Object>> result = items.stream().map(this::toMap).collect(Collectors.toList());
        cacheService.setDictItems(dictCode, result);
        return result;
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> configs(long page, long size) {
        long[] ps = paginationHelper.clamp(page, size);
        Page<SysConfig> pageObj = new Page<>(ps[0], ps[1]);
        configMapper.selectPage(pageObj, new LambdaQueryWrapper<SysConfig>()
                .orderByAsc(SysConfig::getConfigGroup)
                .orderByAsc(SysConfig::getConfigKey));
        List<Map<String, Object>> items = pageObj.getRecords().stream()
                .map(this::toMap)
                .collect(Collectors.toList());
        return new PageResponse<>(ps[0], ps[1], pageObj.getTotal(), items);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> configByKey(String key) {
        SysConfig config = configMapper.selectOne(
                new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, key));
        if (config == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "参数不存在");
        }
        return toMap(config);
    }

    @Transactional
    public Map<String, Object> updateConfig(String key, SystemDtos.ConfigUpdateRequest req) {
        SysConfig config = configMapper.selectOne(
                new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, key));
        if (config == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "参数不存在");
        }
        if (config.getEditable() != null && config.getEditable() == 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "该参数不可编辑");
        }
        configMapper.update(null, new LambdaUpdateWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, key)
                .set(SysConfig::getConfigValue, req.configValue())
                .set(SysConfig::getUpdatedAt, LocalDateTime.now()));
        cacheService.invalidateConfig(key);
        return configByKey(key);
    }

    // ============ 字典类型 CRUD ============

    @Transactional
    public Map<String, Object> createDictType(SystemDtos.DictTypeCreateRequest req) {
        Long exists = dictTypeMapper.selectCount(
                new LambdaQueryWrapper<SysDictType>().eq(SysDictType::getDictCode, req.dictCode()));
        if (exists != null && exists > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "字典编码已存在");
        }
        long id = sequenceService.nextId("sys_dict_type");
        SysDictType entity = new SysDictType();
        entity.setId(id);
        entity.setDictCode(req.dictCode());
        entity.setDictName(req.dictName());
        entity.setStatus("ENABLED");
        entity.setRemark(req.remark());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        dictTypeMapper.insert(entity);
        return dictTypeById(id);
    }

    @Transactional
    public Map<String, Object> updateDictType(long id, SystemDtos.DictTypeUpdateRequest req) {
        dictTypeById(id);
        dictTypeMapper.update(null, new LambdaUpdateWrapper<SysDictType>()
                .eq(SysDictType::getId, id)
                .set(SysDictType::getDictName, req.dictName())
                .set(SysDictType::getRemark, req.remark())
                .set(SysDictType::getUpdatedAt, LocalDateTime.now()));
        return dictTypeById(id);
    }

    @Transactional
    public Map<String, Object> deleteDictType(long id) {
        dictTypeById(id);
        Long itemCount = dictItemMapper.selectCount(
                new LambdaQueryWrapper<SysDictItem>().eq(SysDictItem::getDictTypeId, id));
        if (itemCount != null && itemCount > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该字典类型下存在字典项，不可删除");
        }
        dictTypeMapper.deleteById(id);
        return Map.of("id", id, "deleted", true);
    }

    private Map<String, Object> dictTypeById(long id) {
        SysDictType type = dictTypeMapper.selectById(id);
        if (type == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "字典类型不存在");
        }
        cacheService.invalidateDict(type.getDictCode());
        return toMap(type);
    }

    // ============ 字典项 CRUD ============

    @Transactional
    public Map<String, Object> createDictItem(SystemDtos.DictItemCreateRequest req) {
        Long typeCount = dictTypeMapper.selectCount(
                new LambdaQueryWrapper<SysDictType>().eq(SysDictType::getId, req.dictTypeId()));
        if (typeCount == null || typeCount == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "字典类型不存在");
        }
        long id = sequenceService.nextId("sys_dict_item");
        SysDictItem entity = new SysDictItem();
        entity.setId(id);
        entity.setDictTypeId(req.dictTypeId());
        entity.setItemLabel(req.itemLabel());
        entity.setItemValue(req.itemValue());
        entity.setSortOrder(req.sortOrder());
        entity.setStatus("ENABLED");
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        dictItemMapper.insert(entity);
        invalidateDictCacheByTypeId(req.dictTypeId());
        return dictItemById(id);
    }

    @Transactional
    public Map<String, Object> updateDictItem(long id, SystemDtos.DictItemUpdateRequest req) {
        SysDictItem existing = dictItemMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "字典项不存在");
        }
        dictItemMapper.update(null, new LambdaUpdateWrapper<SysDictItem>()
                .eq(SysDictItem::getId, id)
                .set(SysDictItem::getItemLabel, req.itemLabel())
                .set(SysDictItem::getItemValue, req.itemValue())
                .set(SysDictItem::getSortOrder, req.sortOrder())
                .set(SysDictItem::getUpdatedAt, LocalDateTime.now()));
        invalidateDictCacheByTypeId(existing.getDictTypeId());
        return dictItemById(id);
    }

    @Transactional
    public Map<String, Object> deleteDictItem(long id) {
        SysDictItem existing = dictItemMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "字典项不存在");
        }
        dictItemMapper.deleteById(id);
        invalidateDictCacheByTypeId(existing.getDictTypeId());
        return Map.of("id", id, "deleted", true);
    }

    private void invalidateDictCacheByTypeId(long dictTypeId) {
        SysDictType type = dictTypeMapper.selectById(dictTypeId);
        if (type != null && type.getDictCode() != null) {
            cacheService.invalidateDict(type.getDictCode());
        }
    }

    private Map<String, Object> dictItemById(long id) {
        SysDictItem item = dictItemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "字典项不存在");
        }
        return toMap(item);
    }

}
