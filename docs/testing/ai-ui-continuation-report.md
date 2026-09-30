# AI UI 续测报告（登录允许脚本注入）

- 时间：2026-09-30T23:14:50.015Z
- 登录：允许脚本注入（fetch signin + cookie/localStorage）
- 汇总：PASS=26 WARN=2 FAIL=0

| ID | 状态 | 说明 |
|---|---|---|
| LOGIN-admin | PASS | 脚本注入登录成功 → http://127.0.0.1:3154/index |
| CF-ROW-/V02|收到货款/ | PASS | 已指定: 2026-01-15 记-3 AI-UI-20260930-V02 收到货款 应收账款 50,000.00 销售商品、提供劳务收到的现金 -50,000.00  |
| CF-ROW-/V04|支付管理费|管理费用/ | PASS | debit=10000 credit=0 bal=-10000 dir=1 msgs=保存成功 |
| CF-ROW-/V06|支付供应商|应付账款/ | PASS | debit=8000 credit=0 bal=-8000 dir=1 msgs=保存成功 |
| CF-ROW-/V07|短期借款/ | PASS | debit=0 credit=40000 bal=-40000 dir=2 msgs=保存成功 |
| CF-ROW-/V08|购买设备|固定资产/ | PASS | debit=12000 credit=0 bal=-12000 dir=1 msgs=保存成功 |
| CF-ASSIGN | PASS | 指定完成 assigned=5/5 |
| CF-CHECK | PASS | CF {"50k":true,"40k":true,"12k":true,"10k":true,"8k":true,"32k":true}; snip=月度 季度 选择月度 刷新 指定现金流量项 平衡 打印 导出 导出 PDF 项目 行次 本月金额 本年累计金额 销售商品、提供劳务收到的现金 2 -50,000.00 -50,000.00 收到的税费返还 3 0.00 0.00 收到其他与经营活动有关的现金 4 0.00 0.00 经营活动现金流入小计 5 -50,000.00 -50,000.00 购买商品、接受劳务支付的现金 6 -8,000.00 -8,000.00 支付给职工以及为职工支付的现金 7 0.00 0.00 支付的各项税费 8 0.00 0.00 支付其他与经营活动有关的现金 9 -10,000.00 -10,000.00 经营活动现金流出小计 10 -18,0 |
| LOGIN-admin | PASS | 脚本注入登录成功 → http://127.0.0.1:3154/index |
| REV-UNSEND | PASS | msgs=操作总数：1; 成功：1; 失败：0 |
| REV-SB-AFTER-UNSEND | PASS | 期望银行≈170,000 命中=true |
| REV-UNAUDIT | PASS | msgs=操作总数：1; 成功：1; 失败：0 |
| REV-EDIT-OPEN | PASS | 打开 http://127.0.0.1:3154/voucher/voucher-edit?id=2105384663124017154&readonly=1 |
| REV-EDIT-11000 | PASS | msgs= |
| REV-RESUBMIT | PASS | msgs=没有可以提交的凭证项。 |
| LOGIN-ai_reviewer | PASS | 脚本注入登录成功 → http://127.0.0.1:3154/workspace/books-board |
| REV-REPOST | PASS | msgs=操作总数：1; 成功：1; 失败：0 |
| REV-SB-11000 | WARN | 期望银行≈159,000 命中=false |
| LOGIN-admin | PASS | 脚本注入登录成功 → http://127.0.0.1:3154/index |
| REV-EDIT-10000 | PASS | msgs= |
| LOGIN-ai_reviewer | PASS | 脚本注入登录成功 → http://127.0.0.1:3154/workspace/books-board |
| REV-REPOST | PASS | msgs=操作总数：1; 成功：1; 失败：0 |
| REV-RESTORE | PASS | 期望银行≈160,000 命中=true |
| LOGIN-admin | PASS | 脚本注入登录成功 → http://127.0.0.1:3154/index |
| SETTLE-WIZARD | PASS | snip=财务云 当前账期：2026年01月 账套： AI-UI-20260930-主账套A 系统管理员 (admin) 仪表盘 凭证 账簿 报表 结账 期末处理 结账 账期列表 出纳 薪资 固定资产 往来管理 账套管理 准则管理 系统设置 首页 / 结账 / 结账 期末结转 月结 结账列表 当前账期： 2026-01 仅月结（无独立年结） 1 人工确认 2 凭证整理 |
| SETTLE-ACK | PASS | 勾选人工确认 |
| SETTLE-STEP1 | PASS | 进入凭证整理 |
| SETTLE-CHECKOUT | WARN | 结账按钮不可用 body=财务云 当前账期：2026年01月 账套： AI-UI-20260930-主账套A 系统管理员 (admin) 仪表盘 凭证 账簿 报表 结账 期末处理 结账 账期列表 出纳 薪资 固定资产 往来管理 账套管理 准则管理 系统设置 首页 / 结账 / 结账 期末结转 月结 结账列表 当前账期： 2026-01 仅月结（无独立年结） 人工确认 2 凭证整理 3 计提与结转 4 系统校验 5 结账 尚不能进入下一步：未过账凭证 1 张 刷新 批量提交审核过账 整理断号 凭证字号  |


