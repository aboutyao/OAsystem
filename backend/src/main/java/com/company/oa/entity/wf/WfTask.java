package com.company.oa.entity.wf;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.oa.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_task")
public class WfTask extends BaseEntity {

    private String flowableTaskId;

    private String processInstanceId;

    private Long wfInstanceId;

    private String nodeId;

    private String nodeName;

    private Long assigneeId;

    private String assigneeNameSnapshot;

    private Long assigneeDeptId;

    private String taskType;

    private String status;

    private LocalDateTime dueAt;

    private LocalDateTime completedAt;

    private Long addSignOriginTaskId;

    private String addSignMode;

    private LocalDateTime slaDeadline;
}
