package com.company.oa.entity.oa;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("leave_balance")
public class LeaveBalance {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String leaveType;
    private Integer year;
    private Double totalDays;
    private Double usedDays;
    private Double pendingDays;
    private Double remainingDays;
    private Double carriedOverDays;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
