package com.company.oa.entity.asset;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("asset_record")
public class AssetRecord {

    @TableId(type = IdType.INPUT)
    private Long id;

    private Long assetId;

    private String recordType;

    private Long fromUserId;

    private Long toUserId;

    private String reason;

    private Long operatedBy;

    private LocalDateTime operatedAt;
}
