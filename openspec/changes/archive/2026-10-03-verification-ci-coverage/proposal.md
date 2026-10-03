## Why

第二至第四轮新增的证据核验工具、SQL 边界及真实薪资事务 IT 尚未进入 CI，现有绿色流水线可能完全没有执行这些检查。R0-009 先补齐自动验证范围和可复核报告，防止把旧提交或不完整检查的成功当作新候选证据。

## What Changes

- 在 PR→main 与 push→main 均执行 `verification-tools`：复用当前 126 项证据核验、运行器和来源测试，以及 13 项 SQL 离线边界；失败、跳过或零执行不记通过。
- 新增独立 `payroll-mysql-it` job，显式执行 `PayrollTransactionMysqlIT`，保持普通后端单测无数据库依赖；使用固定 MySQL 9.4.0 镜像并观察实际版本，不将其他版本兼容性视作已验证。
- 新增受控 DDL-only 初始化入口：只允许全新专用测试库，创建前验证环境、来源及语句计划；拒绝已有库，禁止清库和安装种子，不调用 `run_init_sql.py`。
- 保存实际数据库版本、默认隔离级别、生产范围索引及观察权限，绑定 checkout 的完整 SHA、CI run/attempt、报告和源码哈希；PR 源分支 SHA 与实际测试合并 SHA 分别记录。
- 成功或失败均保留原始报告、日志及元数据并上传；更新可信 `release_evidence_profile.json` 的必需 job/step 身份及相关 fixture。

## Capabilities

### New Capabilities

- `verification-ci-coverage`: 两类事件上的必需工具与真实事务检查、全新测试库初始化边界、实际运行证据及可信 CI 范围同步。

### Modified Capabilities

- 无。`release-candidate-evidence` 的版本匹配和完整 CI 检查契约保持适用，仅扩充其实现中的可信 profile；薪资业务行为和现有 IT 断言不变。

## Impact

- 预计涉及 `.github/workflows/ci.yml`、新增受控初始化/报告工具及其测试、`tools/release_evidence_profile.json`、相关 fixture 和验收说明。仅新增独立验证 job，不修改现有 `docker-compose.yml` 的 MySQL 9.7 或既有会计 E2E 初始化方式。
- Java 21、Python、PyMySQL 和固定 `mysql:9.4.0-oraclelinux9` 为实施依赖。镜像标签已由主会话查官方列表；实际拉取、启动及版本仍须验证，失败不得静默替换版本。
- 本片不采集生产运行身份，不触发真实会计签字或第二实例恢复，不提交、推送、部署。实现交付可通过本地工具和受控隔离集成验证完成；新候选实际远端 CI 仍为后续待办，既有 `4571047` 的 CI 不覆盖当前未提交代码。
