package com.company.oa.entity.form;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.oa.common.entity.SoftDeleteEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("form_template")
public class FormTemplate extends SoftDeleteEntity {

    private String templateCode;

    private String templateName;

    private String businessType;

    private String description;

    private String status;

    private Long currentVersionId;
}