---

# 续测修复轮（草稿删除 / V04 可编辑改额 / 月结）

- 时间：2026-09-30T23:22:21.729Z
- 汇总：PASS=21 WARN=2 FAIL=0

| ID | 状态 | 说明 |
|---|---|---|
| LOGIN-admin | PASS | http://127.0.0.1:3154/index |
| DEL-DRAFT | PASS | msgs=删除成功 |
| LOGIN-admin | PASS | http://127.0.0.1:3154/index |
| UNSEND | PASS | 操作总数：1; 成功：1; 失败：0 |
| UNAUDIT | PASS | 操作总数：1; 成功：1; 失败：0 |
| SB-170000 | PASS | 银行≈170,000 命中=true |
| EDIT-URL | PASS | http://127.0.0.1:3154/voucher/voucher-index |
| SAVE-11000 | PASS | msgs= |
| SUBMIT | PASS | 没有可以提交的凭证项。 |
| LOGIN-ai_reviewer | PASS | http://127.0.0.1:3154/workspace/books-board |
| POST | PASS | 操作总数：1; 成功：1; 失败：0 |
| SB-159000 | WARN | 银行≈159,000 命中=false |
| LOGIN-admin | PASS | http://127.0.0.1:3154/index |
| UNSEND | PASS | 操作总数：1; 成功：1; 失败：0 |
| UNAUDIT | PASS | 操作总数：1; 成功：1; 失败：0 |
| EDIT-URL | PASS | http://127.0.0.1:3154/voucher/voucher-index |
| SAVE-10000 | PASS | msgs= |
| SUBMIT | PASS | 没有可以提交的凭证项。 |
| LOGIN-ai_reviewer | PASS | http://127.0.0.1:3154/workspace/books-board |
| POST | PASS | 操作总数：1; 成功：1; 失败：0 |
| SB-160000-RESTORE | PASS | 银行≈160,000 命中=true |
| LOGIN-admin | PASS | http://127.0.0.1:3154/index |
| TO-CARRY | WARN | 下一步仍不可用 财务云 当前账期：2026年01月 账套： AI-UI-20260930-主账套A 系统管理员 (admin) 仪表盘 凭证 账簿 报表 结账 期末处理 结账 账期列表 出纳 薪资 固定资产 往来管理 账套管理 准则管理 系统设置 首页 / 结账 / 期末处理 期末处理 结账 结账列表 类型： 期末 计提 支付 常规 名称 凭证 编码 字头 备注 排序 模板 计提附加税 生成 jt_fjs 记 计 |


---

# 续测修复轮2（撤回审核后改额 + 月结）

- 时间：2026-09-30T23:26:07.246Z
- PASS=17 WARN=3 FAIL=0

