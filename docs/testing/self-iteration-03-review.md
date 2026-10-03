# 自主迭代第 3 轮独立审查：有效工资凭证查重

审查日期：2026-10-03。审查者：独立代理 `release_risk_audit`，未参与本轮源码或测试实现。基线提交：`4571047d0b836848190e8952785e8590cee606d9`；审查对象为其上的未提交工作区，文件字节身份见文末。本审查仅写入本文件，没有修改源码、测试、POM、任务或其他文档，也没有启动服务、数据库、Maven、OpenSpec、网络访问或提交。

## 结论

**本切片未发现未解决的阻塞问题，符合 `payroll-live-voucher-dedupe` 的规格和限定范围。** 当前行失效关联不会再跳过有效 peer 查询；查询先排除失效凭证，再选择任一有效关联，最新失效关联不会遮蔽更早有效关联。实际 MySQL 多行查询证据与 Java 服务控制流证据分别成立，没有用 mock 查询结果代替 SQL 行为验证。

本结论允许该独立切片完成，不等于全部工资并发安全、现场历史数据已修复或候选版本可发布。并发生成、生成与修改/删除交错风险仍未关闭，应保留后续 R0 风险及发布评估，不因本切片通过而抹去。

## 审查范围与符合性

已读 active 变更的 proposal、design、tasks 和 `specs/payroll-smb/spec.md`，并核查两个生产文件、`EmployeeSalaryServiceTest.java`、`tools/test_payroll_live_voucher_sql.py` 的实际差异及执行证据。

| 要求 | 审查结果及位置 |
|---|---|
| 任一有效关联防重，失效关联不遮蔽有效关联 | Mapper 两查询按选定 FK JOIN `voucher`，要求 `v.deleted='n'` 后才 `ORDER BY/LIMIT 1`，见 `EmployeeSalaryMapper.java:42–70`。不存在和逻辑删除凭证均不进入候选。 |
| 当前行关联为空或失效时仍查 peer | `EmployeeSalaryService.java:429–438` 保留 own live 快速拒绝，其余两种状态都查询 live peer；命中即返回重复生成拒绝。 |
| 拒绝时不清理关联或创建凭证 | peer 拒绝在 stale 清理及后续凭证保存之前；新增服务回归验证无工资 update、无 voucher save，并保留所选及另一类型关联。 |
| 无有效关联时只清理当前行自身失效关联 | `EmployeeSalaryService.java:439–448` 仅在 own 非空、own 未命中有效且 peer 未命中时清理当前行所选字段；不清理其他历史工资行。own 为空时不再对当前行作无意义清理。 |
| 保留同账套、员工、月份、关联类型范围 | 工资侧 `bookId/employeeId/belongDate` 三条件保持；计提与发放查询分别使用各自 FK 列，服务保留原 `0/2` 与 `1/3` 分流。没有顺带调整既有模板类型口径。 |
| 保留工资历史与按 ID 判定有效的语义 | 未增加工资 `deleted='n'`、凭证状态/日期或凭证 `book_id` 过滤。历史软删工资关联仍防重；现有异常 foreign-book voucher FK 仍按 ID 判有效，测试明确覆盖这一保留行为，没有以本片静默修历史归属。 |

新增服务测试共 28 个参数化执行，覆盖 own stale/blank 命中有效 peer、own live 快速拒绝、无 live 时清理自身所选失效关联及 own 空时不清理历史行，包含全部四种既有类型分流。仅失效的放行测试到达既有模板查找，不冒称已经验证完整凭证保存或过账路径。

## 实际证据与验证边界

审查读取并核对了以下实际产物，没有另行运行这些命令：

| 证据 | 核对结果 |
|---|---|
| `.e2e-run/iteration3-sql-before.log` | 原 Mapper 注解 SQL 在同一夹具下运行 15 个测试方法：7 个离线边界方法和 8 个 MySQL 方法；6 个预期 subtest 失败，分别是两类关联的 missing、deleted、newest-stale 场景。 |
| `.e2e-run/iteration3-sql-after.log` | 修复后同组 15 个方法全部通过，无失败或跳过。MySQL 场景覆盖较新失效引用与较早 live、历史软删工资、工资侧三维 scope、所选关联类型、凭证全部现有状态及历史日期、foreign-book voucher ID 保留语义。 |
| `.e2e-run/iteration3-backend-tests.log` | 完整后端 `mvn -o test`：545 项，失败/错误/跳过均为 0，BUILD SUCCESS，35.792 秒，完成时间 `2026-10-03T12:30:58+08:00`。 |
| `financial-cloud/target/surefire-reports/TEST-*.xml` | 独立只读汇总 89 个 suite，545 项，失败/错误/跳过均为 0；`EmployeeSalaryServiceTest` 为 58 项，包含新增 28 项执行。 |

