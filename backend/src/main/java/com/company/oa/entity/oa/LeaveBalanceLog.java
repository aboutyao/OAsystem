package com.company.oa.entity.oa;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("leave_balance_log")
public class LeaveBalanceLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String leaveType;
    private Integer year;
    private String changeType;
    private Double days;
    private Long relatedLeaveId;
    private String remark;
    private Long operatorId;
    private LocalDateTime createdAt;
    private Integer deleted;
}
