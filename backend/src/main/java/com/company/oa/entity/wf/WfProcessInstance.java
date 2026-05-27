package com.company.oa.entity.wf;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.company.oa.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_process_instance")
public class WfProcessInstance extends BaseEntity {

    private String processInstanceId;

    private Long templateId;

    private Long processVersionId;

    private String businessType;

    private Long businessId;

    private String title;

    private Long starterId;

    private String starterNameSnapshot;

    private Long starterDeptId;

    private String starterDeptNameSnapshot;

    private String currentNodeName;

    private String status;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    private Integer deleted;

    private LocalDateTime slaDeadline;

    private boolean slaBreached;
}
