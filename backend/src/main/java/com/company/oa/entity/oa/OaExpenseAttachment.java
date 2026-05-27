package com.company.oa.entity.oa;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("oa_expense_attachment")
public class OaExpenseAttachment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long expenseId;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String mimeType;
    private Long uploadedBy;
    private LocalDateTime createdAt;
    private Integer deleted;
}
