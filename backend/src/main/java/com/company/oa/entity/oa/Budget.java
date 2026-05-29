package com.company.oa.entity.oa;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("oa_budget")
public class Budget {
    @TableId(type = IdType.INPUT)
    private Long id;
    private Long deptId;
    private String budgetType; // MONTHLY, QUARTERLY, YEARLY
    private Integer year;
    private Integer month;
    private Integer quarter;
    private String category; // EXPENSE, PURCHASE, TRAVEL, etc.
    private BigDecimal budgetAmount;
    private BigDecimal usedAmount;
    private BigDecimal warningThreshold; // 80% means warn at 80% usage
    private String status; // ACTIVE, INACTIVE
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
