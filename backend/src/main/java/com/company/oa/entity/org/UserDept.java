package com.company.oa.entity.org;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("org_user_dept")
public class UserDept {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long deptId;

    private String relationType;

    private LocalDate startDate;

    private LocalDate endDate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}