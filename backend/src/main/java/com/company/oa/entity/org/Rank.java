package com.company.oa.entity.org;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.oa.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("org_rank")
public class Rank extends BaseEntity {

    private String rankCode;

    private String rankName;

    private Integer rankLevel;

    private String status;

    private String remark;
}
