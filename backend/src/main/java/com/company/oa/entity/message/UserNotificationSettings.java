package com.company.oa.entity.message;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.oa.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_notification_settings")
public class UserNotificationSettings extends BaseEntity {
    private Long userId;
    private Boolean enableEmail;
    private Boolean enableSse;
    private Boolean enableDnd;
    private String dndStart;
    private String dndEnd;
}
