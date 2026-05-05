package com.company.oa.entity.perm;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("perm_role_button")
public class PermRoleButton {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long roleId;

    private Long buttonId;
}
