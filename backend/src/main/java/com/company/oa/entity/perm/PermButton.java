package com.company.oa.entity.perm;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("perm_button")
public class PermButton {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long menuId;

    private String buttonCode;

    private String buttonName;

    private String permissionCode;

    private String status;
}
