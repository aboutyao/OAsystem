package com.company.oa.entity.wf;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wf_cc_record")
public class WfCcRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long wfInstanceId;

    private Long receiverId;

    private String ccReason;

    private Long createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime readAt;
}
