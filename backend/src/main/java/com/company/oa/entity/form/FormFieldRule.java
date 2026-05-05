package com.company.oa.entity.form;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.oa.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("form_field_rule")
public class FormFieldRule extends BaseEntity {

    private Long templateId;

    private String fieldCode;

    private String ruleType;

    private String ruleExpression;

    private String errorMessage;

    private String description;

    private String status;
}
