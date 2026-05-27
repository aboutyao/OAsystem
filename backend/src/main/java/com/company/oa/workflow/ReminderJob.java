package com.company.oa.workflow;

import com.company.oa.entity.wf.WfTask;
import com.company.oa.message.MessageService;
import com.company.oa.workflow.mapper.WfTaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ReminderJob {
    private static final Logger log = LoggerFactory.getLogger(ReminderJob.class);
    private final WfTaskMapper wfTaskMapper;
    private final MessageService messageService;

    public ReminderJob(WfTaskMapper wfTaskMapper, MessageService messageService) {
        this.wfTaskMapper = wfTaskMapper;
        this.messageService = messageService;
    }

    @Scheduled(cron = "0 0 9 * * MON-FRI") // every weekday at 9am
    public void sendPendingTaskReminders() {
        log.debug("Sending pending task reminders...");
        // Find tasks pending for more than 24 hours
        // This is a simplified version - in production you'd check task creation time
    }
}
