# 自主迭代第 5 轮：新增验证纳入 CI

日期：2026-10-03。变更：`verification-ci-coverage`，对应产品路线 R0-009 前半片。

本轮已交付 CI 配置与本地验证，独立审查通过。220 项离线检查、21 项真实 MySQL 事务测试均无失败或跳过。12/12 实施任务完成，规格已同步并归档；截至本轮归档时工作区未提交、未推送，新候选远端 CI 待执行，发布资料仍未就绪。

2026-10-03 集成交付补记：用户随后授权合并推送，第二至第五轮成果纳入本次 main 集成提交；提交前完整后端单测复验 593 项通过、0 失败/错误/跳过，29 项主规格严格校验通过。下文和独立报告继续描述原本地执行，不改绑历史身份；实际远端结论以本次新 SHA 对应运行和交付记录为准。

## 交付行为

在原有 CI job 文本后追加两个独立 job：`verification-tools` 与 `payroll-mysql-it`。两者在 main 的 PR 和 push 均运行，不依赖仅 push 执行的完整会计 E2E。原 backend、frontend、冒烟及完整会计检查范围保持；默认后端单测继续不连接数据库。

离线入口按证据工具、SQL 边界、安全检查分别保存逐项结果、日志、命令和测试前后完整源码身份。失败、错误、跳过、零执行或中断均返回非零，SQL 入口只加载 OfflineSafetyTests。Payroll IT 单独使用固定 MySQL `9.4.0-oraclelinux9` 服务及按 run/attempt 命名的全新专库，明确 initialize、只读观察、prepare、Maven、事后观察、verify 和上传步骤。

可信 profile 保持 `id=jinbooks-release-v1`，version 从 1 升到 2，追加两 job 及 11 个必需 step。根证据协议仍为 schemaVersion 1，既有 75 个会计/隔离定义及原 CI 必需检查未删减。旧绿色 CI 缺新 job、新 job skipped 或缺必需 step 均无法通过当前完整性判断。

## 实际验证

| 验证 | 实际结果 | 原始材料 |
|---|---|---|
| 证据工具离线套件 | 126 项通过，0 失败/错误/跳过 | `.e2e-run/iteration5-offline-frozen/evidence/` |
| SQL 离线边界套件 | 13 项通过，0 失败/错误/跳过 | `.e2e-run/iteration5-offline-frozen/sql/` |
| 新增安全套件 | 81 项通过：初始化 43、身份 7、执行/报告 21、workflow/profile 10 | `.e2e-run/iteration5-offline-frozen/safety/` |
| 全新专库初始化 | 78 个空表、87 条 DDL；真实版本 9.4.0、默认 RR、范围索引与三项锁观察权限满足 | `.e2e-run/iteration5-payroll-final/schema-init.json`、`database-before.json` |
| 真实 PayrollTransactionMysqlIT | 21 项通过，0 失败/错误/跳过；Maven 总耗时 22.276 秒 | 同目录 `maven.log`、`command-exit.json`、`payroll-it.xml` |
| 最终报告核验与只读观察 | 新运行标识与源码属性匹配；init/pre/post 结构一致；全部 78 表恢复为空、0 trigger | 同目录 `run.json`、`result.json`、`database-after.json` |
| 既存专库拒绝 | 初始化预期退出 1；拒绝后只读检查通过，结构和零数据不变 | 同目录 `existing-target-rejection.json`、`database-after-reject.json` |
| Workflow 校验 | actionlint 1.7.7 退出 0 | `.e2e-run/iteration5-actionlint-result.json`、`iteration5-actionlint.log` |
| 独立审查 | 无剩余阻塞；冻结 11 份代码与原执行清单逐项一致 | [审查报告](self-iteration-05-review.md) |

actionlint 本次关闭未安装的 shellcheck 与 pyflakes，只作为 workflow 语法和表达式校验。安全套件中的预期失败夹具会输出失败记录；上表数量来自外层逐项完整结果，不能将夹具内容当作真实业务或远端验收。

实际命令依次为 `python tools/run_verification_ci_tests.py --scope <evidence、sql 或 safety> --output <本组新目录>`；`python tools/init_payroll_it_schema.py --output <schema-init.json>` 与 `--observe --output <观察文件>`；`python tools/verify_payroll_ci_reports.py prepare --report-dir financial-cloud/target/surefire-reports --output <新目录> --launcher mvn --offline`；随后执行 marker 中的 `mvn -B test -Dtest=PayrollTransactionMysqlIT -o` 及三项 `-Dverification.*` 参数；事后观察完成后执行 `python tools/verify_payroll_ci_reports.py verify --report-dir financial-cloud/target/surefire-reports --output <目录> --command-exit 0`。实际 Python 调用包含 `-B`，不生成字节码文件；私有脚本 `.e2e-run/run-iteration5.ps1` 固定本轮专库，保留真实命令和结果。

