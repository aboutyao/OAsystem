package com.company.oa.entity.rule;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.oa.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("rule_version")
public class RuleVersion extends BaseEntity {

    private Long ruleId;

    private Integer versionNo;

    private String ruleContent;

    private String naturalLanguage;

    private String status;

    private LocalDateTime effectiveAt;

    private LocalDateTime expiredAt;

    private LocalDateTime publishedAt;

    private Long publishedBy;

    private String changeReason;
}
