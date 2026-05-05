package com.company.oa.entity.meeting;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.oa.common.entity.VersionedEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("meeting_room")
public class MeetingRoom extends VersionedEntity {

    private String roomName;

    private String location;

    private Integer capacity;

    private String equipment;

    private String status;

    private String remark;
}