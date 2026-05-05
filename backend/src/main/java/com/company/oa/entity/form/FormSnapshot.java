package com.company.oa.entity.form;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("form_snapshot")
public class FormSnapshot {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long templateId;

    private Long versionId;

    private String businessType;

    private Long businessId;

    private String dataJson;

    private LocalDateTime createdAt;
}