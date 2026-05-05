package com.company.oa.entity.rule;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.oa.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("rule_definition")
public class RuleDefinition extends BaseEntity {

    private Long groupId;

    private String ruleCode;

    private String ruleName;

    private String ruleType;

    private String description;

    private String status;

    private String businessType;
}
