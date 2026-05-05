package com.company.oa.entity.asset;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.oa.common.entity.VersionedEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("asset_supply")
public class AssetSupply extends VersionedEntity {

    private String supplyCode;

    private String supplyName;

    private String category;

    private String unit;

    private BigDecimal stockQuantity;

    private BigDecimal warningQuantity;

    private String status;

    private String remark;
}