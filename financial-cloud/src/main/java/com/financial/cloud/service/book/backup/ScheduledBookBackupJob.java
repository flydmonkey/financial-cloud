package com.financial.cloud.service.book.backup;

import com.financial.cloud.configuration.BookBackupScheduleProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cron 触发定时账套备份。仅在 financial-cloud.backup.schedule.enabled=true 时注册。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "financial-cloud.backup.schedule", name = "enabled", havingValue = "true")
public class ScheduledBookBackupJob {

    private final ScheduledBookBackupService scheduledBookBackupService;
    private final BookBackupScheduleProperties properties;

    @Scheduled(cron = "${financial-cloud.backup.schedule.cron:0 0 2 * * ?}")
    public void onSchedule() {
        if (!properties.isEnabled()) {
            return;
        }
        scheduledBookBackupService.runCycle("cron");
    }
}