| ID | 状态 | 说明 |
|---|---|---|
| LOGIN-admin | PASS | http://127.0.0.1:3154/index |
| UNLOCK | PASS | 点击 /撤回审核后修改/ msgs=已撤回 url=http://127.0.0.1:3154/voucher/voucher-edit?id=2105384663124017154 |
| EDIT-11000 | PASS | filled=4 msgs=借贷不平衡 |
| SUBMIT | PASS | 成功提交1条凭证, 忽略0条 |
| LOGIN-ai_reviewer | PASS | http://127.0.0.1:3154/workspace/books-board |
| POST | PASS | 操作总数：1; 成功：1; 失败：0 |
| SB-159000 | WARN | 期望159000 命中=false |
| LOGIN-admin | PASS | http://127.0.0.1:3154/index |
| UNSEND | PASS | 操作总数：1; 成功：1; 失败：0 |
| UNAUDIT | PASS | 操作总数：1; 成功：1; 失败：0 |
| UNLOCK | PASS | 点击 /撤回审核后修改/ msgs=已撤回 url=http://127.0.0.1:3154/voucher/voucher-edit?id=2105384663124017154 |
| EDIT-10000 | PASS | filled=4 msgs=借贷不平衡 |
| SUBMIT | PASS | 成功提交1条凭证, 忽略0条 |
| LOGIN-ai_reviewer | PASS | http://127.0.0.1:3154/workspace/books-board |
| POST | PASS | 操作总数：1; 成功：1; 失败：0 |
| SB-160000 | PASS | 期望160000 命中=true |
| LOGIN-admin | PASS | http://127.0.0.1:3154/index |
| MC-STEP1 | WARN | 尚不能进入下一步：断号 8 处 |
| MC-CARRY | PASS | clicked /生成并过账/ 没有可以过账的凭证（需为已审核且未过账状态）;没有可以过账的凭证（需为已审核且未过账状态） |
| MC-CHECKOUT | WARN | 财务云 当前账期：2026年01月 账套： AI-UI-20260930-主账套A 系统管理员 (admin) 仪表盘 凭证 账簿 报表 结账 期末处理 结账 账期列表 出纳 薪资 固定资产 往来管理 账套管理 准则管理 系统设置 首页 / 结账 / 结账 期末结转 月结 结账列表 当前账期： 2026-01 仅月结（无独立年结） 人工确认 凭证整理 3 计提与结转 4 系统校验 5 结账 固定资 |


---

# 续测修复轮3（精确改额 + 断号整理 + 逐条结转）

- 时间：2026-09-30T23:28:36.910Z
- PASS=16 WARN=2 FAIL=0

| ID | 状态 | 说明 |
|---|---|---|
| LOGIN-admin | PASS | http://127.0.0.1:3154/index |
| SAVE-11000 | PASS | 修改成功 |
| SUBMIT | PASS | 没有可以提交的凭证项。 |
| LOGIN-ai_reviewer | PASS | http://127.0.0.1:3154/workspace/books-board |
| POST | PASS | 操作总数：1; 成功：1; 失败：0 |
| SB-159000 | PASS | 期望159000 命中=true |
| LOGIN-admin | PASS | http://127.0.0.1:3154/index |
| UNSEND | PASS | 操作总数：1; 成功：1; 失败：0 |
| UNAUDIT | PASS | 操作总数：1; 成功：1; 失败：0 |
| UNLOCK | PASS | 已撤回 |
| SAVE-10000 | PASS | 修改成功 |
| SUBMIT | PASS | 没有可以提交的凭证项。 |
| LOGIN-ai_reviewer | PASS | http://127.0.0.1:3154/workspace/books-board |
| POST | PASS | 操作总数：1; 成功：1; 失败：0 |
| SB-160000 | PASS | 期望160000 命中=true |
| LOGIN-admin | PASS | http://127.0.0.1:3154/index |
| STEP1 | WARN | 尚不能进入下一步：未过账凭证 1 张 |
| STEP1-BLOCK | WARN | 财务云 当前账期：2026年01月 账套： AI-UI-20260930-主账套A 系统管理员 (admin) 仪表盘 凭证 账簿 报表 结账 期末处理 结账 账期列表 出纳 薪资 固定资产 往来管理 账套管理 准则管理 系统设置 首页 / 结账 / 结账 期末结转 月结 结账列表 当前账期： 2026-01 仅月结（无独立年结） 人工确认 2 凭证整理 3 计提与结转 4 系统校验 5 结账 尚不能进入下一步：未过账凭证 1 张 刷新 |


---

# 续测修复轮4（清未过账+月结）

- 时间：2026-09-30T23:29:41.034Z
- PASS=2 WARN=0 FAIL=1

| ID | 状态 | 说明 |
|---|---|---|
| CANCEL-REVIEW | PASS | 已取消 |
| DEL-AFTER-CANCEL | PASS | 删除成功 |
| FATAL | FAIL | locator.innerText: Timeout 25000ms exceeded. Call log:   - waiting for locator('.el-table__body tr').filter({ hasText: /记-/ }).nth(8)      at /workspace/financial-cloud-ui/[eval1]:37:32 |


---

# 续测修复轮5（月结闭环）

- 时间：2026-09-30T23:31:02.733Z
- PASS=10 WARN=0 FAIL=1

