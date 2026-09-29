# Acceptance notes — scheduled-book-backup

Date: 2026-09-29

## Config

```yaml
financial-cloud:
  backup:
    schedule:
      enabled: false   # default off
      cron: "0 0 2 * * ?"
      directory: ./data/book-backups
      retain-count: 7
```

Enable on deploy by setting `enabled: true` and ensuring the directory is writable.

## Code-path verification

| Item | Result | Evidence |
|------|--------|----------|
| System export | PASS | `BookBackupService.exportForSystem` + unit test |
| Retain prune | PASS | `ScheduledBookBackupServiceTest.prune_keepsNewestRetainCount` |
| Cycle write | PASS | `runCycle_writesZipAndUpdatesLastRun` |
| Cron gate | PASS | `ScheduledBookBackupJob` `@ConditionalOnProperty(...enabled=true)` |
| Admin API | PASS | `GET/POST /api/book/backup/schedule/*` + `ProductRoles.requireAdministrator` |
| UI | PASS | books index schedule alert + 「立即定时备份」 |
| Docs | PASS | 01 / overview / gap / roadmap / backlog |

## Manual smoke (recommended)

1. Set `enabled: true`, restart, wait for cron or click 「立即定时备份」
2. Confirm ZIPs under configured directory; restore one via existing 恢复备份
3. Confirm retain-count deletes older files after multiple runs
