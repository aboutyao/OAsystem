package com.company.oa.entity.oa;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@TableName("oa_purchase_item")
public class OaPurchaseItem implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long purchaseId;

    private String itemName;

    private String specification;

    private String unit;

    private BigDecimal quantity;

    private BigDecimal unitPrice;

    private BigDecimal amount;

    private Integer sortOrder;

    private String remark;
}
