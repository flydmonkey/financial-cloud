## Why

克隆式恢复已满足「拷贝出新账套」；代账交接/误操作回滚时常需要把备份灌回**指定现有账套**。此前明确 Non-goal，现以强确认 + 自动预备份补齐，避免静默覆盖。

## What Changes

- 新增 `POST /api/book/backup/restore-overwrite`：覆盖指定账套业务数据。
- 安全门槛：账套管理员；确认短语必须为 `覆盖恢复`；封存账套拒绝；覆盖前自动落盘预备份 ZIP。
- 同一事务：按备份表规格逆序清空目标账套业务表 → 按克隆同款 ID 重映射灌入（保留原 `book_id` 与成员授权）。
- 前端恢复对话框增加「覆盖到现有账套」模式。
- 补齐此前遗漏的定时备份 status/run Controller 端点。
- 更新 `book-backup` 规格与产品文档。

## Capabilities

### Modified Capabilities

- `book-backup`: 增加覆盖式恢复要求（含确认短语、预备份、事务语义）

## Impact

- `BookRestoreService` / `BookBackupController` / books UI / docs / tests
- 复用 `BookBackupService.export` 与 `BackupTableRegistry`
