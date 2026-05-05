package com.company.oa.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.oa.entity.audit.AuditOperationLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditOperationLogMapper extends BaseMapper<AuditOperationLog> {
}
