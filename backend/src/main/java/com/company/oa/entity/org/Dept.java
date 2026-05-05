package com.company.oa.entity.org;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.oa.common.entity.SoftDeleteEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("org_dept")
public class Dept extends SoftDeleteEntity {

    private Long parentId;

    private String deptCode;

    private String deptName;

    private String deptPath;

    private Long leaderUserId;

    private Integer sortOrder;

    private String status;
}