## Context

Manual book backup/restore already ships (`BookBackupService` / `BookRestoreService`). `@EnableScheduling` is on. Prior Non-goal: no scheduled backup. This change adds local scheduled persistence only.

## Goals / Non-Goals

**Goals:**
- Config-driven cron exporting all enabled books (`status=1`, not deleted) to a local directory.
- Per-book retain-N cleanup.
- Admin status + run-now API; light UI hint on books page.
- Reuse identical ZIP format for restore compatibility.

**Non-Goals:**
- Object storage / S3 / NAS sync / email alerts.
- Overwrite-in-place restore.
- Per-book custom cron.
- Changing restore semantics.

## Decisions

1. **System export path** — Add `BookBackupService.exportForSystem(bookId)` that skips `requireBookAdministrator` but reuses private ZIP build + audits with a synthetic system operator id (`scheduled-backup`). Alternative: fake admin UserInfo — rejected (permission coupling).

2. **Config under `financial-cloud.backup.schedule`** — `enabled` (default false), `cron` (default `0 0 2 * * ?`), `directory` (default `./data/book-backups`), `retain-count` (default 7). Fail-soft if directory missing: create on first run.

3. **Scheduler bean** — `ScheduledBookBackupJob` with `@Scheduled(cron=...)` gated by enabled; synchronized so run-now and cron do not overlap.

4. **File naming** — `scheduled-<safeBookName>-<bookId>-<yyyyMMdd-HHmmss>.zip` so retain prune can filter by bookId segment.

5. **Auth for status/run-now** — Require platform/instance admin (same pattern as other system ops); not every book admin.

6. **UI** — Compact card/alert on books index: show enabled + last run; button「立即备份全部」when admin.

## Risks / Trade-offs

- **[Risk] Disk full** → Mitigation: retain-N; log failures; do not crash JVM.
- **[Risk] Long export blocks** → Mitigation: sequential per book; single-flight lock; log duration.
- **[Risk] Accidental enable in prod without disk** → Default `enabled=false`.

## Migration Plan

1. Deploy with `enabled=false`.
2. Ops set directory + enable + restart (or refresh if `@ConfigurationProperties` + `@Scheduled` cron from property).
3. Rollback: set `enabled=false` / remove job bean via config; leftover ZIPs harmless.
