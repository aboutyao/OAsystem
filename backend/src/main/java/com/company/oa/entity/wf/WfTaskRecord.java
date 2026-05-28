package com.company.oa.entity.wf;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wf_task_record")
public class WfTaskRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long wfInstanceId;

    private Long taskId;

    private String action;

    private Long operatorId;

    private String operatorNameSnapshot;

    private String nodeName;

    private String comment;

    private String attachmentIds;

    private Long parentRecordId;

    private LocalDateTime operatedAt;
}
