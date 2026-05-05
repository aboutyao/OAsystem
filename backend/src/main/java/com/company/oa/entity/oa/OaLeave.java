package com.company.oa.entity.oa;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.company.oa.common.entity.VersionedEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_leave")
public class OaLeave extends VersionedEntity {

    private String processInstanceId;

    private Long wfInstanceId;

    private Long ruleVersionId;

    private String leaveType;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    private BigDecimal durationHours;

    private BigDecimal durationDays;

    private String reason;

    private String handoverNote;

    private String status;

    private Long createdBy;

    private String createdNameSnapshot;

    private Long createdDeptId;

    private String createdDeptNameSnapshot;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
