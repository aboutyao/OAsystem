package com.company.oa.entity.perm;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("perm_data_scope")
public class PermDataScope {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long roleId;

    private String scopeType;

    private String businessType;

    private LocalDateTime createdAt;
}
