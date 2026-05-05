package com.company.oa.entity.contract;

import com.company.oa.common.entity.VersionedEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("contract_info")
public class ContractInfo extends VersionedEntity {

    private String processInstanceId;

    private Long wfInstanceId;

    private Long ruleVersionId;

    private String contractNo;

    private String contractName;

    private String contractType;

    private String counterparty;

    private BigDecimal amount;

    private LocalDate startDate;

    private LocalDate endDate;

    private LocalDate signDate;

    private String status;

    private Long createdBy;

    private String createdNameSnapshot;

    private Long createdDeptId;

    private String createdDeptNameSnapshot;
}