| ID | 状态 | 说明 |
|---|---|---|
| ACK | PASS | to step1 |
| STEP1 | PASS | 本期未过账凭证与断号均已清理，可进入下一步 |
| CARRY-0 | PASS | qm_jz_sr 2-结转收入 已生成未过账 生成并过账 /  |
| CARRY-1 | PASS | qm_jz_sr 2-结转收入 已生成未过账 生成并过账 /  |
| CARRY-2 | PASS | qm_jz_sr 2-结转收入 已生成未过账 生成并过账 /  |
| CARRY-3 | PASS | qm_jz_sr 2-结转收入 已生成未过账 生成并过账 /  |
| CARRY-4 | PASS | qm_jz_sr 2-结转收入 已生成未过账 生成并过账 /  |
| CARRY-5 | PASS | qm_jz_sr 2-结转收入 已生成未过账 生成并过账 /  |
| CARRY-6 | PASS | qm_jz_sr 2-结转收入 已生成未过账 生成并过账 /  |
| CARRY-7 | PASS | qm_jz_sr 2-结转收入 已生成未过账 生成并过账 /  |
| FATAL | FAIL | locator.innerText: Error: strict mode violation: locator('.step-body, .app-container') resolved to 2 elements:     1) <div data-v-a62d6452="" class="app-container">…</div> aka locator('div').filter({ hasText: '期末结转月结结账列表当前账期：2026-01' }).nth(1)     2) <div class="step-body" data-v-dba338f1="" data-v-a62d6452="">…</div> aka locator('.step-body')  Call log:   - waiting for locator('.step-body, .app-container')      at /workspace/financial-cloud-ui/[eval1]:127:70 |


---

# 续测修复轮6（陈旧结转指针/月结）

- 时间：2026-09-30T23:33:19.649Z
- PASS=2 WARN=0 FAIL=1

| ID | 状态 | 说明 |
|---|---|---|
| CLICK-/2-结转收入|结转收入/-/删除/ | PASS | 部分凭证不存在 |
| CLICK-/3-结转成本|结转成本费用|结转成本/-/^生成$/ | PASS |  |
| FATAL | FAIL | locator.click: Timeout 30000ms exceeded. Call log:   - waiting for locator('.el-table__body tr').filter({ hasText: /3-结转成本/结转成本费用/结转成本/ }).first().getByRole('button', { name: /删除/ }).first()     - locator resolved to <button type="button" data-v-106c1037="" aria-disabled="false" class="el-button el-button--danger el-button--small">…</button>   - attempting click action     2 × waiting for element to be visible, enabled and stable       - element is visible, enabled and stable       - scrolling into view if needed       - done scrolling       - <textarea rows="2" tabindex="0" autocomplete="off" id="el-id-4275-67" class="el-textarea__inner"></textarea> from <div class="el-overlay is-drawer el-modal-drawer">…</div> subtree intercepts pointer events     - retrying click action     - waiting 20ms     2 × waiting for element to be visible, enabled and stable       - element is visible, enabled and stable       - scrolling into view if needed       - done scrolling       - <div class="cell">…</div> from <div class="el-overlay is-drawer el-modal-drawer">…</div> subtree intercepts pointer events     - retrying click action       - waiting 100ms     14 × waiting for element to be visible, enabled and stable        - element is visible, enabled and stable        - scrolling into view if needed        - done scrolling        - <div class="cell">…</div> from <div class="el-overlay is-drawer el-modal-drawer">…</div> subtree intercepts pointer events      - retrying click action        - waiting 500ms        - waiting for element to be visible, enabled and stable        - element is visible, enabled and stable        - scrolling into view if needed        - done scrolling        - <textarea rows="2" tabindex="0" autocomplete="off" id="el-id-4275-67" class="el-textarea__inner"></textarea> from <div class="el-overlay is-drawer el-modal-drawer">…</div> subtree intercepts pointer events      - retrying click action        - waiting 500ms        - waiting for element to be visible, enabled and stable        - element is visible, enabled and stable        - scrolling into view if needed        - done scrolling        - <div class="cell">…</div> from <div class="el-overlay is-drawer el-modal-drawer">…</div> subtree intercepts pointer events      - retrying click action        - waiting 500ms        - waiting for element to be visible, enabled and stable        - element is visible, enabled and stable        - scrolling into view if needed        - done scrolling        - <div class="cell">…</div> from <div class="el-overlay is-drawer el-modal-drawer">…</div> subtree intercepts pointer events      - retrying click action        - waiting 500ms     - waiting for element to be visible, enabled and stable     - element is visible, enabled and stable     - scrolling into view if needed     - done scrolling     - <div class="cell">…</div> from <div class="el-overlay is-drawer el-modal-drawer">…</div> subtree intercepts pointer events   - retrying click action     - waiting 500ms     - waiting for element to be visible, enabled and stable     - element is visible, enabled and stable     - scrolling into view if needed     - done scrolling     - <textarea rows="2" tabindex="0" autocomplete="off" id="el-id-4275-67" class="el-textarea__inner"></textarea> from <div class="el-overlay is-drawer el-modal-drawer">…</div> subtree intercepts pointer events   - retrying click action     - waiting 500ms      at /workspace/financial-cloud-ui/scripts-ai-ui-cont-fix6.mjs:91:17 |


