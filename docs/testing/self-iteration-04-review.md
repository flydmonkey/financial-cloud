# 自主迭代第 4 轮独立审查：工资写入事务完整性

审查日期：2026-10-03。审查者：独立代理 `release_risk_audit`，未参与本轮实现。本审查仅写本文件，未修改源码、测试、POM、规格、任务或其他文档，未运行数据库、服务、Maven、OpenSpec、网络或提交。审查对象是本轮冻结后的未提交工作区；最终 15 个生产、测试、工具和迁移文件的字节身份见文末。

## 结论

**本切片审查通过，未发现未解决的阻塞问题，符合 `payroll-row-transaction-integrity` 的规格及限定范围。** 六个参与工资写入口在同账套协调后读取当前关键状态，生成的凭证、分录、字号与选定工资链接在失败时一起回滚。跨账套生成的锁范围问题和外层 RR payroll unlink 读取缺口均已修复，并由真实事务回归关闭；没有以 mock 的多 peer 查询结果代替 SQL 语义或锁等待证据。

本结论允许该实现切片完成，不等于整个财务系统并发安全、现场历史数据已修复、远端 CI 已通过或候选版本已批准发布。正式迁移、部署及发布仍依外部证据门槛和授权；本审查未执行这些动作。

## 两项阻塞的关闭

1. **不同账套生成的锁范围问题已关闭。** `.e2e-run/iteration4-rc-first.log` 中两项生成在实际 RC 连接上超时，证明范围索引与 `FORCE INDEX` 本身不足以保证独立推进。进一步的真实等待和 `EXPLAIN FOR CONNECTION` 定位 peer 倒序扫描请求另一账套工资记录；最终查询固定工资 scope 在先，并移除业务不需要的创建时间倒序。`.e2e-run/iteration4-targeted-fixed.log/.xml` 三项针对性测试全通过；最终 21 项完整 IT 中，`anotherBookCanCommitWhileFirstBookGenerationIsPaused` 和 `missingOwnVoucherBeyondExistingMaximumDoesNotBlockAnotherBookGeneration` 分别 0.435、0.390 秒通过，另一账套在第一账套真实事务仍暂停时完成生成。另有跨账套月度推送 0.385 秒通过，验证 salary 删除/插入不依赖释放第一账套锁。这关闭了已复现的独立推进缺口，不承诺任意规模下的吞吐或响应时延。

2. **外层 RR unlink current-read 缺口已关闭。** 修复前 `EmployeeSalaryService.deleteVoucher` 在 book 锁等待后读到当前工资链接，却调用原 `VoucherService.delete`，其 header 和 item 普通 SELECT 可继续使用调用方旧快照。若另一个参与工资生成事务刚提交新凭证，删除会错误返回“部分凭证不存在”；仅将 header 改为 current read 而不修改 item 读取，还可能使现金流关系清理遗漏新分录 ID。当前 `EmployeeSalaryService.java:668` 改为 `deletePayrollVoucher`，`VoucherService.java:1653–1694` 新建 `MANDATORY` 入口并复用 `deleteInternal(..., true)`，对 header/item 加 `FOR UPDATE`；普通 `delete(..., false)` 保留原行为。已复审真实回归：生成者在实际保存后，用同一个 JDBC connection 查询真实新分录，插入并验证现金流关系；旧 RR 先证实 header/item/cashflow 均不可见，等待后成功删除，最终关联清空、header/item 软删、字号保留且 cashflow 物理删除。最终完整 IT 的 `unlinkWithOlderOuterRepeatableReadSnapshotDeletesNewVoucherAndItemsAfterBookWait` 无 error/failure，0.299 秒。单测还验证新 helper 的 MANDATORY 拒绝无事务调用、普通 delete 的 REQUIRED/读取行为及既有 scope、状态、过账和期间守卫保持。

### peer 排序调整复审

