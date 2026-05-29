package com.company.oa.entity.oa;

import com.company.oa.common.entity.VersionedEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_seal_apply")
public class OaSealApply extends VersionedEntity {

    private String processInstanceId;

    private Long wfInstanceId;

    private Long ruleVersionId;

    private String sealType;

    private String sealName;

    private String fileTitle;

    private String useReason;

    private LocalDateTime useAt;

    private Integer outFlag;

    private String attachments;

    private LocalDateTime returnAt;

    private String status;

    private Long createdBy;

    private String createdNameSnapshot;

    private Long createdDeptId;

    private String createdDeptNameSnapshot;
}
