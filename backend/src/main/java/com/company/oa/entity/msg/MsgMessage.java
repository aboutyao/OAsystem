package com.company.oa.entity.msg;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("msg_message")
public class MsgMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long receiverId;

    private String messageType;

    private String title;

    private String content;

    private String businessType;

    private Long businessId;

    private Long wfInstanceId;

    private String readStatus;

    private String archiveStatus;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private LocalDateTime readAt;
}