两个 peer 查询去掉 `ORDER BY es.created_date DESC`，保留相同过滤、`LIMIT 1` 与 `FOR UPDATE`，符合既有业务规格。主规格要求任一有效关联即拒绝，没有要求返回最新 ID；服务只判断返回值是否非空，使用同一拒绝信息，不对外展示或写回该 ID。故多个 live peer 中选择任一记录的结果等价；历史软删工资、foreign-book voucher FK 及所选类型范围均保持，缺失或软删 voucher 仍在 LIMIT 前排除。

`.e2e-run/iteration4-backward-scan.log` 记录真实阻塞计划已是工资 `es` first、scope 索引 ref 三常量、voucher PK eq_ref，并有 `Backward index scan`；两次等待分别请求另一账套工资 PRIMARY/scope 的 `X,REC_NOT_GAP`。该证据不能归因于“voucher 表先扫描”，也不能称 STRAIGHT_JOIN 单独解决故障。移除排序后的真实生成回归和完整 IT 已通过，更新后的设计如实保留这条实证链。

## 已核查的实现

已读 active `payroll-row-transaction-integrity` 的 proposal、design、tasks、delta spec，以及生产代码、单测、IT、SQL 工具、索引补丁和初始化注册。代码符合性与实际执行证据均已核查。

| 要求 | 独立核查结果 |
|---|---|
| 同账套六入口协调 | `EmployeeSalaryService` 的 update/save/delete/generateVoucher/deleteVoucher 及 `EmployeeSalaryTempService.createFinalDetail` 在首条业务 SQL 前取同一个 book 主键行锁；新事务 RC、默认 REQUIRED，已有外层事务不会被更改隔离或传播。`PayrollWriteLock` 拒绝空范围、缺失/软删账套和无实际事务调用。 |
| 等待后读取当前工资及 peer | 按 ID、批量、月份和 live peer 的关键查询均为 locking read，关闭查询缓存并刷新既有缓存。工资查询绑定 book，批删规范化并排序 ID、完整验证后才删除；peer 查询保持同 book/employee/month/所选 FK 范围。 |
| 保留历史有效性语义 | live peer 不增加工资 soft-delete、凭证 book/status/date 过滤；foreign-book voucher FK 仍按 ID 判 live，历史软删工资仍可防重。类型 `0/2`、`1/3` 分组和既有模板口径保持。 |
| 生成原子性 | save 非成功时 `setRollbackOnly`，最终只写选定 FK，限定 salary ID/book；零行写回抛业务异常，SQL 异常由事务回滚。另一 FK 保留；失败模板等 save 前拒绝不再独立清理 stale FK。未发现本片新增的 orphan 或另一 FK 被清空路径。 |
| 月度推送 | book 锁在配置/预览读取前取得，预览查询同时限定 preview 和 employee 的 book/month/active；确认工资按 book/month current read，先检查全月链接，后沿用删除/插入。不扩展到预览编辑或月结配置并发。 |
| unlink 删除依赖 | targeted helper 的 header 和 item current read 后，cashflow/items/auxiliary/header 删除、journal 清链接、carryforward 清理均为 current DML；`JournalEntryService.clearLinksByVoucherIds` 直接 UPDATE，没有另一次旧快照查询。current-term/月结状态并发已排除在本片协议之外，不因此要求重构通用凭证流程。 |
| 索引与迁移 | 非唯一 `(book_id, belong_date, employee_id, created_date)` 索引补丁及 IT preflight 检查 exact name、四列及顺序、无前缀、可见、ASC、BTREE；现有正确索引可重复执行，不兼容同名索引明确失败，缺索引不能静默回落。builder 注册及 init SQL 只纳入同一索引补丁；上线需先执行迁移。 |

## 实际证据与测试可信度

审查只读产物，不另行运行命令。

