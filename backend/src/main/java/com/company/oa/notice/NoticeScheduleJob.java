package com.company.oa.notice;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.company.oa.entity.notice.OaNotice;
import com.company.oa.notice.mapper.OaNoticeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class NoticeScheduleJob {
    private static final Logger log = LoggerFactory.getLogger(NoticeScheduleJob.class);
    private final OaNoticeMapper noticeMapper;

    public NoticeScheduleJob(OaNoticeMapper noticeMapper) {
        this.noticeMapper = noticeMapper;
    }

    @Scheduled(fixedRate = 60000) // every minute
    public void publishScheduledNotices() {
        LocalDateTime now = LocalDateTime.now();
        var scheduledNotices = noticeMapper.selectList(
                new LambdaQueryWrapper<OaNotice>()
                        .eq(OaNotice::getStatus, "SCHEDULED")
                        .isNotNull(OaNotice::getScheduledAt)
                        .le(OaNotice::getScheduledAt, now)
        );

        for (OaNotice notice : scheduledNotices) {
            try {
                noticeMapper.update(null,
                        new LambdaUpdateWrapper<OaNotice>()
                                .eq(OaNotice::getId, notice.getId())
                                .set(OaNotice::getStatus, "PUBLISHED")
                                .set(OaNotice::getPublishAt, now)
                                .set(OaNotice::getUpdatedAt, now)
                                .setSql("version = version + 1"));
                log.info("Published scheduled notice: {}", notice.getId());
            } catch (Exception e) {
                log.error("Failed to publish scheduled notice: {}", notice.getId(), e);
            }
        }
    }
}
