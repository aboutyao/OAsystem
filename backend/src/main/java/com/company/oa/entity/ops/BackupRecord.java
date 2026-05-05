package com.company.oa.entity.ops;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("backup_record")
public class BackupRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String backupType;

    private String backupPath;

    private Long backupSize;

    private String status;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private Long durationMs;

    private String failReason;

    private String triggeredBy;
}