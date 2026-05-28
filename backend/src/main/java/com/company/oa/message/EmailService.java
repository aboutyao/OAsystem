package com.company.oa.message;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.oa.entity.message.UserNotificationSettings;
import com.company.oa.message.mapper.UserNotificationSettingsMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender mailSender;
    private final UserNotificationSettingsMapper settingsMapper;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    public EmailService(JavaMailSender mailSender, UserNotificationSettingsMapper settingsMapper) {
        this.mailSender = mailSender;
        this.settingsMapper = settingsMapper;
    }

    /**
     * Send approval notification email without DND check (legacy).
     */
    @Async
    public void sendApprovalNotification(String toEmail, String subject, String body) {
        send(toEmail, "[OA系统] " + subject, body);
    }

    /**
     * Send approval notification email, respecting the recipient's DND settings.
     * If DND is active for the given userId, the email is skipped.
     */
    @Async
    public void sendApprovalNotification(long userId, String toEmail, String subject, String body) {
        if (isDndActive(userId)) {
            log.debug("Email skipped (DND active) for userId={}: {}", userId, subject);
            return;
        }
        send(toEmail, "[OA系统] " + subject, body);
    }

    /**
     * Checks whether the given user has DND enabled and the current time
     * falls within the configured quiet-time window.
     */
    private boolean isDndActive(long userId) {
        var settings = settingsMapper.selectOne(
                new LambdaQueryWrapper<UserNotificationSettings>()
                        .eq(UserNotificationSettings::getUserId, userId)
        );
        if (settings == null || !Boolean.TRUE.equals(settings.getEnableDnd())) {
            return false;
        }

        String startStr = settings.getDndStart();
        String endStr = settings.getDndEnd();
        if (startStr == null || endStr == null || startStr.isBlank() || endStr.isBlank()) {
            return false;
        }

        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
            LocalTime start = LocalTime.parse(startStr.trim(), fmt);
            LocalTime end = LocalTime.parse(endStr.trim(), fmt);
            LocalTime now = LocalTime.now();

            if (start.isBefore(end) || start.equals(end)) {
                return !now.isBefore(start) && !now.isAfter(end);
            } else {
                return !now.isBefore(start) || !now.isAfter(end);
            }
        } catch (Exception e) {
            return false;
        }
    }

    @Async
    public void send(String toEmail, String subject, String body) {
        if (toEmail == null || toEmail.isBlank() || fromAddress == null || fromAddress.isBlank()) {
            log.debug("Email skipped - no recipient or sender configured: {}", subject);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent to {}: {}", toEmail, subject);
        } catch (Exception e) {
            log.warn("Failed to send email to {}: {}", toEmail, e.getMessage());
        }
    }
}
