package com.company.oa.entity.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_import_task")
public class SysImportTask {

    @TableId(type = IdType.INPUT)
    private Long id;

    private String taskCode;

    private String businessType;

    private String fileName;

    private Long fileSize;

    private Integer totalRows;

    private Integer successRows;

    private Integer failedRows;

    private String status;

    private String errorSummary;

    private Long submittedBy;

    private LocalDateTime submittedAt;

    private LocalDateTime finishedAt;
}
