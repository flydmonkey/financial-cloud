## Context

见 proposal.md 的原因。现有 HTTP controller 没有事务，账套归属检查会将 DTO（含批删 ListIdsDto）绑定服务端当前账套；工资 public 写方法和 VoucherService.save/delete 使用 REQUIRED 事务。第三轮已修复失效引用遮蔽有效关联，本轮以其未提交工作区为起点。MySQL 默认 REPEATABLE READ，单纯先读再等锁不会刷新一致性快照。

## Goals / Non-Goals

**Goals:** 在六个确认工资写入入口中实现同账套协调，等待后关键读取是 current read，生成失败不提交孤立凭证；用真实 Spring/MyBatis/MySQL 事务与数据库等待观察验证。

**Non-Goals:** 不锁整个财务系统，不协调通用凭证/结转/恢复等未参与入口，不增加业务表/唯一键或自动重试，不修正 0/1 模板口径，不批量修复历史资料。

## Decisions

- 新建共享 PayrollWriteLock，通过 BookMapper.lockActiveBookId 对已有未删 book 主键行 `FOR UPDATE`。六入口为 EmployeeSalaryService 的 update/save/delete/generateVoucher/deleteVoucher 和 EmployeeSalaryTempService.createFinalDetail，均在第一条业务 SQL 前取得锁，保持 book → salary → voucher 的顺序。使用服务端绑定 scope；缺失/不存在的账套及非本账套工资明确失败。选 book 锁而非单工资行锁，是为了覆盖不同 peer 和整月推送；employee 锁要求批量锁多个员工及排序，复杂度较高。
- 关键工资读取新增 selectActiveByIdForUpdate(bookId,id)、selectActiveByIdsForUpdate(bookId,ids)、selectActiveByMonthForUpdate(bookId,belongDate)。批次 ID 规范排序，SQL 按 id 排序；live peer 原查询改为 current locking read。锁定查询关闭缓存并刷新已有查询缓存，以支持已有外层 RR 快照的调用。推送使用锁定月工资列表检查关联，再沿用原删除/插入流程。
- 真实双事务证明：原工资表只有主键时范围锁扫描会阻塞其他账套，新增范围索引后 RR 索引边界锁仍造成等待。因此六入口新建事务指定 READ_COMMITTED，传播仍为 REQUIRED，不修改数据库默认隔离级别；已有外层 RR 事务继续加入并用 current read 保证判断正确，但可能保留调用方的范围/间隙等待。不同账套独立推进的保证限定这些入口自己新建的 RC 事务。新增非唯一 idx_salary_payroll_scope(book_id,belong_date,employee_id,created_date)，peer/月当前读指定 FORCE INDEX，避免全表扫描。不能通过 SKIP LOCKED 绕过未读工资或凭证。
- 生成先验证原 0/1/2/3 类型，再锁 book 并读取当前工资。删除独立 stale 清理步骤：失败模板等路径不再清理关联，成功最终写回覆盖当前选定 stale 链接即可。VoucherService.save 非成功时标记事务回滚；最终按工资 id/book 写回失败抛 BusinessException，异常由现有事务回滚。成功仍只写所选关联。
- RC + 范围索引的初版仍被真实 lock-wait 证明扫描并等待另一账套工资记录；STRAIGHT_JOIN 固定工资 scope 在先、凭证主键检查在后。进一步通过真实 EXPLAIN FOR CONNECTION 定位到 Backward index scan 等待 scope 之前的其他账套记录，因此去掉仅用于挑选最新记录的 ORDER BY；业务只判断 any-live 是否存在，不使用返回 ID 的时间顺序。工资解除关联调用专用 VoucherService.deletePayrollVoucher（MANDATORY），复用原删除流程但凭证头与分录改为 current locking read，以覆盖外层 RR 已建立旧快照后删除刚提交工资凭证及现金流关系。
- 测试类 PayrollTransactionMysqlIT 只在显式配置的新 `financial_cloud_e2e*` 命名空间执行；默认单测不发现 IT，缺少环境时失败而非跳过。使用真实 MyBatis Mapper、Spring 事务代理及 VoucherService.save 核心；非账务配置/模板/预览输入可固定。闩锁只暂停真实 SQL，不改锁查询或返回值；第三连接观察 MySQL lock waits 后放行，验证最终工资与凭证头/分录/字号。
- 将本轮前源码完整保存为本机快照，在同一测试夹具上运行旧版本反证，记录预期失败；双连接共用新数据库中的专用共享表，不能使用每连接私有的临时表。库与夹具操作严格限定新建测试命名空间，不使用真实账务资料。

## Risks / Trade-offs

- [同账套工资写入串行化增加等待] → 保持事务内无网络/人工等待，覆盖不同账套独立推进，较大规模吞吐在 R2 实测，不承诺固定性能提升。
- [未参与写入口可能仍产生交错] → 记录协议参与边界，不能将本片结果表述为全系统并发安全；后续审计通用凭证、结转、恢复协调。
- [RR 旧快照或 MyBatis 查询缓存造成过期判断] → 关键工资/peer/月检查使用 current locking read 且关闭缓存，专设已有外层快照回归。
- [真实并发测试权限不足] → 实查指定库创建与 lock-wait 观察权限，缺少能力时保留失败，不用线程延时或 mock 当完成证据。

## Migration Plan

已有数据库须先执行 sql/patches/2026-10-03-payroll-write-scope-index.sql，并确认该非唯一索引的四列和顺序；新库由 tools/build_init_sql.py 纳入同一补丁。本片不修改历史数据，也不增加业务表或唯一键。缺索引时 FORCE INDEX 明确失败，不能静默退回扩大锁扫描。应用版本回滚恢复本片代码，范围索引可保留，旧版并发风险随之恢复；不得把回滚描述为消除历史孤立凭证。正式部署仍依候选证据门槛及授权执行。测试命名空间与来源快照按本轮记录保留，清理只限明确创建的测试资源。
