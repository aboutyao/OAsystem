package com.company.oa.entity.wf;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.oa.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_process_version")
public class WfProcessVersion extends BaseEntity {

    private Long templateId;

    private Integer versionNo;

    private String flowableDefinitionId;

    private String bpmnXml;

    private String formConfig;

    private String status;

    private LocalDateTime publishedAt;

    private Long publishedBy;

    private String changeReason;
}
