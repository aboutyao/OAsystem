package com.company.oa.entity.perm;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("perm_menu")
public class PermMenu {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;

    private String menuCode;

    private String menuName;

    private String routePath;

    private String component;

    private String icon;

    private Integer sortOrder;

    private Short visible;

    private String status;
}
