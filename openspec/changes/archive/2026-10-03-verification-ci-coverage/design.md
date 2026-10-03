## Context

动机见 [proposal.md](proposal.md)，行为契约见 [spec.md](specs/verification-ci-coverage/spec.md)。现有 CI 的后端命令只选择 `*Test`，Python 工具没有入口，完整 E2E 仅 push 执行；PR 冒烟不能提供新工资事务 IT 的结果。

当前工具基线是 92 项核验器、22 项验收运行器、12 项来源测试和 13 项 SQL 离线保护。第四轮真实 IT 的 21 项在 MySQL 9.4.0、默认 RR 上通过，要求 `financial_cloud_e2e_20261003_it4_*`、显式连接配置、生产范围索引、锁等待观察权限及空业务表。它会创建自有夹具和故障触发器，结束后清理其拥有的记录；无需启动应用服务。

`sql/financial_cloud_init.sql` 同时含 DDL、DROP 和种子，且 seed 标记之前也有 INSERT；`tools/run_init_sql.py` 会删除已有库并安装种子。第四轮准备脚本从已有隔离库复制空结构，但 CI 不能依赖本机来源实例。现有 compose 配置为 9.7，不能替代本片固定 9.4.0 环境。

## Goals / Non-Goals

**Goals:**

- 在现有 workflow 中添加两个可独立定位失败的 job，保留原 job 内容和执行边界。
- 复用当前测试及生产 DDL，让本地与 CI 使用同一初始化和结果核验入口。
- 用来源指纹、实际环境观察和原报告说明本次运行；失败也保留证据。

**Non-Goals:**

- 不改变薪资事务协议、IT 的断言或数据库命名限制，不将数据库 IT 混进默认单测。
- 不修改现有 compose 版本、会计 E2E 的初始化方式，不做 MySQL 8.x/9.7 兼容认证或广泛数据库矩阵。
- 不实现生产服务身份 API、生产前端运行绑定、真实远端 CI 调度、会计签字、独立恢复或自动发布。

## Decisions

### 1. 两个无事件/路径跳过条件的新 job

固定 job ID 和显示名为 `verification-tools`、`payroll-mysql-it`，沿用 workflow 的 PR→main 与 push→main 触发器。新 job 不依赖 push-only `e2e`、不设置路径过滤或 `continue-on-error`。这样同一可信 profile 能精确查找 GitHub 导出的名称，不会把未运行视作成功。

| Job | 必需步骤显示名 | 范围 |
|---|---|---|
| verification-tools | Run evidence tool tests | 显式加载 `test_release_evidence.py`、`test_accounting_acceptance.py`、`test_release_evidence_provenance.py` |
| verification-tools | Run payroll SQL offline boundary tests | 只加载 `test_payroll_live_voucher_sql.py` 的 OfflineSafetyTests，不启用 `--mysql` |
| verification-tools | Run payroll initializer safety tests | 新初始化与证据入口的必要文件/mock单测 |
| payroll-mysql-it | Initialize payroll IT schema | 全新专库及受控生产 DDL |
| payroll-mysql-it | Verify payroll IT environment | 实测版本、数据库、默认RR、索引及观察权限 |
| payroll-mysql-it | Prepare payroll transaction evidence | 执行前来源/start及旧XML摘要marker |
| payroll-mysql-it | Run payroll MySQL transaction integration | 显式选择完整 `PayrollTransactionMysqlIT` |
| payroll-mysql-it | Validate payroll IT reports | 新鲜原始XML、完整case结果及候选身份核验 |

两个 job 分别使用 `Save verification tools evidence` / `Save payroll integration evidence` 上传证据。上传用 `if: always()`，缺必需目录不得只输出 warning；已开始的测试入口尽早建立失败记录。服务启动或 checkout 之前即失败时，GitHub 尚未执行上传步骤，job 失败仍保留，不能承诺该阶段一定有附件。原有后端与前端 job 不必等待新 job 才执行，既有发布 profile 则要求所有必要 job 成功。

不选择全目录 unittest discover，因为会包含显式数据库套件或不相关脚本；不选择复用会计 E2E job，因为其种子、清账套和 push 条件与本 IT 不兼容。

### 2. 显式离线套件与可审阅结果入口

