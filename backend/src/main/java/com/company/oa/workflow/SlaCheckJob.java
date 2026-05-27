package com.company.oa.workflow;

import com.company.oa.common.service.SequenceService;
import com.company.oa.entity.wf.WfProcessInstance;
import com.company.oa.message.MessageService;
import com.company.oa.workflow.mapper.WfProcessInstanceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class SlaCheckJob {
    private static final Logger log = LoggerFactory.getLogger(SlaCheckJob.class);
    private final WfProcessInstanceMapper instanceMapper;
    private final MessageService messageService;

    public SlaCheckJob(WfProcessInstanceMapper instanceMapper, MessageService messageService) {
        this.instanceMapper = instanceMapper;
        this.messageService = messageService;
    }

    @Scheduled(fixedRate = 300000) // every 5 minutes
    public void checkSlaBreaches() {
        log.debug("Checking SLA breaches...");
        List<WfProcessInstance> breached = instanceMapper.selectList(
            new LambdaQueryWrapper<WfProcessInstance>()
                .eq(WfProcessInstance::getStatus, "APPROVING")
                .eq(WfProcessInstance::getDeleted, 0)
                .le(WfProcessInstance::getSlaDeadline, LocalDateTime.now())
                .eq(WfProcessInstance::isSlaBreached, false));

        for (WfProcessInstance instance : breached) {
            instanceMapper.markSlaBreach(instance.getId());
            // Notify the starter
            messageService.send(instance.getStarterId(), "WORKFLOW",
                "审批超时提醒: " + instance.getTitle(),
                "您的「" + instance.getTitle() + "」已超过SLA时限，请关注。",
                "WORKFLOW", null, instance.getId());
            log.info("SLA breach marked for instance {}", instance.getId());
        }
    }
}
