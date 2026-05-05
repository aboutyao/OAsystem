package com.company.oa.entity.wf;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wf_delegation")
public class WfDelegation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long delegatorId;

    private Long delegateeId;

    private String businessScope;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    private String reason;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime cancelledAt;
}