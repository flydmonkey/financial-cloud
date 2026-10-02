# 四个业务模块账套隔离审计（2026-10-01）

## 结论

凭证、固定资产、工资、出纳均存在账套隔离缺口。常规业务列表在此次抽查中未返回外部账套记录，但按 ID 明细和写操作缺少归属校验；凭证模板列表还会直接混入其他账套模板。
此次是审计和待做项补充，未修改这四个模块的业务实现。前一轮通过的会计流程回归未包含这些跨账套安全断言。

## 实测确认

使用隔离数据库 `financial_cloud_e2e_20261001`、测试后端 2254、前端代理 3254。创建两个测试账套，切换到 A 后访问 B 的假记录，使用同一管理员会话。测试验证的是当前账套隔离；尚未使用只有单账套授权的用户验证权限升级。

| 模块 | 结果 |
|---|---|
| 凭证 | `GET /api/voucher/get/{B凭证ID}` 返回 B 数据；`PUT /api/voucher/update` 成功修改 B 凭证并将其 bookId 改成 A；模板 fetch 未传 relatedId 时包含 B 模板 |
| 固定资产 | `GET /api/fixed-asset/card/get/{B卡片ID}` 返回 B 数据；delete 传 B 卡片 ID 返回成功，随后明细为空，确认删除生效 |
| 工资 | 工资明细和员工 get 返回 B 数据（工资明细包含关联员工信息）；工资 update 传 B 工资 ID 返回成功，数据库确认基本工资变为 4321，bookId 被改为 A |
| 出纳 | 账户和流水 get 返回 B 数据；账户 update 成功改名并将 bookId 改为 A；账户迁移前，在 A 新增流水引用 B 账户 ID 成功，B 账户余额从 100 变为 107 |

此次六类明细对应的 fetch 列表未返回 B 的假记录。不能据此认定其他筛选、关联查询和汇总路径全部安全。

## 代码确认及后续覆盖

- 凭证：`VoucherService.queryById`（339 行）按主键查；update（659 行）未比较原凭证账套；submitBatch（535 行）、audit（761 行）、sender（871 行）载入 ID 集后未统一限定当前账套。后面三项尚未实测状态变更。
- 模板：`VoucherTemplateController` fetch 未注入 relatedId；Mapper 中 relatedId 是可选条件；get/save/delete 未校验当前账套。模板的公共标准数据需要与账套模板明确区分。
- 固定资产：`FixedAssetService.getById`（104 行）、update（315 行）、delete（347 行）未统一限定账套；分类服务有同类路径。copy/suspend/resume/dispose 已显式比较账套；盘点服务有 requireCheck 归属校验，仍需关联数据回归。
- 工资：`EmployeeSalaryService.update`（112 行）直接 updateById，delete（126 行）直接批量删除；员工 update/delete、个税抵扣和暂存工资明细需一并补齐。deleteVoucher（597 行）按薪资 ID 获取源记录，使用该记录账套删除凭证，而未先验证当前账套。
- 出纳：`JournalAccountService.update`（59 行）直接 updateById，delete（79 行）仅按 ID 删除；`JournalEntryService.save` 未验证 accId 对应账户归属，update/delete 也需源记录限定。余额更新不能仅依赖账户 ID。
- 当前 MyBatis 配置只有分页拦截器，没有自动账套过滤拦截器，因此上述主键操作不会自动补齐隔离条件。
- 凭证附件上传、下载、删除已有显式账套校验；反审核、反过账等部分路径也已有校验。不能把局部安全视作整个模块安全。

## 修复验收要求

从登录会话确定当前账套；读取和修改原记录时匹配 ID + bookId；禁止客户端覆盖原账套归属。批量操作先验证全部记录，拒绝包含外部账套 ID 的请求，避免部分成功。关联账户、科目、员工、类别及凭证均验证同账套归属。

每个入口都需验证 A 正常访问、B 正常访问、A 引用 B ID、客户端伪造 bookId、混合批量 ID、跨账套关联 ID；失败后源记录、关联凭证、余额不变。单账套权限用户的拒绝结果还需独立验证。

## 证据

`.e2e-run/book-isolation-audit.json` 保存 API 观察及数据库核对；`book-isolation-audit.log` 为运行日志。诊断脚本只收集结果，运行成功不表示安全验收通过。
临时复现文件为 `.e2e-run/book-isolation-audit.spec.ts`、`seed-isolation-audit.py`、`verify-isolation-audit.py`，配置为 `isolation-audit.config.ts`。这些假数据及脚本仅用于隔离库。

未修改原业务数据库，未提交、推送或部署。修复待做项已加入当前变更 tasks.md 第 6 节。