| 产物 | 核查结果及限制 |
|---|---|
| `.e2e-run/iteration4-backend-tests.log` | 最终完整后端 593 项单测、0 失败/错误/跳过，BUILD SUCCESS，22.507 秒，完成于 `2026-10-03T15:08:32+08:00`。 |
| `.e2e-run/iteration4-unit-reports/TEST-*.xml` | 独立只读汇总 90 个 suite，共 593 项，失败/错误/跳过均为 0。默认单测没有混入显式 IT。 |
| `.e2e-run/iteration4-before-stale.log` | 保留原来源源码的同夹具反证：失败模板后 stale FK 被清空，1 个预期失败。 |
| `.e2e-run/iteration4-before-rollback.log` | 原来源源码的真实零行写回及 save 返回失败反证，2 个预期失败；分别出现错误声称成功和已保存 header 未回滚。 |
| `.e2e-run/iteration4-index-probe.json` | 真实 RR 范围边界等待，包括 FORCE INDEX；证明只增加范围索引不足以保证不同 book 独立。 |
| `.e2e-run/iteration4-migration.json` | 最终生产补丁哈希与源码一致；缺索引真实错误 1176、不兼容同名索引真实错误 1061、正确补丁执行两次成功，确认四列完整、顺序、非唯一、可见。ASC/BTREE 由生产检查及实际 IT preflight 进一步验证。 |
| `.e2e-run/iteration4-rc-first.log`、`iteration4-backward-scan.log/.xml` | 保留中间实现的跨账套超时、实际等待和阻塞计划；用于解释修复，不能记作最终通过产物。 |
| `.e2e-run/iteration4-targeted-fixed.log/.xml` | 修复后针对性 3 项、0 失败/错误/跳过，BUILD SUCCESS，包含两个跨账套生成及 outer RR unlink。 |
| `.e2e-run/iteration4-mysql.log` 及最终 `PayrollTransactionMysqlIT` XML | 最终显式真实事务 IT 共 21 项、0 失败/错误/跳过，BUILD SUCCESS，19.465 秒，suite 9.557 秒，完成于 `2026-10-03T15:10:07+08:00`；日志记录 12 次真实 book 锁等待及实际连接上的 RC/RR。 |
| `.e2e-run/iteration4-sql-after.log` | 实际 Java Mapper 注解 SQL 的 21 个回归方法全 OK：13 个离线安全边界方法、8 个 MySQL 方法；保留两类 peer、多行失效遮蔽、历史软删、三维 scope、另一类型、状态/日期及 foreign-book voucher ID 语义。 |

`PayrollTransactionMysqlIT` 使用真实 Spring 事务代理、真实 MyBatis Mapper 和 VoucherService 保存/删除核心；每个事务使用真实物理连接，拦截器逐条验证 catalog、autoCommit、连接身份与实际隔离，第三连接观察 book 锁等待后才放行。普通 mock 只固定配置/科目等输入，不替代多 peer SQL 语义。真实同账套竞态覆盖六入口、不同工资 peer、更新金额先提交、删除先提交、相反批删顺序；外层 RR 生成与 unlink 均先建立并证明旧快照。测试环境记录为 MySQL 9.4.0，数据库默认 RR 没有全局改写。

SQL 工具直接抽取当前 Java text block，保留实际 STRAIGHT_JOIN、唯一允许的生产 scope index hint 和最终 FOR UPDATE，用参数绑定执行；只允许 shadow temporary salary/voucher 表，不允许隐藏外库/表、表达式、写入或其他锁尾缀。临时表验证单连接查询语义，跨连接锁与原子性由上述共享表真实 IT 独立证明。

故障注入边界明确：零行写回在实际 header 保存后、同事务把工资软删，使生产最终 UPDATE 真正返回零；SQL 异常由真实 MySQL trigger 触发；save 返回失败是在真实 save 完成并观察到 header/items/word 后，测试 advisor 改写响应，证明服务面对“已有写入但返回失败”的回滚契约，不冒称自然业务路径已经触发该返回。trigger、夹具和清理均限制新建专用 `financial_cloud_e2e_20261003_it4_*` 库及本次 UUID 前缀；清理等待工作线程停止，不使用 TRUNCATE 或删除数据库。

