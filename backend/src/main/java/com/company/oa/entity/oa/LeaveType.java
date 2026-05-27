package com.company.oa.entity.oa;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("leave_type")
public class LeaveType {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String typeCode;
    private String typeName;
    private Integer daysPerYear;
    private Boolean isPaid;
    private Boolean requiresProof;
    private Integer maxConsecutiveDays;
    private Double minLeaveHours;
    private Integer sortOrder;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
