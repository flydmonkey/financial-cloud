## Why

手动备份已落地，但代账场景仍依赖人工记得导出；主机故障或误删时若无人刚备份过即有数据风险。调度与本地落盘是 book-backup Non-goal v2 的最小交付，可复用现有 ZIP 格式与导出实现。

## What Changes

- 增加**配置驱动的定时账套备份**：按 cron 对启用中账套写出与手动导出同格式的 ZIP 到本机目录。
- 支持每账套保留最近 N 份，超限自动清理旧文件。
- 提供只读查询：最近一次调度结果 / 目录内文件列表（实例管理员）。
- 可选「立即跑一轮」管理接口（不改变 cron）。
- 文档与 gap/roadmap 将「定时备份」从未做改为已落地（覆盖式恢复仍不做）。

## Capabilities

### New Capabilities

- `scheduled-book-backup`: 定时将账套业务备份包落盘、保留策略、调度可见性与手动触发。

### Modified Capabilities

- `book-backup`: 补充「系统调度可导出同格式包」的能力边界（不改变手动导出/恢复语义）。

## Impact

- 后端：`@EnableScheduling` 已存在；新增 scheduler + properties；轻量扩展 `BookBackupService`（系统导出路径，绕过账套管理员校验）。
- 配置：`application.yml` 增加 `financial-cloud.backup.schedule.*`。
- 前端：账套管理页展示调度开关提示 + 最近运行摘要（极简）。
- 无新外部依赖；不引入对象存储 / 邮件。
