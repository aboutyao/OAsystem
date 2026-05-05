package com.company.oa.entity.perm;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("perm_field_permission")
public class PermFieldPermission {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long roleId;

    private String businessType;

    private String fieldCode;

    private Short visible;

    private Short editable;

    private Short required;

    private Short masked;
}
