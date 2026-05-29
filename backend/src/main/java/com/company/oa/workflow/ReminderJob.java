package com.company.oa.workflow;

import com.company.oa.entity.wf.WfTask;
import com.company.oa.message.MessageService;
import com.company.oa.workflow.mapper.WfTaskMapper;
import com.company.oa.workflow.mapper.WfProcessInstanceMapper;
import com.company.oa.entity.wf.WfProcessInstance;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class ReminderJob {
    private static final Logger log = LoggerFactory.getLogger(ReminderJob.class);
    private final WfTaskMapper wfTaskMapper;
    private final WfProcessInstanceMapper instanceMapper;
    private final MessageService messageService;

    public ReminderJob(WfTaskMapper wfTaskMapper, WfProcessInstanceMapper instanceMapper, MessageService messageService) {
        this.wfTaskMapper = wfTaskMapper;
        this.instanceMapper = instanceMapper;
        this.messageService = messageService;
    }

    /**
     * 每天早上9点发送待办任务提醒
     * 提醒超过24小时未处理的待办任务
     */
    @Scheduled(cron = "0 0 9 * * MON-FRI")
    public void sendPendingTaskReminders() {
        log.info("开始发送待办任务提醒...");
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);

        List<WfTask> overdueTasks = wfTaskMapper.selectList(
            new LambdaQueryWrapper<WfTask>()
                .eq(WfTask::getStatus, "PENDING")
                .le(WfTask::getCreatedAt, threshold)
        );

        int reminderCount = 0;
        for (WfTask task : overdueTasks) {
            try {
                // 获取流程实例信息
                WfProcessInstance instance = instanceMapper.selectById(task.getWfInstanceId());
                String title = instance != null ? instance.getTitle() : "未知流程";

                // 计算等待时长
                long hoursPending = ChronoUnit.HOURS.between(task.getCreatedAt(), LocalDateTime.now());

                messageService.send(task.getAssigneeId(), "WORKFLOW",
                    "待办提醒: " + title,
                    "您有一条待办任务已等待 " + hoursPending + " 小时，请尽快处理。",
                    "WORKFLOW", null, task.getWfInstanceId());
                reminderCount++;
            } catch (Exception e) {
                log.error("发送待办提醒失败, taskId={}", task.getId(), e);
            }
        }
        log.info("待办任务提醒完成，共发送 {} 条提醒", reminderCount);
    }

    /**
     * 每天下午5点发送即将超时提醒
     * 提醒距离SLA截止还有2小时的任务
     */
    @Scheduled(cron = "0 0 17 * * MON-FRI")
    public void sendSlaWarningReminders() {
        log.info("开始发送SLA预警提醒...");
        LocalDateTime warningThreshold = LocalDateTime.now().plusHours(2);

        List<WfProcessInstance> approachingSla = instanceMapper.selectList(
            new LambdaQueryWrapper<WfProcessInstance>()
                .eq(WfProcessInstance::getStatus, "APPROVING")
                .eq(WfProcessInstance::getDeleted, 0)
                .le(WfProcessInstance::getSlaDeadline, warningThreshold)
                .gt(WfProcessInstance::getSlaDeadline, LocalDateTime.now())
                .eq(WfProcessInstance::isSlaBreached, false)
        );

        int warningCount = 0;
        for (WfProcessInstance instance : approachingSla) {
            try {
                messageService.send(instance.getStarterId(), "WORKFLOW",
                    "SLA预警: " + instance.getTitle(),
                    "您的「" + instance.getTitle() + "」距离SLA截止还有不到2小时，请关注。",
                    "WORKFLOW", null, instance.getId());
                warningCount++;
            } catch (Exception e) {
                log.error("发送SLA预警失败, instanceId={}", instance.getId(), e);
            }
        }
        log.info("SLA预警提醒完成，共发送 {} 条预警", warningCount);
    }
}
