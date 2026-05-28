package com.company.oa.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.oa.entity.contract.ContractInfo;
import com.company.oa.entity.org.User;
import com.company.oa.contract.mapper.ContractInfoMapper;
import com.company.oa.message.MessageService;
import com.company.oa.org.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 自动提醒定时任务
 * 检查合同到期等
 */
@Component
public class AutoReminderJob {
    private static final Logger log = LoggerFactory.getLogger(AutoReminderJob.class);
    private final ContractInfoMapper contractMapper;
    private final UserMapper userMapper;
    private final MessageService messageService;

    public AutoReminderJob(ContractInfoMapper contractMapper, UserMapper userMapper, MessageService messageService) {
        this.contractMapper = contractMapper;
        this.userMapper = userMapper;
        this.messageService = messageService;
    }

    @Scheduled(cron = "0 0 9 * * ?") // 每天早上9点执行
    public void checkContractExpiry() {
        log.debug("Checking contract expiry...");
        LocalDate now = LocalDate.now();
        LocalDate thirtyDaysLater = now.plusDays(30);

        List<ContractInfo> expiringContracts = contractMapper.selectList(
                new LambdaQueryWrapper<ContractInfo>()
                        .eq(ContractInfo::getDeleted, 0)
                        .in(ContractInfo::getStatus, "ACTIVE", "SIGNED")
                        .le(ContractInfo::getEndDate, thirtyDaysLater)
                        .ge(ContractInfo::getEndDate, now)
        );

        for (ContractInfo contract : expiringContracts) {
            long daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(now, contract.getEndDate());
            String message = "合同「" + contract.getContractName() + "」将在" + daysRemaining + "天后到期，请及时处理续签";

            // 通知所有管理员
            List<User> admins = userMapper.selectList(
                    new LambdaQueryWrapper<User>()
                            .eq(User::getAccountStatus, "ENABLED")
                            .eq(User::getDeleted, 0)
            );
            for (User admin : admins) {
                messageService.send(admin.getId(), "REMIND",
                        "合同到期提醒", message, "CONTRACT", contract.getId(), null);
            }
        }
        log.info("Contract expiry check completed, found {} expiring contracts", expiringContracts.size());
    }
}
