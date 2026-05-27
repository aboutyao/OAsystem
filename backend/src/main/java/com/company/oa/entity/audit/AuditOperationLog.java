package com.company.oa.entity.audit;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("audit_operation_log")
public class AuditOperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String requestId;

    private Long operatorId;

    private String operationType;

    private String businessType;

    private Long businessId;

    private String requestMethod;

    private String requestUri;

    private String requestParams;

    private String result;

    private String errorMessage;

    private String ipAddress;

    private String oldValue;

    private String newValue;

    private LocalDateTime operatedAt;

}
