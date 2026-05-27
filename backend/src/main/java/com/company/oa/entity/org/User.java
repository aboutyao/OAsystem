package com.company.oa.entity.org;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.company.oa.common.entity.SoftDeleteEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("org_user")
public class User extends SoftDeleteEntity {

    private String username;

    private String passwordHash;

    private String employeeNo;

    private String realName;

    private String mobile;

    private String email;

    private Long mainDeptId;

    private Long positionId;

    private Long rankId;

    private Long managerUserId;

    private String employeeStatus;

    private String accountStatus;

    private LocalDate entryDate;

    private LocalDate resignDate;

    private LocalDateTime lastLoginAt;

    private LocalDateTime passwordChangedAt;

    private LocalDateTime passwordExpiresAt;

    private Integer loginFailCount;

    private LocalDateTime lockedUntil;

    private String totpSecret;

    private Boolean totpEnabled;

    @TableField(exist = false)
    private String mainDeptName;

    @TableField(exist = false)
    private String positionName;

    @TableField(exist = false)
    private String rankName;
}