package com.company.oa.entity.file;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.oa.common.entity.VersionedEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("file_library_folder")
public class FileLibraryFolder extends VersionedEntity {

    private String folderName;

    private Long parentId;

    private String folderPath;

    private Integer sortOrder;

    private Long createdBy;

    private String status;
}