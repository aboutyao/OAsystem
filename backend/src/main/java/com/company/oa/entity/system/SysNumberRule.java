package com.company.oa.entity.system;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_number_rule")
public class SysNumberRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String ruleCode;

    private String businessType;

    private String prefix;

    private String datePattern;

    private Integer seqLength;

    private String seqReset;

    private String currentPeriod;

    private Long currentSeq;

    private String description;

    private String status;

    private Integer version;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
