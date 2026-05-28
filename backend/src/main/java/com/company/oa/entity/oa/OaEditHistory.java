package com.company.oa.entity.oa;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("oa_edit_history")
public class OaEditHistory {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String entityType;

    private Long entityId;

    private String snapshotJson;

    private Long editedBy;

    private LocalDateTime editedAt;
}
