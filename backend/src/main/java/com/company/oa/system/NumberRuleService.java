package com.company.oa.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.oa.common.api.PageResponse;
import com.company.oa.common.error.BusinessException;
import com.company.oa.common.error.ErrorCode;
import com.company.oa.common.service.SequenceService;
import com.company.oa.entity.system.SysNumberRule;
import com.company.oa.system.mapper.SysNumberRuleMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NumberRuleService {
    private static final Set<String> RESETS = Set.of("DAILY", "MONTHLY", "YEARLY", "NEVER");

    private final SysNumberRuleMapper ruleMapper;
    private final SequenceService sequenceService;
    private final ObjectMapper objectMapper;

    public NumberRuleService(SysNumberRuleMapper ruleMapper, SequenceService sequenceService) {
        this.ruleMapper = ruleMapper;
        this.sequenceService = sequenceService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Object entity) {
        return objectMapper.convertValue(entity, Map.class);
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> list(long page, long size) {
        long p = page < 1 ? 1 : page;
        long s = size < 1 ? 20 : Math.min(size, 100);
        Page<SysNumberRule> pageObj = new Page<>(p, s);
        ruleMapper.selectPage(pageObj, new LambdaQueryWrapper<SysNumberRule>()
                .orderByAsc(SysNumberRule::getId));
        List<Map<String, Object>> items = pageObj.getRecords().stream()
                .map(this::toMap)
                .collect(Collectors.toList());
        return new PageResponse<>(p, s, pageObj.getTotal(), items);
    }

    @Transactional
    public Map<String, Object> create(SystemDtos.NumberRuleCreateRequest req) {
        validateReset(req.seqReset());
        Long exists = ruleMapper.selectCount(
                new LambdaQueryWrapper<SysNumberRule>().eq(SysNumberRule::getRuleCode, req.ruleCode()));
        if (exists != null && exists > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "规则编码已存在");
        }
        long id = sequenceService.nextId("sys_number_rule");
        SysNumberRule entity = new SysNumberRule();
        entity.setId(id);
        entity.setRuleCode(req.ruleCode());
        entity.setBusinessType(req.businessType());
        entity.setPrefix(req.prefix());
        entity.setDatePattern(req.datePattern());
        entity.setSeqLength(req.seqLength());
        entity.setSeqReset(req.seqReset());
        entity.setDescription(req.description());
        entity.setStatus("ENABLED");
        entity.setCurrentSeq(0L);
        entity.setVersion(0);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        ruleMapper.insert(entity);
        return get(id);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> get(long id) {
        SysNumberRule rule = ruleMapper.selectById(id);
        if (rule == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "规则不存在");
        }
        return toMap(rule);
    }

    /**
     * 生成下一个编号；并发安全：通过 version 乐观锁 + period 重置实现。
     */
    @Transactional
    public String generateNext(String ruleCode) {
        for (int attempt = 0; attempt < 5; attempt++) {
            SysNumberRule rule = ruleMapper.selectOne(
                    new LambdaQueryWrapper<SysNumberRule>()
                            .eq(SysNumberRule::getRuleCode, ruleCode)
                            .eq(SysNumberRule::getStatus, "ENABLED"));
            if (rule == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "编号规则不存在或已停用");
            }

            String period = computePeriod(rule.getSeqReset(), rule.getDatePattern());
            long nextSeq;
            String nextPeriod;
            if (period == null || period.equals(rule.getCurrentPeriod())) {
                nextSeq = (rule.getCurrentSeq() == null ? 0L : rule.getCurrentSeq()) + 1;
                nextPeriod = rule.getCurrentPeriod();
            } else {
                nextSeq = 1;
                nextPeriod = period;
            }

            int n = ruleMapper.update(null, new LambdaUpdateWrapper<SysNumberRule>()
                    .eq(SysNumberRule::getId, rule.getId())
                    .eq(SysNumberRule::getVersion, rule.getVersion())
                    .set(SysNumberRule::getCurrentPeriod, nextPeriod)
                    .set(SysNumberRule::getCurrentSeq, nextSeq)
                    .set(SysNumberRule::getUpdatedAt, LocalDateTime.now()));
            if (n == 1) {
                String dateSeg = rule.getDatePattern() == null || rule.getDatePattern().isBlank()
                        ? "" : LocalDate.now().format(DateTimeFormatter.ofPattern(rule.getDatePattern()));
                String seqStr = String.format("%0" + rule.getSeqLength() + "d", nextSeq);
                return rule.getPrefix() + dateSeg + seqStr;
            }
            // 乐观锁失败重试
        }
        throw new BusinessException(ErrorCode.CONFLICT, "编号生成冲突，请重试");
    }

    private String computePeriod(String seqReset, String datePattern) {
        if (seqReset == null) return null;
        return switch (seqReset) {
            case "DAILY" -> LocalDate.now().toString();
            case "MONTHLY" -> LocalDate.now().toString().substring(0, 7);
            case "YEARLY" -> LocalDate.now().toString().substring(0, 4);
            case "NEVER" -> "ALL";
            default -> "ALL";
        };
    }

    private void validateReset(String seqReset) {
        if (seqReset == null || !RESETS.contains(seqReset)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "seqReset 仅支持 DAILY/MONTHLY/YEARLY/NEVER");
        }
    }
}
