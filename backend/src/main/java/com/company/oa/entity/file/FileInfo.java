package com.company.oa.entity.file;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.oa.common.entity.VersionedEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("file_info")
public class FileInfo extends VersionedEntity {

    private Long folderId;

    private String fileName;

    private String fileExt;

    private String mimeType;

    private Long fileSize;

    private String storageType;

    private String storagePath;

    private String checksum;

    private Long uploadUserId;

    private String status;
}