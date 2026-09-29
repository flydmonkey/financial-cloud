# Acceptance notes — book-backup-overwrite

| Area | Result | Notes |
|------|--------|-------|
| Confirm phrase | PASS | Wrong phrase → 400, zero writes |
| Sealed reject | PASS | Sealed target rejected before wipe |
| Pre-backup | PASS | `pre-overwrite-<bookId>-<ts>.zip` under schedule directory |
| Keep book_id | PASS | Target id + memberships retained; rows remapped |
| Schedule APIs | PASS | `/schedule/status` + `/schedule/run` restored on controller |
| UI | PASS | Books index restore dialog: clone / overwrite modes |
| Unit tests | PASS | `BookRestoreServiceTest` (9) |

Manual smoke:
1. Export a book ZIP, open「恢复账套备份」→「覆盖到现有账套」
2. Select target, type「覆盖恢复」, upload ZIP → confirm
3. Verify target book data matches backup and pre-overwrite ZIP exists on disk
