# 自主迭代第 5 轮独立审查：验证工具与薪资事务 CI 覆盖

审查日期：2026-10-03。审查者：独立代理 `release_risk_audit`，未参与实现。审查仅写本文件；未修改实现、测试、证据、规格、任务或其他文档，未运行测试、Maven、数据库、服务、OpenSpec、网络或提交。已读 `verification-ci-coverage` 全部规划上下文、冻结代码及本次实际原始产物；另由独立只读复核确认最终时间顺序及 XML 属性绑定。

## 结论

**本切片审查通过，未发现未解决的阻塞问题，符合 `verification-ci-coverage` 的规格及限定交付范围。** 两个必需 job 已接入 PR→main 与 push→main；受控初始化、运行来源、完整用例结果、失败证据保留及可信 profile v2 的范围一致。审查发现的三个证据校验缺口均已关闭，最终冻结实现具有 220 项离线检查及 21 项真实 MySQL IT 的本地验证证据。

本结论允许配置与工具实现切片完成。当前是未提交工作区的本地证据，`execution=local`、`remoteRunObserved=false`，没有本次候选的远端 CI 运行结果。未提交、推送、部署或操作生产库；生产运行身份与数据源绑定、真实会计签字和独立实例恢复继续待补，不据此认定发布就绪。

## 三个阻塞的关闭

1. **历史 XML 通过 hash/mtime 重新绑定已关闭。** 原方案允许将缺少本次执行身份的历史绿色 XML 复制到新路径，或只修改空白形成新 hash 后被当成本次结果。当前 `verify_payroll_ci_reports.py:130` 的 prepare 生成随机 nonce，并记录完整 checkout SHA 和源码指纹；实际 Maven 传入三项 `verification.*` 属性。`validate_xml` 要求原始 Surefire XML 中每项恰好出现一次且精确匹配 marker，同时仍要求真实退出码 0、非空本次日志、报告新鲜度与完整 21 项结果。复制/空白改写、旧 nonce、错误 SHA、缺失或重复属性均有反例。最终原 XML 的三项属性各出现一次，与新 marker 完全一致。

2. **仅凭 `passed=true` 接受数据库记录已关闭。** 当前 `database_evidence` 验证固定候选 DDL 的完整 plan、两个源码 hash 与 prepare 源码清单一致，init/pre/post 同一专库且与显式 `FC_DB_NAME` 一致，并要求真实 9.4.0、默认 RR、三个 P_S 表可读、完整表集合、全部 0 行、0 触发器、逐表 SHOW CREATE hash 及完整生产 scope index。初始化还要求目标原先不存在、实际新建且完整执行 DDL；观察必须为既存目标的只读事务。prepare 保存 init/pre 原件 hash，verify 拒绝原件改变及前后结构 map 不同。passed-only、错误数据库、旧来源、缺事实、错误版本/行数/索引/权限等反例不能合格。

3. **提前观察空库充当测试后清理结果已关闭。** `observation_times` 强制完整、含 UTC 时区的开始/完成字段，且 `start <= complete <= now`；prepare 要求初始化完成不晚于前观察开始，前观察完成后才生成 marker；verify 要求后观察开始不早于本次 prepare、XML 和 Maven 日志的完成 mtime。missing/naive/reversed/future/out-of-order 和 premature-post 反例均已通过。workflow 实际顺序为 Maven→always 后观察→verify。修改此 guard 后，主任务保留首轮原始 attempt，以新数据库、新源码指纹和新 nonce 重跑完整 IT；最终结果来自冻结后的执行，没有以重写旧身份替代重跑。

## 实现符合性

