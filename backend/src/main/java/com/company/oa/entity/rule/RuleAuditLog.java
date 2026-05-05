package com.company.oa.entity.rule;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rule_audit_log")
public class RuleAuditLog {

    @TableId(type = IdType.INPUT)
    private Long id;

    private Long ruleId;

    private Long ruleVersionId;

    private String action;

    private String beforeData;

    private String afterData;

    private String reason;

    private Long operatorId;

    private LocalDateTime operatedAt;
}
