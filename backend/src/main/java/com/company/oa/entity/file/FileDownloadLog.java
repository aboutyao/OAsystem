package com.company.oa.entity.file;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("file_download_log")
public class FileDownloadLog {

    @TableId(type = IdType.INPUT)
    private Long id;

    private Long fileId;

    private Long userId;

    @TableField(exist = false)
    private String userName;

    private String businessType;

    private Long businessId;

    private String ipAddress;

    private String userAgent;

    private LocalDateTime downloadedAt;
}
