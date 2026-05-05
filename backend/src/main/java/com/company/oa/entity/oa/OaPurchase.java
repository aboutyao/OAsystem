package com.company.oa.entity.oa;

import com.company.oa.common.entity.VersionedEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_purchase")
public class OaPurchase extends VersionedEntity {

    private String processInstanceId;

    private Long wfInstanceId;

    private Long ruleVersionId;

    private String purchaseNo;

    private String purchaseType;

    private String supplierName;

    private String budgetSubject;

    private BigDecimal totalAmount;

    private String arrivalStatus;

    private String acceptanceStatus;

    private String reason;

    private String status;

    private Long createdBy;

    private String createdNameSnapshot;

    private Long createdDeptId;

    private String createdDeptNameSnapshot;
}
