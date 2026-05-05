package com.company.oa.entity.wf;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.oa.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_process_template")
public class WfProcessTemplate extends BaseEntity {

    private String templateCode;

    private String templateName;

    private String businessType;

    private String description;

    private String status;
}
