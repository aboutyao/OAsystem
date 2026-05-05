package com.company.oa.entity.perm;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.oa.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("perm_role")
public class PermRole extends BaseEntity {

    private String roleCode;

    private String roleName;

    private String roleType;

    private String status;

    private Integer sortOrder;

    private String remark;
}
