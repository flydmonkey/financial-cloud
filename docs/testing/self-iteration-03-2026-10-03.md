# 自主迭代第 3 轮：薪资有效凭证查重

日期：2026-10-03。OpenSpec：`payroll-live-voucher-dedupe`；路线见 [产品迭代路径](../product/23-self-iteration-roadmap.md)。验证对象为 `4571047d0b836848190e8952785e8590cee606d9` 上的未提交工作区；本轮未推送或部署。源码、原 Mapper、原始日志、Surefire 汇总和 CI 记录摘要保存于 `.e2e-run/iteration3-evidence.json`。

## 问题与修复

原生成逻辑在当前行存在失效关联时不检查其他行；历史查询又仅取最新裸关联，可能让最新失效引用遮住更早有效凭证。这是固定状态下可触发的重复业务凭证风险，本轮未查询现场账套、未认定生产已发生重复记账。

两类历史查询现在关联未删除的凭证，筛选有效性后再选择记录。当前行为空或失效时都会检查有效历史关联；命中后拒绝，不清理链接、不保存凭证。没有有效关联时，仅清理当前行自身对应失效链接并继续既有生成流程。

工资侧同账套、同员工、同所属月和原关联类型范围保留；历史软删工资仍参与查重，不按凭证状态或日期另行收窄。沿用原按凭证 ID 判断有效性的口径，未新增凭证 book 过滤。无需表结构或数据迁移，没有批量修改历史记录。

## 真实验证

| 检查 | 实际结果 | 证据 |
|---|---|---|
| 原 Mapper 注解 SQL 的 MySQL 反证 | 15 项方法中出现 6 个预期失败子场景：两类查询均错误选择最新失效引用，并返回不存在/已删除的凭证引用 | `.e2e-run/iteration3-sql-before.log` |
| 修复后的同一 SQL/边界套件 | **15 项通过，0 失败/错误/跳过**：7 项离线提取/配置保护，8 组实际 MySQL 查询 | `.e2e-run/iteration3-sql-after.log` |
| `mvn -o test`，financial-cloud 目录 | **545 项通过，0 失败/错误/跳过**，含新增 28 个服务参数化场景 | `.e2e-run/iteration3-backend-tests.log`、`financial-cloud/target/surefire-reports/` |
| `python tools/test_payroll_live_voucher_sql.py` | 7 项离线边界通过；该默认命令不连接数据库 | 主会话命令输出 |
| `openspec validate payroll-live-voucher-dedupe --strict` | 通过 | 主会话命令输出 |
| `git diff --check` | 通过，保留仓库既有 LF/CRLF 提示 | 主会话命令输出 |

实际 MySQL 使用已指定隔离库 `financial_cloud_e2e_20261002_iteration1`、端口 3307；本轮读取原注解 SQL，在同一连接内创建 `employee_salary` 和 `voucher` 临时表，夹具 INSERT/DELETE 只作用于临时表，关闭连接后丢弃。没有启动服务、清理真实账套或改动真实业务表。原 Mapper 字节先行保存，SHA-256 为 `0fb42901da65b4852cdad89869b2dee05a0d7ba7167ed8a093fab6b4e1acf101`，修复前后使用相同夹具与断言。

服务回归证明 own stale + live peer 的拒绝且无清理/保存、own live 快速拒绝、空关联查历史、只有 stale 时仅清当前关联并到达原模板流程。查询回归覆盖最新失效、缺失/软删凭证、软删工资历史、业务范围/类型隔离及原凭证状态/日期口径。直接执行注解 SQL 检验真实 MySQL 关系语义；不等同完整 MyBatis 参数绑定或业务 E2E 验收。本轮未重跑完整会计/账套隔离及工资 API E2E，未构建本轮发布 JAR。

## 独立审查与状态

独立审查见 [第 3 轮审查记录](self-iteration-03-review.md)：限定范围内无未解决阻塞问题，四个最终源文件哈希与本机证据快照一致。6/6 实施任务完成，新增 1 条要求及 5 个场景已同步薪资主规格，全部 28 项主规格严格校验通过；既有要求均保留。变更已归档至 `openspec/changes/archive/2026-10-03-payroll-live-voucher-dedupe/`。

2026-10-03 另已从 GitHub 原始运行与 job API 核实第一轮提交 `4571047` 的完整 [CI #157](https://github.com/flydmonkey/financial-cloud/actions/runs/37029890945)：backend-test、frontend-check 和完整 e2e 均成功；push 下 PR 专用 e2e-smoke 按配置跳过。原始副本位于 `.e2e-run/iteration3-baseline-ci-run.json` 与 `iteration3-baseline-ci-jobs.json`。该结果不覆盖第二、第三轮未提交改动，也不证明生产前端运行绑定。

剩余 R0 风险是生成与修改/删除交错、两个生成请求并发及关联写入失败的原子保护。静态审计已记录可验证交错，但本轮未执行并发复现，不能宣称已关闭。对应候选完整远端 CI、干净源码/生产产物运行绑定、完整会计及隔离重验、真实月份三方签字、问题台账复核和独立实例恢复仍待完成；正式发布依赖相应证据和授权。
