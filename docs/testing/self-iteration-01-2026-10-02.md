# 自主迭代第 1 轮：工资明细与凭证关联完整性

日期：2026-10-02。路线见 [产品迭代路径](../product/23-self-iteration-roadmap.md)，本轮 OpenSpec 为 `payroll-voucher-link-integrity`。基线 `9758a9a`，验证对象为其上的未提交工作区改动；本轮未推送、未正式部署。

2026-10-03 状态补记：修复随后已提交为 `4571047d0b836848190e8952785e8590cee606d9` 并归档；已通过 GitHub 原始运行和 job 记录实查完整 [CI #157](https://github.com/flydmonkey/financial-cloud/actions/runs/37029890945)，backend-test、frontend-check 和完整 e2e 均成功。push 事件下 PR 专用 e2e-smoke 按配置跳过。首轮本地原证据保留采集时的工作区身份；此 CI 不代表后来第二、第三轮未提交改动已通过远端验证。

## 问题与结果

确认并修复三条账务脱节路径：普通编辑把工资凭证关联更新为 null；直接删除明细不检查关联；工资入口在底层凭证删除失败时仍清空关联并返回成功。

实现结果：任一计提/发放关联存在即拒绝普通修改与明细删除，混合删除批次完整检查并整批拒绝；工资凭证删除失败返回原原因、保留关联并标记事务回滚，成功时仅解除请求类型的原关联；解除失败抛业务异常回滚。无关联明细及可删除草稿仍能按原流程处理。现有账套归属和角色校验保持生效。

普通无关联工资编辑仍不自动重算个税，需用预览、重算和推送流程；本轮不改累计个税历史级联。既有生成逻辑对失效链接的修复策略没有改动，不能把本轮表述为已全面解决所有历史失效关联。

## 本轮实际验证

| 检查 | 结果 | 证据 |
|---|---|---|
| 针对性工资服务/规则测试 | 41 通过，0 失败/错误/跳过；其中新增服务测试 30 | `financial-cloud/target/surefire-reports/TEST-com.financial.cloud.service.hr.EmployeeSalaryServiceTest.xml` |
| 全部后端单元测试 `mvn -o test` | 517 通过，0 失败/错误/跳过 | `.e2e-run/iteration1-backend-tests.log` |
| 独立输出目录后端构建 | BUILD SUCCESS | `.e2e-run/iteration1-backend-package.log` |
| 工资真实 API 端到端 | 1 个完整场景通过，0 失败/跳过/重试；约 8.9 秒总耗时 | `.e2e-run/iteration1-payroll.json`、`.e2e-run/iteration1-payroll.log` |
| 独立代码审查 | 无本轮范围内未解决问题；发现的失败半提交问题已修复 | 服务与新测试复查，失败返回标记 rollback-only |
| 修改 E2E 的范围 ESLint/测试发现 | 通过 | `npx eslint e2e/payroll-smb-regression.spec.ts --no-ignore`；Playwright `--list` |
| OpenSpec 严格检查及工作区差异检查 | 本轮变更及27项主规格通过，任务7/7完成，21个新文档本地链接无缺失，差异检查通过 | 变更 `validate --strict`；主规格 `validate --specs --strict`；`instructions apply`；`git diff --check` |

事务测试通过真实 Spring 事务拦截器与记录型事务管理器验证失败回滚、成功提交；它不是实际 JDBC 故障注入。API 回归实际访问新构建后端并比较持久化工资、凭证、分录和科目余额。

工资端到端覆盖：固定自定义基数/工资金额、预览推送、两类草稿分别删除后重生成、仅所选关联清空、另一关联和凭证保持不变、真实审核过账、修改/单删/混批删/两类凭证删除拒绝、每步数据不变、重复生成拒绝、无关联明细正常删除、银行支付文件内容。不能以 1 个场景宣称所有工资分支或完整会计验收已复跑。

本轮没有改前端业务代码，未重跑完整前端构建、前端全量测试、50 项会计验收或25项账套隔离。额外对 E2E 入口运行严格 TypeScript 检查发现既有 `helpers/reports.ts` 与 `helpers/voucher.ts` 类型问题，范围 ESLint和正常 Playwright 运行通过，未将该额外检查记为通过；相关 helper 不在本轮修改范围。

## 隔离环境与复现

新建库 `financial_cloud_e2e_20261002_iteration1`，创建前检查不存在，拒绝重置已存在数据库。MySQL 为本机 WSL 的 `192.168.137.121:3307`，服务为同机 `2264`，自动备份关闭，未操作原业务数据库。Windows Java 的回环连接报错后使用 WSL OpenJDK21 启动同一构建产物；健康检查 UP 后运行 API 回归。

```powershell
# 后端单元测试（financial-cloud 目录）
mvn -o test

# 工资 API 回归（financial-cloud-ui 目录；仅指定隔离库及对应服务）
$env:FC_DB_HOST='192.168.137.121'
$env:FC_DB_PORT='3307'
$env:FC_DB_NAME='financial_cloud_e2e_20261002_iteration1'
$env:E2E_API_URL='http://192.168.137.121:2264'
$env:E2E_BASE_URL='http://192.168.137.121:2264'
$env:E2E_RESET_BOOK='1'
$env:CI='1'
npx playwright test e2e/payroll-smb-regression.spec.ts --retries=0
```

该工资回归会清理指定测试库账套，复现前须核对服务确实连接同一隔离库。本轮 API 测试直接以后台服务为基址，没有启动前端页面服务。验证完成后已核对PID797220的产物、端口和隔离库连接并停止本轮测试服务，测试库与证据保留；重跑前需重新启动同一隔离配置。

本机证据快照 `.e2e-run/iteration1-evidence.json` 保存基线完整 SHA、服务/测试源码及后端包的 SHA256、实际 XML/Playwright统计和待验收状态。后端包 SHA256 为 `4fa4810fb4dad4bcf55deb3af109480db2300d37e022777c9aa995a1f9025249`。它记录已测工作区，尚未绑定未来提交、部署产物和实际远端 CI。

## 下一切片与待验收

R0-001 已验证，R0-002 候选版本证据核验是下一优先切片：关联提交、构建产物、环境、实际 CI、会计签字及恢复结果；缺少或旧版本证据不得判为可发布。并发生成/更正的完整串行化与既有失效链接修复策略列入后续风险审计，不在本轮宣称完成。

真实完整月份试账、会计签字、实际第二服务器恢复、候选版远端 CI及正式发布继续待完成，操作材料沿用 [平行试账](accountant-month-pilot.md) 与 [便携备份](portable-book-backup.md)。这些依赖需要负责人/授权资料及明确目标环境；等待期间可以继续独立证据核验与已复现风险修复。