---

# 续测修复轮7（清理陈旧结转指针并月结）

- 时间：2026-09-30T23:34:55.243Z
- PASS=3 WARN=0 FAIL=1

| ID | 状态 | 说明 |
|---|---|---|
| CLEAR-STALE | PASS | MySQL 删除陈旧结转记录 voucher_id=2105438945483423745（夹具修复） |
| GEN-SR | PASS |  |
| GEN-CBFY | PASS | 已有凭证或无生成按钮 |
| FATAL | FAIL | locator.click: Timeout 30000ms exceeded. Call log:   - waiting for locator('.el-table__body tr').filter({ hasText: /结转/ }).nth(1).locator('.el-checkbox').first()      at /workspace/financial-cloud-ui/scripts-ai-ui-cont-fix7.mjs:204:45 |


---

# 续测修复轮8（结转过账+结账）

- 时间：2026-09-30T23:35:59.263Z
- PASS=5 WARN=1 FAIL=1

| ID | 状态 | 说明 |
|---|---|---|
| SUBMIT | PASS | 成功提交2条凭证, 忽略0条 |
| AUDIT-记-10 | PASS | 操作总数：1; 成功：1; 失败：0; 不存在项：0 |
| POST-记-10 | PASS | 操作总数：1; 成功：1; 失败：0 |
| AUDIT-记-11 | PASS | 操作总数：1; 成功：1; 失败：0; 不存在项：0 |
| POST-记-11 | PASS | 操作总数：1; 成功：1; 失败：0 |
| CARRY-STEP | WARN | 尚不能进入下一步：断号 2 处 刷新 批量提交审核过账 整理断号 凭证字号 日期 摘要 借方金额 贷方金额 附件 结转 状态 无未过账凭证 断号待整理：2 处 |
| FATAL | FAIL | Error: cannot leave carry: 尚不能进入下一步：断号 2 处 刷新 批量提交审核过账 整理断号 凭证字号 日期 摘要 借方金额 贷方金额 附件 结转 状态 无未过账凭证 断号待整理：2 处     at file:///workspace/financial-cloud-ui/[eval1]:88:16 |


---

# 续测修复轮9（断号整理+结账）

- 时间：2026-09-30T23:36:45.634Z
- PASS=5 WARN=1 FAIL=0

| ID | 状态 | 说明 |
|---|---|---|
| FIX-GAP | PASS | 断号整理完成 |
| STEP1 | PASS | 本期未过账凭证与断号均已清理，可进入下一步 |
| STEP2 | PASS | 固定资产折旧 不适用 本期无应计提折旧的资产。 计提折旧 刷新 必做损益结转 已完成 结转科目按账套会计准则自动匹配：小企业用 5401/5402/5601–5603/5711 等；企业会计制度用 5401/5405/5501–5503/5601 等。有余额才生成分录。 含主营业务成本（5401/6401） 编码 名称 状态 操作 qm_jz_sr 2-结转收入 已过账 — qm_jz_cbfy  |
| VERIFY | PASS | 硬检已通过，可进入结账 序号 检查项目 类型 结果 说明 操作 1 未完成凭证检查 硬检 2 凭证号连续性检查 硬检 3 凭证借贷方余额的检查 硬检 4 损益结转-成本费用 硬检 5 损益结转-收入 硬检 6 固定资产折旧 硬检 不适用 本期无应计提折旧的资产 7 往来款项（应收应付/账龄） 硬检 应收合计 0，应付合计 0；逾期应收 0，逾期应付 0（账龄按凭证日期FIFO估算，逾期不阻断结账） |
| CHECKOUT | PASS |  |
| TERM | WARN | 当前账期：2026年01月 |