| 范围 | 独立核查 |
|---|---|
| 两事件必需执行 | `verification-tools`、`payroll-mysql-it` 无 job `if`、事件/路径绕过、`needs` 或 `continue-on-error`；PR 与 push 都由现有 main 触发器到达。原 backend、frontend、smoke/full E2E 内容及边界保留，普通 backend 仍仅选择 `*Test`，不引入数据库。 |
| 显式离线范围 | runner 只加载指定证据、验收运行器、来源、SQL OfflineSafetyTests 和新增安全模块。零用例、重复 ID、fail/error/skip/subtest failure、未完成及异常中断均不能通过；逐 case 结果、来源前后状态和失败记录保存。没有使用全目录 discover 或默认加载真实 SQL 套件。 |
| 初始化写入边界 | 固定读取 `sql/financial_cloud_init.sql` 与生产索引补丁，所有候选 CREATE/ALTER 在连接前完成白名单校验。引号、注释、分号和 executable version comment 不隐藏危险执行；种子、历史 DROP 和源库 CREATE/USE 不进入计划。跨库、AS SELECT、未知结构/危险表达式、任意 PREPARE/EXECUTE 被拒绝。生产索引补丁整体经过 canonical hash 审核后只提取相同纯 ALTER，运行不开放动态 SQL，也不启用 MULTI_STATEMENTS。 |
| 历史冗余 ALTER | 三条固定资产历史 ADD 仅在类型、默认值、属性及预期 AFTER 与已有 CREATE 明确一致时离线排除，保留原 SQL/hash/原因；没有吞掉数据库 duplicate-column 错误。实际计划为 78 CREATE、9 ALTER，共 87 条。 |
| 全新专库 | 五项 FC_DB 参数显式非空，专用命名空间及 64 字符限制；完整 plan 再校验后才连接。存在检查后 CREATE 不带 IF NOT EXISTS，创建竞态或半初始化失败均返回失败，保留目标，不 drop/reset/reuse/seed，不调用 `run_init_sql.py`。 |
| 观察及原件 | `--observe` 仅在既存专库开启实际只读事务，读全部表/列/行数/引擎/触发器/结构及索引，最后 rollback；初始化与前后观察各存不同原件。报告核验工具只读文件，不连接数据库或启动 Maven。 |
| 源码与 PR 身份 | 完整实际 HEAD、Git 状态和文件字节指纹在运行前后采集；CI checkout 必须匹配 GITHUB_SHA 且 clean，run/attempt/event 必需，PR source-head 与实际合并 checkout 分别记录，push 要求两者一致；本地 dirty 状态保留且不虚构远端身份。 |
| 报告完整性 | suite/class、15 个单独 case 与 6 个参数化 invocation 共 21 个完整 ID、唯一性、必需统计及零失败/错误/跳过均核验；retry/flaky 内容、实体声明、缺日志/退出码、来源漂移及旧报告均拒绝。单一总数或 exit 0 不能代替完整报告。 |
| 失败保留与上传 | Maven pipefail 和实际 exit output 保持非零失败；后观察和 verify 按前置成功条件 always 执行，两个上传步骤 always 且缺文件 error。初始化前的服务/checkout 失败仍为 job failure，设计没有承诺此时必有附件。密码和连接用户名不进入初始化 JSON；失败异常只保留受控原因或类型。 |
| 可信 profile | `jinbooks-release-v1` 的 profile version 为 2，原 groups/jobs/steps 保留，新增两个 job 及完整必需步骤精确匹配 workflow。旧绿色 CI 缺新 job、skipped 新 job、缺 prepare/verify 反例通过已有核验器的语义阻塞；`verify_release_evidence.py` 本轮仅去掉错误文案中固定的“version 1”，未降低核验规则。 |

## 实际产物复核

已只读比较原 JSON、XML、日志、逐 case 结果及 hash；没有另行执行核验命令。

| 产物 | 结果 |
|---|---|
| `.e2e-run/iteration5-offline-frozen/evidence/` | 126 planned/executed/unique case，全部 passed，失败/错误/跳过 0。原日志为 126 项、14.785 秒、OK。 |
| `.e2e-run/iteration5-offline-frozen/sql/` | 13 项 OfflineSafetyTests，全部 passed，失败/错误/跳过 0；没有真实 SQL 套件混入。 |
| `.e2e-run/iteration5-offline-frozen/safety/` | 81 项，组成 43 initializer + 7 identity + 21 execution/report guard + 10 workflow/profile，逐 case 全部 passed，失败/错误/跳过 0。原日志 22.406 秒、OK。 |
| `.e2e-run/iteration5-payroll-final/schema-init.json` | 新库 `financial_cloud_e2e_20261003_it4_ci_local_6c0d4fa1_final`，targetExisted=false、databaseCreated=true、完整 87 DDL；78 张 InnoDB 表均 0 行、0 触发器。plan hash 为 `bd00d3fa68367e204d9583d9e1b25d16c725e1666d3ef6f07e6c16cee810c373`。 |
| `database-before.json` / `database-after.json` | 同一目标、同一 plan/source；实际 MySQL 9.4.0、global/session RR、performance_schema=1、data_lock_waits/data_locks/threads 可读，观察 transactionReadOnly=1。78 行数均 0、0 触发器，全部 SHOW CREATE map 与初始化相同；scope index exact name、四列顺序、非唯一、无前缀、可见、ASC、BTREE 一致。 |
| `run.json` / `command-exit.json` / `maven.log` / `result.json` | 本地显式 `mvn -B test -Dtest=PayrollTransactionMysqlIT -o` 加三项运行属性；真实 command exit 0，BUILD SUCCESS，21 项/0 失败/0 错误/0 跳过，suite 11.04 秒、Maven 总计 22.276 秒。result passed=true，sourceUnchanged=true。 |
| 原始 Surefire XML | `financial-cloud/target/surefire-reports/TEST-com.financial.cloud.service.hr.PayrollTransactionMysqlIT.xml`，21 个 testcase/unique name，case ID 与 result 完全一致，三项属性各一次匹配；原 XML hash `a313f8b7fa859b6b1265a455ee015876ec8962bc17f034f4870f0beecd5bef40`。 |
| final `existing-target-rejection.json` / `database-after-reject.json` | 同一 final 目标重复初始化明确失败，targetExisted=true、databaseCreated=false，无 DDL 完成；随后只读检查 passed=true，仍 78 空表、0 触发器、结构 map 无变化。 |
| `.e2e-run/iteration5-actionlint-result.json` / `.log` | actionlint 1.7.7 对完整 workflow 返回 0。参数明确禁用未安装的 ShellCheck/Pyflakes；不能据此声称这两个检查器已运行。 |
| `.e2e-run/iteration5-evidence.json` | 最终私有索引的 33 项事实检查全部为 true，offlineActualExecutedTotal=220，allRequestedLocalChecksSatisfied=true，releaseReady=false、accountantSignOff=pending。11 份冻结 manifest 与本文及当前文件逐一匹配，0 差异；私有 `iteration5-payroll-final/payroll-it.xml` 与原 XML 字节 hash 相同，保留可供后续核查的原报告。 |

