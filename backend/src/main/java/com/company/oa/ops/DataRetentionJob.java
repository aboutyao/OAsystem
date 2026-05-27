package com.company.oa.ops;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.oa.audit.mapper.AuditLoginLogMapper;
import com.company.oa.audit.mapper.AuditOperationLogMapper;
import com.company.oa.entity.audit.AuditLoginLog;
import com.company.oa.entity.audit.AuditOperationLog;
import com.company.oa.entity.ops.AppExceptionLog;
import com.company.oa.ops.mapper.AppExceptionLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataRetentionJob {

    private static final Logger log = LoggerFactory.getLogger(DataRetentionJob.class);

    private final AuditLoginLogMapper loginLogMapper;
    private final AuditOperationLogMapper operationLogMapper;
    private final AppExceptionLogMapper exceptionLogMapper;

    public DataRetentionJob(AuditLoginLogMapper loginLogMapper,
                            AuditOperationLogMapper operationLogMapper,
                            AppExceptionLogMapper exceptionLogMapper) {
        this.loginLogMapper = loginLogMapper;
        this.operationLogMapper = operationLogMapper;
        this.exceptionLogMapper = exceptionLogMapper;
    }

    @Scheduled(cron = "0 0 3 * * *") // every day at 3am
    public void cleanupOldRecords() {
        log.info("Starting data retention cleanup...");
        LocalDateTime cutoff = LocalDateTime.now().minusDays(90); // keep 90 days

        // Clean login logs older than 90 days
        int loginDeleted = loginLogMapper.delete(
            new LambdaQueryWrapper<AuditLoginLog>()
                .lt(AuditLoginLog::getLoggedAt, cutoff));
        log.info("Deleted {} old login logs", loginDeleted);

        // Clean operation logs older than 90 days
        int opDeleted = operationLogMapper.delete(
            new LambdaQueryWrapper<AuditOperationLog>()
                .lt(AuditOperationLog::getOperatedAt, cutoff));
        log.info("Deleted {} old operation logs", opDeleted);

        // Clean exception logs older than 30 days
        LocalDateTime exceptionCutoff = LocalDateTime.now().minusDays(30);
        int exDeleted = exceptionLogMapper.delete(
            new LambdaQueryWrapper<AppExceptionLog>()
                .lt(AppExceptionLog::getOccurredAt, exceptionCutoff));
        log.info("Deleted {} old exception logs", exDeleted);

        log.info("Data retention cleanup completed");
    }
}
