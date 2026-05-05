package com.company.oa.entity.notice;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.oa.common.entity.VersionedEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_notice")
public class OaNotice extends VersionedEntity {

    private String noticeNo;

    private String title;

    private String content;

    private String category;

    private String noticeType;

    private String publishScopeType;

    private Integer topFlag;

    private LocalDateTime topUntil;

    private String status;

    private LocalDateTime publishAt;

    private LocalDateTime withdrawAt;

    private LocalDateTime expireAt;

    private Long createdBy;

    private String createdNameSnapshot;
}