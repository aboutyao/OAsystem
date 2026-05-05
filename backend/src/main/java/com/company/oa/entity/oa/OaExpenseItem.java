package com.company.oa.entity.oa;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("oa_expense_item")
public class OaExpenseItem implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long expenseId;

    private String feeType;

    private LocalDate feeDate;

    private BigDecimal amount;

    private String description;

    private Integer sortOrder;
}
