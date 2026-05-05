package com.company.oa.entity.asset;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("asset_supply_record")
public class AssetSupplyRecord {

    @TableId(type = IdType.INPUT)
    private Long id;

    private Long supplyId;

    private String recordType;

    private BigDecimal quantity;

    private Long userId;

    private String reason;

    private Long operatedBy;

    private LocalDateTime operatedAt;
}
