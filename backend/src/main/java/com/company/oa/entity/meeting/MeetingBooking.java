package com.company.oa.entity.meeting;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.oa.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("meeting_booking")
public class MeetingBooking extends BaseEntity {

    private Long roomId;

    private String title;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    private Long organizerId;

    private Integer participantCount;

    private String attendees;

    private String status;

    private String cancelReason;

    private Long createdBy;

    private String createdNameSnapshot;
}
