package com.company.oa.entity.perm;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("perm_data_scope_dept")
public class PermDataScopeDept {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long dataScopeId;

    private Long deptId;
}
