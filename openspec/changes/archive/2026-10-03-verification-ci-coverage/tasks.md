## 1. 明确套件与原始证据入口

- [x] 1.1 建立可复用的运行元数据采集：完整checkout SHA、before/after源码状态和指纹、事件/run/attempt、PR sourceHead、命令与开始/结束状态；以文件/mock单测验证源码漂移、SHA不符、local缺CI字段及凭据脱敏，不伪填远端身份。
- [x] 1.2 实现显式离线套件入口及逐case报告，加载现有126项证据工具、13项OfflineSafetyTests和新增初始化/证据安全测试；验证fail/error/skip/0执行/中断均非零，默认不加载LiveVoucherSqlTests、不连接数据库，并保存成功与失败原始结果。
- [x] 1.3 实现Payroll报告prepare/verify两阶段，执行前marker记录旧XML摘要与来源，执行后核验本次完整21项case、统计、0失败/错误/跳过及内容哈希；fixture验证旧报告未重生成、缺marker、漏/重复case、字段缺失及源码变化均失败，日志和XML可追溯。

## 2. 全新专库受控初始化与观察

- [x] 2.1 从固定仓库DDL和工资范围索引补丁构造完整结构计划，保留生产字段和约束，不执行DROP/种子/任意动态SQL；离线单测覆盖早期INSERT、注释/引号/分号、跨库及危险语句、未知结构段和篡改索引补丁，验证全部计划创建前完成审核并保留输入/计划哈希。
- [x] 2.2 实现显式五项FC_DB参数、专用命名空间及64字符边界，查询目标不存在后无IF NOT EXISTS创建；mock验证既存库、创建竞态、非法配置及半初始化失败无drop/覆盖/种子/自动重用，且不调用run_init_sql。
- [x] 2.3 实现初始化后及只读复查记录：实际9.4.0、目标DATABASE、默认RR、全部表0初始行、完整生产scope索引及锁观察权限；单测验证版本/索引/隔离级别/权限不符失败且不泄露凭据，受控新库实际观察在任务4.1完成。

## 3. Workflow与可信profile接入

- [x] 3.1 在ci.yml加入verification-tools，PR/push均运行表中三个显式步骤并always上传报告；静态/文件测试验证无push-only/path跳过或continue-on-error，失败与缺文件不被吞掉，原job文本和默认backend单测范围保持。
- [x] 3.2 在ci.yml加入独立payroll-mysql-it，固定mysql:9.4.0-oraclelinux9、专库run/attempt命名和显式连接，按initialize→observe→prepare→显式Maven IT→verify执行并always保存XML/日志/元数据；验证两事件均可达、无匹配失败、步骤身份固定且失败报告仍保留，不复用compose或会计E2E库。
- [x] 3.3 追加release_evidence_profile必需job/step并更新版本身份与fixtures，保留既有范围；单测验证旧CI缺新job、新job skipped、缺prepare/verify等必需step无法通过，job/step显示名与workflow精确对应。

## 4. 必要集成验证、审查与收口

- [x] 4.1 用同一受控工具在明确授权的全新专库验证DDL-only初始化、全部空表及版本/权限/索引，再prepare并实际运行完整21项PayrollTransactionMysqlIT、verify及后观察；保存实际命令、源码指纹、XML和环境证据，既存库拒绝复验不得改变其结构/数据。本地真实9.4.0验证与镜像实际拉取/远端未运行状态分别记录，不能补写未经执行结果。
- [x] 4.2 执行全部新增及相关既有工具单测、workflow/profile一致性检查和独立审查；重点复查DDL白名单、数据库归属、旧报告重新绑定、PR合并SHA与sourceHead、失败上传及只读核验边界，修复阻塞发现并保留审查证据。
- [x] 4.3 更新验收说明、R0-009及第五轮记录，保存真实验证范围/命令/结果，严格校验变更及主规格、差异和链接，按项目流程同步与归档完成项；明确配置/本地实现已交付、远端CI仍pending，不提交推送，不声称生产运行来源、会计签字或独立恢复完成。

## 5. 后续外部候选验证

以下不计作本片实现未完成项，也不能用本地fixture或旧提交结果替代：

- 在后续明确的新候选提交上实际执行PR/push CI，核对完整SHA、全部必需job/step及上传的原始报告。
- 固定9.4.0镜像未被实际拉取或启动时保留待验证；MySQL8.x/9.7兼容性需要各自实际环境，不由本片推导。
- 生产前端/后端运行身份与数据源绑定属于R0-009后半片；真实会计试账签字、问题台账认可及第二实例恢复继续按原路线补齐。
