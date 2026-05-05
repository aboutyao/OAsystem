package com.company.oa.entity.oa;

import com.company.oa.common.entity.VersionedEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_expense")
public class OaExpense extends VersionedEntity {

    private String processInstanceId;

    private Long wfInstanceId;

    private Long ruleVersionId;

    private String expenseNo;

    private String expenseType;

    private BigDecimal totalAmount;

    private BigDecimal paidAmount;

    private String payeeAccount;

    private String paymentStatus;

    private LocalDateTime paidAt;

    private String reason;

    private String status;

    private Long createdBy;

    private String createdNameSnapshot;

    private Long createdDeptId;

    private String createdDeptNameSnapshot;
}
