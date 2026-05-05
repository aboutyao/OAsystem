package com.company.oa.entity.notice;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("oa_notice_read")
public class OaNoticeRead {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long noticeId;

    private Long userId;

    private LocalDateTime readAt;

    private Integer confirmed;
}
