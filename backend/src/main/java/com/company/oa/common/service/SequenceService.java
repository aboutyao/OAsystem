package com.company.oa.common.service;

import com.company.oa.common.mapper.SysSequenceMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SequenceService {

    private final SysSequenceMapper sequenceMapper;

    public SequenceService(SysSequenceMapper sequenceMapper) {
        this.sequenceMapper = sequenceMapper;
    }

    @Transactional
    public long nextId(String tableName) {
        sequenceMapper.getCurrentValueForUpdate(tableName);
        sequenceMapper.increment(tableName);
        Long id = sequenceMapper.getCurrentValueForUpdate(tableName);
        return id == null ? 1L : id;
    }
}
