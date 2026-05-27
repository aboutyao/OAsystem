package com.company.oa.workflow;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.oa.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_comment_template")
public class CommentTemplate extends BaseEntity {
    private Long userId;
    private String content;
    private Integer sortOrder;
}
