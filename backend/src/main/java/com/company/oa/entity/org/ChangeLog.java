package com.company.oa.entity.org;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("org_change_log")
public class ChangeLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String targetType;

    private Long targetId;

    private String changeType;

    private String beforeData;

    private String afterData;

    private String reason;

    private Long operatorId;

    private LocalDateTime operatedAt;
}
