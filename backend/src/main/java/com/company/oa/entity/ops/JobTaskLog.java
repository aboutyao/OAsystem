package com.company.oa.entity.ops;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("job_task_log")
public class JobTaskLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String jobCode;

    private String jobName;

    private String status;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    private Long durationMs;

    private Long successCount;

    private Long failCount;

    private String failReason;

    private String triggeredBy;
}