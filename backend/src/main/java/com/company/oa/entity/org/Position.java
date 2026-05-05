package com.company.oa.entity.org;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.oa.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("org_position")
public class Position extends BaseEntity {

    private String positionCode;

    private String positionName;

    private String status;

    private Integer sortOrder;

    private String remark;
}
