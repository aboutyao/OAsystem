package com.company.oa.entity.form;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("form_version")
public class FormVersion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long templateId;

    private Integer versionNo;

    private String fieldsJson;

    private String layoutJson;

    private String status;

    private String changeReason;

    private LocalDateTime publishedAt;

    private Long publishedBy;

    private LocalDateTime createdAt;
}