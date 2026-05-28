package com.company.oa.entity.wf;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.oa.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_approval_rule")
public class WfApprovalRule extends BaseEntity {
    private String businessType;
    private String ruleName;
    private String conditionType;
    private BigDecimal conditionValue;
    private String approvalChain;
    private Integer priority;
    private String status;
    private Long createdBy;
}
