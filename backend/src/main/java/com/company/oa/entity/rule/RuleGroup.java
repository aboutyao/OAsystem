package com.company.oa.entity.rule;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.oa.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("rule_group")
public class RuleGroup extends BaseEntity {

    private String groupCode;

    private String groupName;

    private String description;

    private String status;
}