新增 `tools/run_verification_ci_tests.py --output DIR`（可按固定 scope 选择上述三步），直接执行明确的 unittest 套件并保存运行/逐case结果、日志和实际统计。缺套件、零执行、skip、failure、error、异常中断均返回非零；不调用业务服务、Playwright 或数据库。当前 126+13 只是初始范围，新初始化/证据单测单列，不能把未来总数固定为139来替代case范围。

SQL 离线入口与现有脚本默认 main 选择一致：只执行 OfflineSafetyTests。不得通过普通 discover 加入 LiveVoucherSqlTests 后把其 skip 当通过。文件/mock单测不需要安装或访问 MySQL。

### 3. 独立固定 MySQL 9.4.0 与全新专库

`payroll-mysql-it` 使用 GitHub job 独立的 MySQL service，镜像固定 `mysql:9.4.0-oraclelinux9`，映射到显式回环主机/端口；不调用 docker compose。官方标签已由主会话核查，实际拉取与启动必须验证。镜像不可用时保留失败，不能改用 latest、9.7或8.x后仍记9.4。

库名使用 `financial_cloud_e2e_20261003_it4_ci_<run_id>_<run_attempt>`，纯小写安全字符且不超过64字符；本地使用同前缀及独立nonce。保留现有 Java IT 的命名空间，避免本片修改其业务测试契约。五项 FC_DB 配置必须显式且无业务默认；CI仅连接本 job 的一次性数据库服务。测试账户须具备专库DDL/DML、触发器操作及 performance_schema 锁观察权限；当前可在该一次性容器使用管理员测试账户，凭据不进入证据。

数据库默认仍为 REPEATABLE READ，入口新建事务的RC及外层RR由现有IT检查。环境观察使用真实 `SELECT VERSION()`、`SELECT DATABASE()`、`@@transaction_isolation`、索引元数据及锁观察查询。结果要求9.4.0、RR，以及完整的 `idx_salary_payroll_scope`：`book_id, belong_date, employee_id, created_date` 顺序、非唯一、完整列、可见、升序、BTREE。配置的镜像名只作为 declared 信息。

### 4. 先建立纯结构计划，再创建新库

新增 `tools/init_payroll_it_schema.py`，只消费仓库固定的初始化DDL和工资索引补丁，不接受任意SQL路径或来源数据库。先离线构造并核验全部结构计划，再连接查询目标是否存在，使用无 `IF NOT EXISTS` 的 CREATE DATABASE 防止检查后竞态覆盖；目标存在或创建失败立即退出，不执行后续语句。

从 `sql/financial_cloud_init.sql` 的明确表结构/结构补丁段提取原始 CREATE TABLE 与必要 ALTER TABLE，保留生产字段、引擎和约束；源文件中的历史DROP和种子语句不进入执行计划。不能仅截取seed标记前内容，也不能在执行时遇到危险语句才跳过。语句分割需处理引号、注释及分号；未识别的结构段、跨库引用、CREATE-AS-SELECT、任意动态SQL、DML或DROP进入计划时，创建前失败。原始语句与所选段落索引、输入哈希和执行计划哈希一并记录，避免维护一套手写测试表DDL。

生产范围索引使用固定仓库 `sql/patches/2026-10-03-payroll-write-scope-index.sql`。其中条件SET/PREPARE/EXECUTE仅作为该受控补丁的整体单元允许，须核验它只观察本库目标索引、执行相同已审阅索引结构或无操作；不得开放任意PREPARE/EXECUTE。解析/来源边界不成立时失败而非退回通用初始化工具。

完成后检查创建表清单、所有表行数为0、默认隔离级别、范围索引和观察权限并保存事实。初始化失败时保留新建的不完整测试库与失败证据，不 DROP、不重建、不重试复用。CI job 容器随runner回收；本地库保留供审查。IT随后可按既有协议写入和清理自己的夹具，这与初始化无种子/不清库的边界分开。

### 5. 来源、实际报告与环境记录组成同一次运行

新增共同元数据模块可复用 `release_evidence_provenance.py`，两类入口在开始/完成时采集完整HEAD、源码状态和字节指纹。CI记录 workflow checkout SHA、实际 `git rev-parse HEAD`、run ID/attempt/event；PR源头使用独立 `VERIFICATION_SOURCE_HEAD` 字段记录，不将其写成测试HEAD。CI checkout与声明SHA不一致、源码发生变化时阻塞。没有CI字段的本地执行明确为local，当前未提交工作区可以留研发证据，不能冒充干净候选远端结果。