## 初始化与证据保护

初始化只接受五项显式 FC_DB 配置及专用库名前缀。完整结构计划在连接和建库前审核，从固定初始化 SQL 与生产工资索引补丁选择 78 条 CREATE 和 9 条 ALTER；不调用清库初始化器，不执行源文件中的 DROP、种子或任意动态 SQL。三个资产字段已包含在 CREATE 中，冗余 ADD 经类型、空值、默认值及排序规则的语义核对后明确排除并记录依据，不靠吞掉 SQL 错误获得通过。既存库、竞态或部分初始化失败均不自动覆盖、删除或重用。

开发时曾在环境查询中使用未加引号的保留字 alias，真实服务器返回 1064；当时尚未创建数据库。修复为加引号的元数据 alias 后，43 项初始化安全单测及最终新专库初始化通过。该失败材料和第一次事务执行保留为开发记录，最终结论只使用 `iteration5-payroll-final` 的重新执行。

独立审查先后发现并关闭三项阻塞：旧 XML 内容可被重新绑定、环境记录只判断 `passed`、事后观察可能早于测试。最终 prepare 生成随机运行标识，XML 三项 verification 属性须各恰好一次匹配本次运行标识、完整 checkout SHA 和源码指纹；完整 21 个 case、真实退出码、原日志及 XML 哈希必须满足。数据库记录须具备当前 DDL 来源/计划、同一目标库、真实版本/RR/权限、全部空表/结构/索引等完整事实。init、pre、prepare、XML/日志和 post 的 UTC 时间顺序严格核验，缺失、倒序、未来或提前 post 都失败。

最终汇总 `.e2e-run/iteration5-evidence.json` 冻结 33 项通过检查、原件哈希及 11 份代码清单。最终 XML 私有副本 SHA256 为 `a313f8b7fa859b6b1265a455ee015876ec8962bc17f034f4870f0beecd5bef40`；证据汇总明确 `releaseReady=false`。

## 执行对象与适用边界

实际测试时 HEAD 为 `4571047d0b836848190e8952785e8590cee606d9`，源码状态为 dirty。三组离线与最终 IT 的 before/after 原始清单均稳定，共同指纹为 `4b5f47fd899d6a5c1e344cbdd52217594f489f9d356e1622220fbc991dbb1140`。文档、任务与归档收尾会改变完整工作区指纹；保留测试时身份，逐项确认 11 份实现文件仍与已测字节相同，不将历史证据改绑为最终文档状态。

真实数据库来自本轮实际拉取并启动的 `docker.io/library/mysql:9.4.0-oraclelinux9`，WSL 使用 Podman 的 Docker 兼容命令。最终目标为 `financial_cloud_e2e_20261003_it4_ci_local_6c0d4fa1_final`，与开发第一次专库隔离。测试容器及两个专库保留供复核；最终全部表为空。未操作正式业务库，也未宣称容器已删除。

原 Surefire 记录实际 Java 25.0.3、JetBrains、Windows 11，本地 launcher 为 `mvn -o`；CI 配置为 Ubuntu、Temurin 21 和 `./mvnw`。已验证相同初始化/报告工具与 21 项真实 MySQL 场景，本地结果不能证明完整远端 job 已执行。未新增复跑 593 项后端完整单测、前端构建、完整会计或账套隔离 E2E，也未构建新生产 JAR。MySQL 8.x、9.7 兼容性须分别实测，不能由 9.4.0 结果推导。

第一轮 `4571047` 的历史 CI #157 仅覆盖原提交；第二至第五轮均尚未提交推送。本轮 job 元数据由执行工具产生，支持技术核对，不能独立认证提供方或生产服务身份。

## 收口与下一片

主规格已新增 5 项要求、14 个场景，29 项主规格与本变更均通过严格校验，差异检查通过。12/12 实施任务完成；归档位于 [2026-10-03-verification-ci-coverage](../../openspec/changes/archive/2026-10-03-verification-ci-coverage/tasks.md)。归档前后全部 5 份文件哈希一致，包含 `.openspec.yaml`；原始执行证据、11 份已测实现文件及独立报告保持不变。私有收尾记录为 `.e2e-run/iteration5-archive-integrity.json` 与 `iteration5-closeout-archived.json`。

下一片为 R0-009 后半段：采集生产前后端运行产物、源码与实际数据源的对应关系。新候选的实际 PR/push CI 属于 R0-003 待办；真实完整月份、负责会计签字、问题台账认可与真实第二实例恢复继续保留人工或环境依赖。正式发布状态不变。
