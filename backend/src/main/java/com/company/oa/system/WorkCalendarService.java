package com.company.oa.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.oa.common.api.PageResponse;
import com.company.oa.common.error.BusinessException;
import com.company.oa.common.error.ErrorCode;
import com.company.oa.common.service.SequenceService;
import com.company.oa.entity.system.SysWorkCalendar;
import com.company.oa.system.mapper.SysWorkCalendarMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class WorkCalendarService {
    private static final Set<String> DAY_TYPES = Set.of("WORKDAY", "WEEKEND", "HOLIDAY", "ADJUSTED_WORKDAY");

    private final SysWorkCalendarMapper calendarMapper;
    private final SequenceService sequenceService;
    private final ObjectMapper objectMapper;

    public WorkCalendarService(SysWorkCalendarMapper calendarMapper, SequenceService sequenceService) {
        this.calendarMapper = calendarMapper;
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
    public PageResponse<Map<String, Object>> list(LocalDate from, LocalDate to, long page, long size) {
        long p = page < 1 ? 1 : page;
        long s = size < 1 ? 50 : Math.min(size, 366);

        LambdaQueryWrapper<SysWorkCalendar> wrapper = new LambdaQueryWrapper<>();
        if (from != null) {
            wrapper.ge(SysWorkCalendar::getCalDate, from);
        }
        if (to != null) {
            wrapper.le(SysWorkCalendar::getCalDate, to);
        }
        wrapper.orderByAsc(SysWorkCalendar::getCalDate);

        Page<SysWorkCalendar> pageObj = new Page<>(p, s);
        calendarMapper.selectPage(pageObj, wrapper);
        List<Map<String, Object>> items = pageObj.getRecords().stream()
                .map(this::toMap)
                .collect(Collectors.toList());
        return new PageResponse<>(p, s, pageObj.getTotal(), items);
    }

    @Transactional
    public Map<String, Object> upsert(SystemDtos.WorkCalendarUpsertRequest req) {
        if (!DAY_TYPES.contains(req.dayType())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "dayType 仅支持 WORKDAY/WEEKEND/HOLIDAY/ADJUSTED_WORKDAY");
        }
        SysWorkCalendar existing = calendarMapper.selectOne(
                new LambdaQueryWrapper<SysWorkCalendar>()
                        .eq(SysWorkCalendar::getCalDate, req.calDate()));
        if (existing == null) {
            long id = sequenceService.nextId("sys_work_calendar");
            SysWorkCalendar entity = new SysWorkCalendar();
            entity.setId(id);
            entity.setCalDate(req.calDate());
            entity.setDayType(req.dayType());
            entity.setDescription(req.description());
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            calendarMapper.insert(entity);
        } else {
            calendarMapper.update(null, new LambdaUpdateWrapper<SysWorkCalendar>()
                    .eq(SysWorkCalendar::getCalDate, req.calDate())
                    .set(SysWorkCalendar::getDayType, req.dayType())
                    .set(SysWorkCalendar::getDescription, req.description())
                    .set(SysWorkCalendar::getUpdatedAt, LocalDateTime.now()));
        }
        return get(req.calDate());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> get(LocalDate calDate) {
        SysWorkCalendar cal = calendarMapper.selectOne(
                new LambdaQueryWrapper<SysWorkCalendar>()
                        .eq(SysWorkCalendar::getCalDate, calDate));
        if (cal == null) {
            // 默认根据周末判定
            DayOfWeek dow = calDate.getDayOfWeek();
            String inferred = (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) ? "WEEKEND" : "WORKDAY";
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("calDate", calDate.toString());
            m.put("dayType", inferred);
            m.put("inferred", true);
            return m;
        }
        return toMap(cal);
    }

    /**
     * 计算两个日期之间的工作日天数（含起止日，遇周末/节假日跳过，调休工作日计入）。
     */
    @Transactional(readOnly = true)
    public int countWorkdays(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "日期参数不合法");
        }
        // 加载该区间内所有 holiday/adjusted_workday
        List<SysWorkCalendar> rows = calendarMapper.selectList(
                new LambdaQueryWrapper<SysWorkCalendar>()
                        .ge(SysWorkCalendar::getCalDate, from)
                        .le(SysWorkCalendar::getCalDate, to));
        Map<LocalDate, String> overrides = new HashMap<>();
        for (SysWorkCalendar r : rows) {
            overrides.put(r.getCalDate(), r.getDayType());
        }
        int count = 0;
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            String type = overrides.get(d);
            if (type == null) {
                DayOfWeek dow = d.getDayOfWeek();
                if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) count++;
            } else if ("WORKDAY".equals(type) || "ADJUSTED_WORKDAY".equals(type)) {
                count++;
            }
        }
        return count;
    }

    @Transactional
    public Map<String, Object> remove(LocalDate calDate) {
        int n = calendarMapper.delete(
                new LambdaQueryWrapper<SysWorkCalendar>()
                        .eq(SysWorkCalendar::getCalDate, calDate));
        if (n == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "记录不存在");
        }
        return Map.of("calDate", calDate.toString(), "removed", true);
    }

    public Long total() {
        return calendarMapper.selectCount(null);
    }
}