IT用显式 Maven命令选择整个类并启用无匹配失败。`tools/verify_payroll_ci_reports.py`采用prepare/verify两阶段：Maven前marker记录开始时间、来源指纹及已有XML摘要（不存在也明确记录）；Maven后verify对照marker，拒绝未重新生成的旧XML及源码漂移，不能仅从默认target目录读残留文件。它核验实际 PayrollTransactionMysqlIT XML：suite身份、当前声明的完整case集合（本片基线21）、逐case成功且无重复/遗漏、统计与实际case一致、0失败/错误/跳过、执行完成及文件哈希。报告总数本身不能代替逐case结果，也不通过重试掩盖失败。

prepare 生成随机运行 nonce，实际 Maven 将 nonce、checkout SHA 和源码指纹作为三项 `verification.*` 系统属性传入；verify 要求原始 Surefire XML 每项恰好出现一次且精确匹配。复制旧 XML 或只改空白和修改时间均不能通过。Maven 的真实退出码和非空本次日志也必需。

初始化、前观察和后观察原件必须包含完整实际事实、同一目标、来源 DDL 哈希与完整结构计划；源码哈希须对应 prepare 的源码清单，78 表结构哈希前后相同。记录时间必须含 UTC 时区、开始不晚于完成且完成不在未来；顺序为初始化完成→前观察→prepare→Maven 报告/日志完成→后观察，不能提前观察空库再充当清理后结果。后观察在 Maven 之后、verify 之前按 always 执行。结果索引连接初始化记录、前后来源、命令退出码、原 XML/日志哈希和全部环境观察；缺任何关键记录为失败。未观测字段为 unknown，失败阶段与原因保留。

日志/报告只记录数据库定位、结构和验证结果，不回显密码、授权资料或连接凭据。只读报告核验不创建/清理数据库；初始化工具与IT各自有明确写入边界。

### 6. 可信profile与交付状态同步

`tools/release_evidence_profile.json`保留现有jobs/steps、追加上表两个job及必需步骤，更新profile版本/身份。核验器仍按真实导出的job/step名称、候选SHA和结论工作；fixtures随可信profile生成对应新范围。加入旧绿色CI缺新job、新job skipped、缺必需step等反例，不篡改历史记录。

本片实施交付门槛为新增工具单测、workflow/profile一致性、同一脚本在全新授权隔离库上的实际初始化和21项IT、原报告及来源核验、独立审查和严格规格校验。远端CI必须在后续明确的新提交上真实执行；当前只交付配置/本地证据，状态为远端待验证，不阻塞本片完成，也不代表发布就绪。

## Risks / Trade-offs

- [混合初始化文件误执行种子或DROP] → 执行前完整计划白名单与边界单测，真实新库验证全部初始行数；不复用reset入口。
- [数据库创建竞态或半初始化失败] → 无IF NOT EXISTS创建，既存目标拒绝；失败保留、不删除或重试复用。
- [9.4.0镜像或观察权限不可用] → 验证实际拉取/版本/权限；失败显式保留，不换版本或skip IT。
- [PR合并提交与源头混淆] → 分别记录实际checkout和sourceHead，发布仍按候选完整SHA核验。
- [旧XML残留、汇总绿色掩盖case缺失] → 本次独立报告来源、逐case覆盖及错误检查，不相信退出码或总数单一指标。
- [工作区存在多轮未提交改动] → 完整记录dirty状态和字节指纹；本地研发结果与未来干净候选远端结果分开，不重绑旧证据。

## Migration Plan

1. 增加两个job及受控工具，不修改业务库、薪资业务代码或现有compose/E2E job。
2. 在临时目录完成工具安全测试，在全新明确隔离库验证同一初始化入口及IT，保留失败路径和真实报告。
3. 同步profile、fixture、验收说明和本片状态；归档时保留远端CI待验证。
4. 后续正常提交后核对该提交的实际job/step和上传报告；未发生前不得记为远端通过。
5. 若撤回本片，工作流与profile必须成对回退并明确范围缩减；不自动操作已有测试库或生产库，不将旧profile结果换成新范围成功。
