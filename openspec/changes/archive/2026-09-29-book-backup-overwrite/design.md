## Context

Clone restore already validates ZIP and remaps IDs into a new book. Overwrite reuses validation/remap; differs by wipe + keep book_id.

## Goals / Non-Goals

**Goals:** Safe overwrite path with confirm phrase `覆盖恢复`, admin check, sealed reject, disk pre-backup, transactional wipe+insert.

**Non-Goals:** Partial table restore; overwriting permissions/users; auto-download of pre-backup to browser.

## Decisions

1. Confirm phrase exact match: `覆盖恢复` (Chinese, product-facing).
2. Pre-backup via `BookBackupService.export` written under `financial-cloud.backup.schedule.directory` as `pre-overwrite-<bookId>-<ts>.zip` **before** opening the wipe transaction (file survives rollback).
3. Wipe: reverse `BackupTableRegistry.SPECS` order; BOOK_ID `DELETE WHERE book_id=?`; VIA_VOUCHER delete by voucher subquery.
4. Keep book row id; update shell metadata from manifest (name without forced `（备份恢复）` suffix).
5. Keep permission_book / role_member untouched.
6. Also wire missing schedule status/run endpoints into `BookBackupController`.

## Risks / Trade-offs

- **[Risk] Accidental overwrite** → Mitigation: phrase + admin + sealed guard + UI warnings.
- **[Risk] Wipe then crash before commit** → Mitigation: `@Transactional`; disk pre-backup for manual clone-restore.
- **[Risk] MySQL FK order** → Mitigation: reverse registry order.
