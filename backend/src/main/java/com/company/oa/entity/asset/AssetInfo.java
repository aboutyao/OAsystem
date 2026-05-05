package com.company.oa.entity.asset;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.company.oa.common.entity.VersionedEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("asset_info")
public class AssetInfo extends VersionedEntity {

    private String assetNo;

    private String assetName;

    private String assetCategory;

    private String model;

    private LocalDate purchaseDate;

    private BigDecimal purchaseAmount;

    private Long responsibleUserId;

    private Long deptId;

    @TableField("status")
    private String status;

    private String remark;

    @TableField(exist = false)
    private String responsibleUserName;

    @TableField(exist = false)
    private String deptName;
}