package com.company.oa.entity.perm;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("perm_temp_auth")
public class PermTempAuth {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String authType;

    private Long targetId;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    private String reason;

    private String status;

    private Long createdBy;

    private LocalDateTime createdAt;
}
