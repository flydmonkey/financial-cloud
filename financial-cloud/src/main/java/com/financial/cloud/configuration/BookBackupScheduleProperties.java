package com.financial.cloud.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 定时账套备份配置。默认关闭，需运维显式开启。
 */
@Data
@Component
@ConfigurationProperties(prefix = "financial-cloud.backup.schedule")
public class BookBackupScheduleProperties {

    /** 是否启用定时备份 */
    private boolean enabled = false;

    /** Spring cron，默认每天 02:00 */
    private String cron = "0 0 2 * * ?";

    /** 备份 ZIP 落盘目录（相对路径相对进程工作目录） */
    private String directory = "./data/book-backups";

    /** 每个账套保留的最近定时备份份数 */
    private int retainCount = 7;
}