## 仍保留的边界

同账套串行会增加等待，当前没有吞吐承诺。已有外层 RR 保留调用方隔离并可能保留范围/间隙等待；不同账套独立仅承诺六入口自己创建的 RC 事务。通用凭证写入、月结、恢复、批量外部写入和预览编辑未参与 book 协议，不应将本片表述为整个财务系统并发安全。配置/员工/模板并发变化也不是当前已证明的协议目标。缺迁移时 FORCE INDEX 明确失败，必须作为部署前提记录。

## 最终文件 SHA-256

下表在实现冻结、最终产物齐备后采集。若任何文件变化，本结论需按新差异重新核查。

| 相对路径 | SHA-256 |
|---|---|
| `financial-cloud/src/main/java/com/financial/cloud/repository/book/BookMapper.java` | `91c51223ca4386add5d7c08b498b9c5d3c4d71611390bd857ccbc58210e58afa` |
| `financial-cloud/src/main/java/com/financial/cloud/repository/hr/EmployeeSalaryMapper.java` | `6f6568e91f3e1fd23d3a88d704083a05d3939dbbdf897d716064c4514e4c53e6` |
| `financial-cloud/src/main/java/com/financial/cloud/service/hr/EmployeeSalaryService.java` | `9df605e52160e297249c799a356765fa4cbf41c0662cfabc08a1f68c5e323330` |
| `financial-cloud/src/main/java/com/financial/cloud/service/hr/EmployeeSalaryTempService.java` | `3c58b2fc5f9b05a5dfc485c523f9416752949401d7b40aa32ac66109c01f7280` |
| `financial-cloud/src/main/java/com/financial/cloud/service/hr/PayrollWriteLock.java` | `7c840ac93fc98495bf975392bbc6a1b67d0e88791a908ca44606cbfb3e4b51d2` |
| `financial-cloud/src/main/java/com/financial/cloud/service/voucher/VoucherService.java` | `5204de5e7520474ed08873df0637b754df06998ff8b8bfcdfe55dffbe3850582` |
| `financial-cloud/src/test/java/com/financial/cloud/service/hr/PayrollTransactionMysqlIT.java` | `9c8c750cbd6deb61e2378aa45d4f79b6350b722210a7e2ff550fda3f8cd4dd68` |
| `financial-cloud/src/test/java/com/financial/cloud/service/hr/PayrollWriteLockTest.java` | `2f4b1a2ed4327471796bf849111c2376dd4c67c988bf03abb382b740e089322e` |
| `financial-cloud/src/test/java/com/financial/cloud/service/hr/EmployeeSalaryServiceTest.java` | `6a2efdc5a3e416f498fb900a1ad9f9e9bc04634753b9d119b52fc8ab595cb58b` |
| `financial-cloud/src/test/java/com/financial/cloud/service/hr/EmployeeSalaryTempServiceTest.java` | `8b9c62fdc84148ab8bb40eedf3404790eebfe311996c09264429686ed52ad9a8` |
| `financial-cloud/src/test/java/com/financial/cloud/service/voucher/VoucherServiceTest.java` | `47e0014ac62731bc103af7b708af81d4172a1be9bf6bfffc0265e261f46a30a4` |
| `tools/test_payroll_live_voucher_sql.py` | `cbf214ef46e379d4a02de6a9682818d017a3a09623806ab7974685a3b02c01a1` |
| `sql/patches/2026-10-03-payroll-write-scope-index.sql` | `1ea25f2b15fc50b4fe20d1679e23433e1bdcacfa70c5ee9c66c00f72a0318fc2` |
| `tools/build_init_sql.py` | `d091e8aafa4b3fa924ca596334377bc31c325c1a1ce9b88a5da3b5c04d654572` |
| `sql/financial_cloud_init.sql` | `31fc7456fe4a62a3ba38ffbb22f6920410f8eefc48bbdb888dbb8f2bfbee474d` |
