package com.company.oa.entity.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_export_task")
public class SysExportTask {

    @TableId(type = IdType.INPUT)
    private Long id;

    private String taskCode;

    private String businessType;

    private String filterJson;

    private String fileName;

    private Long fileSize;

    private Integer rowCount;

    private String status;

    private String errorSummary;

    private Long submittedBy;

    private LocalDateTime submittedAt;

    private LocalDateTime finishedAt;

    private Integer downloadCount;
}