原查询只有失效关联时返回裸 stale ID，是旧查询契约；其中 missing/deleted 四个失败证明新 live-only 查询契约的变化，不能单独表述为四次重复记账复现。两类 newest-stale 失败才直接证明旧查询选择失效引用并遮蔽已有 live 的条件性漏洞。自身 stale 跳过 peer 的服务分支由源码顺序及 Java 控制流回归证明。本轮证据未查询现场数据，不证明生产已存在这些历史状态或发生金额事故。

### 查询来源与临时表安全

- SQL 脚本从指定 Java Mapper 方法的真实 `@Select` 文本块提取查询，没有维护第二份业务 SELECT；只将命名占位符按出现顺序转换为 PyMySQL 参数绑定，数据值不插入 SQL 字符串。
- 原 Mapper 保存在 `.e2e-run/iteration3-original-EmployeeSalaryMapper.java`，SHA-256 为 `0fb42901da65b4852cdad89869b2dee05a0d7ba7167ed8a093fab6b4e1acf101`。基线 Git blob 的 SHA-256 为 `d3cd909ed0047a3216f759c18e0145958e37d410959cd4edf985635eb6791272`；二者仅有 67 处 CRLF/LF 差异，归一换行后源码完全相同。保存字节哈希与 Git blob 哈希分别记录，未冒称它们字节相同。
- 数据库参数要求显式给出 host、port、name、user、password，库名仅允许 `financial_cloud_e2e*`；没有业务库默认值。连接后还检查实际 `DATABASE()` 与声明一致。
- `setUpClass` 在同一新连接中先创建 `TEMPORARY TABLE employee_salary` 和 `TEMPORARY TABLE voucher`，只有两表都建立成功后才运行夹具。所有 INSERT、DELETE、查询均复用该连接及无库限定的两表名；当前两条实际 SELECT 使用这两个临时表。若第二张表建立失败，不会进入夹具方法；已注册连接关闭清理。
- 脚本校验 SELECT 来源及表边界，拒绝库限定或其他表、额外 SELECT、函数表达式、注释、多语句及 SELECT 写文件/锁定形式。当前差异满足该边界。关闭连接即丢弃临时表，脚本没有 DROP/DELETE 持久账套表的路径。

### 参数绑定与证据限制

Mapper 两方法的 `@Param("bookId")`、`@Param("employeeId")`、`@Param("belongDate")` 与注解占位符完全一致；服务传入账套、工资行 employee ID 及 `YearMonth.toString()`，调用及字符串月份由 Java 回归验证，旧方法名无生产调用遗留。Python 绑定的顺序和重复参数由离线测试覆盖。

这是实际 MySQL 执行注解 SQL 的查询语义证据，**不是 MyBatis 运行时映射、完整工资 API/E2E、真实过账余额或并发事务验证**。临时表采用所需字段的简化结构，没有复刻生产表所有长度、排序规则或索引。静态绑定核查未发现当前参数名/映射风险，但没有将其表述为新增 MyBatis 集成测试已经运行。

## 仍未关闭的风险

生成查重与写回尚无锁、CAS 或业务唯一键；两个生成请求仍可能都通过查重，生成与直接修改/删除仍可能交错覆盖关联。这些是已列明的后续并发风险，本轮没有实现或验证其原子保护。生成后关联写入结果检查、历史批量修复及 `0/1` 模板口径也保持原范围，不计入本片完成声明。

本轮本地测试不绑定未来提交或远端 CI；真实完整月份会计试账、签字、第二实例恢复及正式部署授权仍依候选发布证据门槛处理。本审查未运行或复核第三轮的远端 CI、完整会计验收、全部账套隔离或生产构建，不把第一轮/第二轮证据改绑本轮源码。

## 最终审查的四个源文件 SHA-256

| 文件 | SHA-256 |
|---|---|
| `financial-cloud/src/main/java/com/financial/cloud/service/hr/EmployeeSalaryService.java` | `9e74e951c23e158e33e7659aacf50cdf504db9035325a4ce44b38dc9961ec003` |
| `financial-cloud/src/main/java/com/financial/cloud/repository/hr/EmployeeSalaryMapper.java` | `390b73c430c2c0a99d02325541af8ba585162dbd2296587e269707d7ebbb9b31` |
| `financial-cloud/src/test/java/com/financial/cloud/service/hr/EmployeeSalaryServiceTest.java` | `94b63047bd30d6843c18e6bda02e2cb6a949f3f6b14fb033b0e88785e22b00f8` |
| `tools/test_payroll_live_voucher_sql.py` | `cb4f9537ae513f3de66621a745dc23c2c0063a28221363579163229c308985c2` |

这些哈希描述本次审查的最终工作区字节。任一文件再次变更后，须复核实际差异并更新审查身份；本报告不覆盖未经复核的后续改动。