三项离线结果及实际 IT 的原始 before/after 均记录 HEAD `4571047d0b836848190e8952785e8590cee606d9`、dirty 工作区及相同完整源码指纹 `4b5f47fd899d6a5c1e344cbdd52217594f489f9d356e1622220fbc991dbb1140`。11 份冻结代码当前 hash 均与该原始 before manifest 匹配。之后写本文、记录、tasks 或归档会改变全仓指纹；不回写测试时身份，也不将原始证据重绑为后续干净提交。

最终时序（北京时间）为初始化 16:22:17.397→19.245，前观察 19.359→19.886，prepare 22.559，XML/日志完成 16:23:08.259/08.304，后观察 16:24:52.386→52.769，result 完成 55.329。init/pre 原件、XML、日志及后观察五项实际文件 hash 均与 marker/result 索引一致。

## 后续边界

实际 Surefire 环境是 **Windows 11 / JetBrains JDK 25.0.3 / 本地 mvn -o**；workflow 配置是 **Ubuntu / Temurin 21 / ./mvnw**，另配置 Python 3.12。固定 9.4.0 新库、同一 helper 及完整 IT 的本地结果已验证，尚未执行该 GitHub 环境完整 job，不能把本地通过写成远端绿色或这些未运行环境的兼容认证。

初始化是固定候选 DDL 的空测试结构工具，不承诺通用迁移、任意未来 SQL 方言或业务库修复；新的不支持结构必须显式失败并受审。当前物理数据库实例没有绑定 server_uuid；这不阻塞固定 job 隔离 service 的本片交付，也不能扩展为生产服务与数据源身份认证。MySQL 8.x/9.7、生产运行版本、真实会计签字、第二实例恢复和部署授权均不在本次证明内。

## 冻结文件 SHA-256

本表为审查时读取实际字节并与最终测试 before manifest 对照的 11 份代码；不含本文和后续规格/记录。

最终私有索引 SHA-256 为 `bae11f334c57b819b5f790e17abfcff00813214e8c9adb1c2739c3417c4ed632`；其 `frozenElevenTestedCodeFiles.aggregateSha256` 为 `001caa1b86e7f92e56e16a8c3a1f86b2af64ee456ed1733f0d4dc4a71a247485`。

| 文件 | SHA-256 |
|---|---|
| `.github/workflows/ci.yml` | `2aac441dba083c573b3ef9d15fa5046e851d211b11474206febb2b74ca8a7ca2` |
| `tools/release_evidence_profile.json` | `3adc2afd76914734d960abfde370264b6cc61e0d2cd6ea6e50accda492c1fe51` |
| `tools/verify_release_evidence.py` | `8aae1bb851c8c8422917bc22593eda95d648f094f0b02d7d844ad5aff2751954` |
| `tools/init_payroll_it_schema.py` | `7f106b7990df970e6f5e364423e355d5f6e7af176510afe3f1c3de9306b02741` |
| `tools/test_init_payroll_it_schema.py` | `9459707a0ce3c6b45e62ccb41302d836afe3fb2cd2067eccf08c926cb23c4788` |
| `tools/verification_ci_evidence.py` | `08e30649bdf3de60ef2d6714d9855337c64e41b8ceaff51c9cecf1e9fb92faa9` |
| `tools/test_verification_ci_evidence.py` | `db96d406cda001387ff18d8b43ad29338a1b9ba56fcc2279d2da91cb70220eac` |
| `tools/run_verification_ci_tests.py` | `8611a3d4fb8b4651d04fcd1cca873431d369a6f2ae942efc78bb8216b4c12748` |
| `tools/verify_payroll_ci_reports.py` | `59b978ee4d6a6bb20a7db10c5ae087299ac1264837e31d2f9698b0e377a2ba62` |
| `tools/test_verification_ci_execution.py` | `51652cef6a383d898a731b70992f9f2b2e7a7501c6f1f0372d17175e72df5753` |
| `tools/test_verification_ci_workflow.py` | `58b19b70ee2755a2a983b1f339fc486f3ec17f939016fe63137304597643f046` |
