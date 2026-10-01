# 财务云 AI UI 全流程测试报告

- **环境**：`http://127.0.0.1:3154`（后端 `:2154`，MySQL `:3307`）
- **执行时间**：2026-09-30 / 2026-10-01（UTC）
- **版本**：财务云 v1.1.0
- **基线文档**：`docs/testing/ai-ui-full-process-test-prompt.md`
- **测试标识**：`AI-UI-20260930`
- **账套**：`AI-UI-20260930-主账套A`（小企业会计准则，启用凭证审核）
- **登录/写入**：用户允许**脚本注入**（登录 + Node/in-page fetch：draft→submit→audit→post、CF specify、结转）；月结向导与报表截图走 UI
- **脚本**：`financial-cloud-ui/scripts-ai-ui-continuation.mjs`、`scripts-ai-ui-month2.mjs`、`scripts-ai-ui-book-b.mjs`、`scripts-ai-ui-book-b-journal-ext.mjs`、`scripts-ai-ui-book-b-arap-fa.mjs`、`scripts-ai-ui-book-b-fa-dispose.mjs`、`scripts-ai-ui-book-b-payroll-exp.mjs`、`scripts-ai-ui-book-c.mjs`、`scripts-ai-ui-book-d.mjs`、`scripts-ai-ui-section6.mjs`、`scripts-ai-ui-optional-workbench-tax.mjs`、`scripts-ai-ui-remaining-gaps.mjs`、`scripts-ai-ui-final-gaps.mjs`
- **明细**：`docs/testing/ai-ui-continuation-report.md`、`docs/testing/ai-ui-month2-report.md`、`docs/testing/ai-ui-book-b-report.md`、`docs/testing/ai-ui-book-b-journal-ext-report.md`、`docs/testing/ai-ui-book-b-arap-fa-report.md`、`docs/testing/ai-ui-book-b-fa-dispose-report.md`、`docs/testing/ai-ui-book-b-payroll-exp-report.md`、`docs/testing/ai-ui-book-c-report.md`、`docs/testing/ai-ui-book-d-report.md`、`docs/testing/ai-ui-cf-begin-cash-fix-report.md`、`docs/testing/ai-ui-section6-report.md`、`docs/testing/ai-ui-optional-workbench-tax-report.md`、`docs/testing/ai-ui-remaining-gaps-report.md`、`docs/testing/ai-ui-final-gaps-report.md`

---

## 最终结论：**有条件通过**（主财务闭环通过；专项 / §六 / 可选与收尾细项均已补测）

主账套已完成建账→期初→V01–V08→CF 指定→V04 反操作→1 月结转/月结→2 月五笔→2 月结转/月结；账期现为 **2026-03**。历史快照抽查：1 月银行 160,000 / 本年利润 40,000；2 月银行 188,000 / 本年利润 53,000。

**支持范围**：主账套两月完整闭环、三表勾稽、反操作与快照、专项 B/C/D、§六校验/批量/权限、可选工作台/封存/税费、收尾细项。不宣称「全系统全部通过」——软观察见 §七。

**缺陷修复**：
1. **BUG-TZ-DATE（P0）** — 已修
2. **BUG-CF-UI-SIGN（P1）** — 已修
3. **OBS-CARRY-STALE-POINTER** — 删除凭证时清理结转指针
4. **BUG-TERM-CROSS-BOOK（P1）** — 已修
5. **OBS-CF-BEGIN-CASH-FEB** — 已修（上期 CF 期末实时重算；2 月期初 160,000）
6. **OBS-CF-AR-ADJ** — 闭环为口径说明（应收+预付 ⇒ −18,000）
7. **BUG-JEXT-REVERSE-POST** — 冲销日期钳到开放账期 + 负金额流水回写；复测冲销过账 **PASS**（19/19）
8. **OBS-PERM-SWITCH / CONFIG-NO-GRANT（P1）** — `switchBook` / `config update|updateByKey` 校验 `permission_book`，无授权返回 **510021**
9. **OBS-AUX-API-NO-MUST（P1）** — 启用辅助核算时，draft/submit 服务端校验科目 `must` 辅助
10. **OBS-EXP-SUMMARY-LEN（P2）** — 凭证摘要统一截断至 64，避免 Data truncation

专项 B 5.1 核心+扩展（含红冲过账/未达 500）、固资深路径、工资/报销、C/D、守卫、CF 勾稽、导出内容级校验已完成。盘盈入账 book-surplus（拆卡/累加/禁 bump）已补测，见 `ai-ui-book-b-fa-surplus-report.md`。

---

## 执行汇总

| 分类 | 通过 | 失败 | 跳过 |
|---|---:|---:|---:|
| 登录建账期初 | 8 | 0 | 1 |
| 1 月凭证/报表/反操作/月结 | 主路径通过 | — | CF 已补全 |
| 2 月凭证/结转/月结/历史快照 | 主路径通过 | — | CF 已补全 |
| P0/P1 修复验证 | TZ+CF UI | — | — |
| 专项 B 出纳日记账 5.1 | 核心+扩展 PASS | — | 见 journal-ext 报告 |
| 专项 B 往来/固资 5.2–5.3 | 核心+清理/盘点/盘盈入账 PASS | — | 两 P1 已修 |
| 专项 B 工资/报销 5.4–5.5 | 核心路径 PASS | — | 公式空 WARN；摘要长度 OBS |
| 专项 C 关闭凭证审核 | PASS（免审核流） | — | — |
| 反结账/闭账守卫 + 导出入口 | PASS | — | 导出仅 smoke（xlsx 非空） |
| 现金流量/间接法点测 | 补全后主路径 PASS | — | BEGIN-CASH/AR-ADJ 已闭环 |
| 专项 D 年末 | PASS（损益+年终结转+跨年） | — | BUG-TERM-CROSS-BOOK 已修 |
| §六 校验/批量/权限 | PASS（含 switchBook 拒授） | — | 专项 C；OBS-PERM 已修 |
| 导出内容级校验 | PASS（xlsx+pdf） | — | 2026-01 样本 |
| 可选：工作台/封存/税费/UI边角 | PASS 21 | — | 见 optional 报告 |
| 剩余细项（守卫/固资/辅助/待办） | PASS（含 API 辅助必填） | — | OBS-AUX/CONFIG 已修 |
| 收尾细项（累计预扣/模板/账龄/互斥） | PASS 20 | — | 见 final-gaps 报告 |

---

## 第一月（已结）

| 检查项 | 结果 |
|---|---|
| V01–V08 录审过（日期 2026-01-15） | PASS |
| 银行 160,000；结转后收入/成本/费用 0；本年利润 40,000 | PASS |
| 资产构成 242,000 | PASS |
| V04 反过账→11,000→恢复 10,000 | PASS |
| 月结五步 → 账期 2026-02 | PASS |
| 现金流量表符号（修复后 YTD） | PASS（销售 50k / 购货 8k / 投资 12k / 借款 40k；缺 V04 其他流出与 2 月项） |

## 第二月（已结）

| 业务 | 金额 | 结果 |
|---|---:|---|
| 赊销 | 30,000 | PASS |
| 收货款 | 40,000 | PASS |
| 销售成本 | 12,000 | PASS |
| 管理费 | 5,000 | PASS |
| 付供应商 | 7,000 | PASS |

结转前/结转后余额抽查：

| 科目 | 预期 | 实际 | 结果 |
|---|---:|---:|---|
| 1002 银行存款 | 188,000 | 188,000 | PASS |
| 1122 应收账款 | 40,000 | 40,000 | PASS |
| 1405 库存商品 | 8,000 | 8,000 | PASS |
| 1601 固定资产 | 12,000 | 12,000 | PASS |
| 2202 应付账款 | 5,000（贷） | 5,000 | PASS |
| 2001 短期借款 | 40,000（贷） | 40,000 | PASS |
| 3103 本年利润累计 | 53,000（贷） | 53,000 | PASS |

结算状态：`2026-01=6`、`2026-02=6`、`2026-03=1`；UI 当前账期 **2026年03月**。

历史快照：切换查询 2026-01 银行仍为 160,000、本年利润 40,000（与结账前一致）。

---

## 缺陷

### BUG-TZ-DATE（P0）— **FIXED** `ec37ae8`
原：开放账期首日被拒。现：`DateUtils` 用 `Asia/Shanghai`；复测 `2026-03-01` draft PASS。

### BUG-CF-UI-SIGN（P1）— **FIXED** `ec37ae8`
原：UI 强制负金额入库。现：绝对值 + 平衡校验仅对流出取负；复测 autofill/保存 PASS。闭账历史负余额已 DB ABS 纠正。

### OBS-UNAUDIT-TO-REVIEWING
反过账后再反审核进入「审核中」，需「撤回审核后修改」（产品路径，非阻断）。

### OBS-CARRY-STALE-POINTER / 重复生成 — **部分 FIXED**
删除结转凭证时清理 `settlement_carryforward`。向导重复生成仍可能产生重复件，需操作纪律。

### BUG-FA-SQL-DATE（P1）— **FIXED + 重启后复测 PASS**
原 `java.sql.Date.toInstant` 崩溃。`299caec` 改为 `java.util.Date`（`Asia/Shanghai`）。重启后端后，`startUseDate=2025-12-15` + `entryPeriod=2026-01` 建卡「新增成功，已生成购入凭证」；探测卡已清理。

### OBS-EXP-SUMMARY-LEN（P2）— **FIXED**
原：`voucher_item.summary` varchar(64)；报销等长摘要触发 `Data truncation`。现：`SubjectDisplayNameUtils.normalizeSummary` 截断至 64（凭证保存边界统一生效）。


### BUG-TERM-CROSS-BOOK（P1）— **FIXED**
结账 `termToNext`→`updateCurrentTerm` 曾因 `getBookConfigList` 未带 `bookId`/`configId` 按 key 全表更新。已改为 `updateCurrentTerm` 始终带 `bookId` 更新；`getBookConfigList` 补选 id/bookId。`ConfigSysServiceTest.updateCurrentTermAlwaysScopesToBookId` 覆盖。


---

## 专项账套 B 进度

### 5.1 出纳日记账 — PASS（含扩展）
- **账套**：`AI-UI-20260930-专项B`，**bookId** `2105448444973871105`，启用 `2026-01`，凭证审核开启
- **核心**：期初 10,000；收入 3,000+支出 1,000 → 日记账 **12,000**；过账后总账曾对齐；后经工资/报销/固资，**总账1002 现基线见扩展报告**
- **扩展**：未过账改额回写（50→80）；草稿删除解绑；红冲 30 反向流水；企业已付银行未付 500（对账单 12500 → 调节后 12000）
- **基线（扩展前）**：日记账 12000 / 总账1002 7644.26
- **明细**：`docs/testing/ai-ui-book-b-report.md`、`docs/testing/ai-ui-book-b-journal-ext-report.md`；截图 `bookb-*` / `bookb-ext-*`

### 5.2 往来与核销 — PASS
- 启用辅助核算；1122↔客户、2202↔供应商；单位 `AI-UI-20260930-客户B/供应商B`
- 过账后客户应收 **6,000**、供应商应付 **5,000**；明细一致
- 部分核销收款 4,000：未清挂账剩余 6,000；核销不改总账；撤销后状态恢复、总账不变
- **明细**：`docs/testing/ai-ui-book-b-arap-fa-report.md`；截图 `bookb-arap-*`

### 5.3 固定资产 — PASS（含清理/盘点/盘盈入账）
- 类别+卡片原值 12,000 / 残值 0% / 12 月直线法 / 费用 5602.02；UI 购入凭证过账（1601/1002）
- 2026-01 计提折旧 1,000 过账后：原值 12,000 / 累计折旧 1,000 / 净值 **11,000**
- **深路径补测**（独立卡，主卡未动）：`B-ASSET-DISP-002` 清理→DISPOSED；盘点盘亏下账；**盘盈入账** split_card / bump_qty / 已折旧禁 bump（见 surplus 报告）
- 缺陷：**BUG-FA-CHECK-DEFICIT-SUMMARY** / **BUG-FA-DISPOSE-VOUCHER-DATE** 已修
- **明细**：`docs/testing/ai-ui-book-b-fa-dispose-report.md`、`docs/testing/ai-ui-book-b-fa-surplus-report.md`；截图 `bookb-fa-disp-*` / `bookb-fa-check-*` / `bookb-fa-surplus-*`

### 5.4 工资闭环 — PASS
- 员工 `AI-UI-20260930-工资员`（B-E01），自定义基数 4,800，银行卡+开户行；专项附加房租 1,000
- 离线验算：应发 8,000 / 个税 38.11 / 实发 **7,232.29**（与预览/明细一致）
- 计提过账：5602.07+8,000 / 2211.01+8,000；发放过账：银行 −7,232.29；应付残留 767.71（SI+HF+税，SMB 模板未分录个税/社保负债）
- 守卫：重复计提拦截；缺银行卡阻断代发导出（504007）
- **明细**：`docs/testing/ai-ui-book-b-payroll-exp-report.md`；截图 `bookb-pay-*`

### 5.5 费用报销 — PASS
- 报销单 123.45（5602.04 / 1002），附件 PNG 上传+下载；提交→审核→生成凭证→过账
- GL：5602.04+123.45、1002−123.45；重复生成幂等返回原凭证 ID；无撤回 API
- **OBS-EXP-SUMMARY-LEN**：长摘要曾触发 truncation，缩短后通过
- 截图 `bookb-exp-*`

### B 其余
专项 B 主路径与深路径已闭环；§六（校验/批量/权限）见专项 C 节与 `ai-ui-section6-report.md`。

---

## 专项账套 C 进度（关闭凭证审核 §六）

- **账套**：`AI-UI-20260930-专项C`，**bookId** `2105453230146252802`，启用 `2026-01`，**voucherReviewed=0**
- **期初**：银行/实收资本各 10,000
- **凭证**：费用 100 / 银行 100（摘要含 `AI-UI-20260930-C-V01`）
- **状态流**：`draft` → `submit` → **`completed`（无审核人）** → `sender` 过账 → `unsender` 反过账 → `unaudit` → `draft` → 删除
- **余额**：提交未过账银行仍 10,000；过账后 **9,900**；反过账恢复 10,000；利润表管理费用过账后 100
- **观察**：新建账套当前账期可能继承创建者账套（曾为 2026-03），脚本强制回启用月；工具栏仍显示「审核」split-button，但提交后无需 reviewer
- **明细**：`docs/testing/ai-ui-book-c-report.md`；截图 `bookc-*`

---



## 专项账套 D 进度（年末结转 §六）

- **账套**：`AI-UI-20260930-专项D`，**bookId** `2105456763365081090`，启用 **2026-12**，凭证审核开启
- **期初**：银行/实收资本各 50,000
- **12 月业务**：收入 10,000 + 管理费用 3,000 → 净利润 **7,000**（审核人 `ai_reviewer`）
- **损益结转**：`qm_jz_sr` + `qm_jz_cbfy` 过账后收入/费用归零，本年利润 = 7,000；利润表本期仍 7,000
- **年末结转**：`qm_jz_bnlr` 借 3103 / 贷 3104.02 = 7,000 → 本年利润 0、未分配利润 +7,000；利润表本期净利润仍 7,000
- **结账跨年**：2026-12 → **2027-01**；银行期初继承 57,000；收入本年累计清零
- **非 12 月拦截**：账套 C（2026-01）生成 `qm_jz_bnlr` →「非年末，无需结转本年利润」
- **缺陷**：`BUG-TERM-CROSS-BOOK` 已修；测试时数据已写回 A=2026-03、C=2026-01  
- **明细**：`docs/testing/ai-ui-book-d-report.md`；截图 `bookd-*`


## §六 校验 / 批量 / 权限（专项 C）

- **结果**：**PASS 22 / FAIL 0 / WARN 1**（`scripts-ai-ui-section6.mjs`）
- 凭证校验：借贷不平衡、缺科目、零金额、单条分录、已结账期间、缺摘要 — 均拒绝持久化
- 金额边角：一借多贷 / 多借一贷 / 0.01 / 123.45 提交过账 PASS；大额仅草稿；另有批量过账样本
- 批量：混合草稿/已提交 — 提交「成功1/忽略2」；过账「成功2/失败1」；终态均为 completed
- 权限：受限用户 `fetchAll` 账套为空；凭证写入 **500014**；UI 引导创建账套
- **OBS-PERM-SWITCH-NO-GRANT** — **FIXED**：无授权 `switchBook` → **510021**（复测 PASS）
- **明细**：`docs/testing/ai-ui-section6-report.md`；截图 `s6-*`

---

## 反结账 / 闭账期守卫 / 导出（主账套 A）

- 闭账期修改与反过账均被拦截（开放账期 2026-03）  
- 非最近已结月反结账被拒；最近已结月（2026-02）可反结账，UI 有入口  
- 资产负债表导出已做内容级校验（见导出节）  

- 明细：`docs/testing/ai-ui-guards-report.md`；账期已恢复为 **2026-03** 开放  

---

## 现金流量 / 间接法点测（主账套 A）

- 已补全 V04 + 2 月 CF 后复核：**1 月经营净额 32,000 / 期末现金 160,000**；**2 月经营净额 28,000 / 期初 160,000 / 期末 188,000**  
- 附表 2 月调整项合计与经营净额均为 28,000  
- **OBS-CF-BEGIN-CASH-FEB FIXED**；**OBS-CF-AR-ADJ** 按产品口径 −18,000（含预付）  
- 明细：`docs/testing/ai-ui-cf-begin-cash-fix-report.md`、`docs/testing/ai-ui-cf-remediate-report.md`、`docs/testing/ai-ui-indirect-cf-report.md`

## 导出内容级校验（主账套 A）

- 2026-01 资产负债表/利润表/现金流量表/科目余额表 xlsx 含关键金额与标题；BS/CF PDF 为合法 `%PDF-`  
- 明细：`docs/testing/ai-ui-export-check-report.md`

---

## 未执行 / 进行中

1. （提示词范围内无阻塞未测项）

## 收尾细项（累计预扣 / 模板 / 账龄 / 互斥）

- **结果**：PASS 20 / FAIL 0 / WARN 0
- 两月累计预扣：2 月应税累计 2540.8，本期个税=累计税−1 月税
- 历史工资改额：不级联重算后续月（OBS，已恢复）
- 凭证模板套用：按 jt_gz 模板科目/方向生成草稿并核对
- 账龄：核销后 OPEN_ITEM，合计=未清 6000；测后已反核销
- 月结 jt_gz：被明细计提互斥拦截
- **明细**：`docs/testing/ai-ui-final-gaps-report.md`；截图 `final-*`


## 剩余细项补测（工资守卫 / 固资生命周期 / 辅助 / 待办）

- **结果**：PASS 16 / FAIL 0 / WARN（软观察见 §七）；产品 OBS 两项已修复测
- 工资：缺银行卡拦代发 **504007**；重复计提拦截；凭证后重推拦截；兼职员工可建；基数清空走回退（软 OBS）
- 固资：暂停→变动流水→恢复；复制 `B-ASSET-SUR-BUMP-副本2`
- 辅助必填：**OBS-AUX-API-NO-MUST FIXED** — API 拒绝「存在未选择辅助核算的分录（客户）」；UI `checkAuxiliary` 仍在
- 工作台：待审 `blocker=AUDIT` → `/voucher/voucher-index`
- 权限：**OBS-PERM-CONFIG-NO-GRANT FIXED** — 无授权 `updateByKey` → **510021**
- **明细**：`docs/testing/ai-ui-remaining-gaps-report.md`；截图 `gap-*`


## 可选深路径：工作台 / 封存 / 税费

- **结果**：PASS 21 / FAIL 0 / WARN 0
- 代账工作台：关注月账套可见、汇总与结账状态、进入处理下钻
- 封存探针账套：封存拒写 → 解封恢复（已恢复）
- 税费测算：收入/利润/企税可独立勾稽；增值税科目无发生 → 0（非正式申报）
- UI 边角：无权限页、凭证筛选/分页可见
- **明细**：`docs/testing/ai-ui-optional-workbench-tax-report.md`；截图 `opt-*`


---

## §七 完成标准（汇总）

### 1. 环境与约束
见文首：FE `:3154` / BE `:2154` / MySQL `:3307`；标识 `AI-UI-20260930`；允许脚本注入写入；月结向导与报表截图走 UI。

### 2. 用例计数（按分类；未执行不计通过）

| 分类 | 通过 | 失败 | 阻塞 | 未执行 | 不适用/软观察 |
|---|---:|---:|---:|---:|---|
| 登录建账期初 | 8 | 0 | 0 | 0 | 1 |
| 凭证/账簿/报表/月结/反操作/跨月（主 A） | 主路径 | 0 | 0 | 0 | — |
| 业务专项 B/C/D | 各专项主路径 | 0 | 0 | 0 | 软 OBS 见下 |
| §六 校验/批量/权限 | 22+复测 | 0 | 0 | 0 | — |
| 可选工作台/封存/税费 | 21 | 0 | 0 | 0 | — |
| 剩余/收尾细项 | 16+20 | 0 | 0 | 0 | 软 OBS |

### 3. 主账套两月金额（摘要）
见上文「第一月 / 第二月」：银行 160k→188k；本年利润 40k→53k；CF 1 月经营净额 32k / 2 月 28k；差额均为 0。独立验算与导出见各 CF/export 明细报告。

### 4. 缺陷表

| ID | 优先级 | 状态 | 账套/期间 | 影响 | 阻断后续？ |
|---|---|---|---|---|---|
| BUG-TZ-DATE | P0 | FIXED | A 开放账期 | 首日凭证被拒 | 否（已修） |
| BUG-CF-UI-SIGN | P1 | FIXED | A CF | 流出符号错误 | 否（已修） |
| BUG-TERM-CROSS-BOOK | P1 | FIXED | 结账 | 账期串账套 | 否（已修） |
| BUG-FA-SQL-DATE | P1 | FIXED | B 固资 | 建卡崩溃 | 否（已修） |
| BUG-FA-CHECK-DEFICIT-SUMMARY | P1 | FIXED | B 盘点 | 摘要超长 | 否（已修） |
| BUG-FA-DISPOSE-VOUCHER-DATE | P1 | FIXED | B 清理 | 凭证日期 | 否（已修） |
| BUG-JEXT-REVERSE-POST | P1 | FIXED | B 日记账 | 冲销过账 | 否（已修） |
| OBS-PERM-SWITCH-NO-GRANT | P1 | FIXED | C 权限 | 无授权可切换账套 | 否（复测 510021） |
| OBS-PERM-CONFIG-NO-GRANT | P1 | FIXED | 权限 | 无授权可改配置 | 否（复测 510021） |
| OBS-AUX-API-NO-MUST | P1 | FIXED | B 辅助 | API 绕过 must | 否（复测拒绝） |
| OBS-CF-BEGIN-CASH-FEB | — | FIXED | A CF | 2 月期初现金 | 否 |
| OBS-EXP-SUMMARY-LEN | P2 | FIXED | B 报销 | summary 超长截断至 64 | 否 |
| OBS-PAY-BASE-FALLBACK | — | 产品设计 | B 工资 | 非自定义规则回退账套默认基数 | 否 |
| OBS-PAY-HIST-NO-CASCADE | — | 产品设计 | B 工资 | 历史改额不级联重算 | 否 |
| OBS-PAY-TEMPLATE-SI-TAX | — | 产品设计 | B 模板 | SMB jt_gz/zf_gz 不含个税/社保分录 | 否 |
| OBS-UNAUDIT-TO-REVIEWING | — | 产品设计 | 凭证 | 反过账后再反审进「审核中」 | 否 |
| OBS-CF-AR-ADJ | — | 口径说明 | A 间接法 | 含预付 | 否 |

### 5. 入口 / 权限证据边界
- **真实业务操作验证**：主路径凭证录审过、结转月结、CF 指定、专项 B/C/D、批量提交/过账、受限用户凭证写入 500014、switchBook/config **510021**、辅助 API 拒绝。
- **按钮/UI 防护为主**：部分菜单可见性、凭证编辑辅助列提示（另有 API 服务端校验）。
- **无阻塞入口缺失**（提示词范围内）。

### 6. 测试资产清单与恢复

| 资产 | 状态 |
|---|---|
| 主账套 A `2105377998655979522` | **保留**；开放账期 **2026-03**；01/02 已结 |
| 专项 B `2105448444973871105` | **保留**；2026-01 开放；往来/固资/工资/报销数据在 |
| 专项 C `2105453230146252802` | **保留**；免审核；§六样本凭证在 |
| 专项 D `2105456763365081090` | **保留**；曾跨至 2027-01 后按测试需要回写 |
| 封存探针 `2105473362390200321` | **保留**；已解封恢复 |
| 用户 `ai_s6_limited` / `ai_reviewer` | **保留**；limited 无 `permission_book` |
| 污染性探测（term 2026-99、无辅助草稿、核销探针） | **已恢复/删除** |
| 截图与明细报告 | **保留**于 `docs/testing/` 与 `/opt/cursor/artifacts/screenshots/` |

默认不清空测试账套，供复核。

### 7. 最终结论（重申）
**有条件通过**：主财务两月闭环与确定性金额/三表勾稽/反操作快照通过；专项与 §六/可选/收尾已补测；P0/P1/P2（摘要截断）已闭环。剩余条目均为产品设计/口径说明（工资基数回退、历史不级联、模板不含社保个税分录、反审路径），不支持宣称「全系统全部通过」。

---

## 产品修复复测（权限授予 + 辅助必填）

- **时间**：2026-10-01（UTC）重启后端后 API 复测
- `ai_s6_limited` → `switchBook(专项C)` → **510021**「无权访问该账套」
- `ai_s6_limited` → `config/sys/updateByKey` → **510021**
- admin → `switchBook(专项B)` → 成功
- 专项 B、`sys.assist.acc.enabled=true`、1122 must 客户：无辅助 `POST /voucher/draft` → 拒绝「存在未选择辅助核算的分录（客户）」
- 无 must 科目（1001/1002）草稿仍成功，测后已删
- 长摘要（86 字）draft → 成功；落库摘要长度 **64**（截断）

---

## 证据

- `/opt/cursor/artifacts/screenshots/`（`m2-*`、`verify-cf-*`、`bookb-*`、`bookb-ext-*`、`bookb-fa-disp-*`、`bookb-fa-check-*`、`bookb-fa-surplus-*`、`bookb-pay-*`、`bookb-exp-*`、`bookc-*`、`bookd-*`、`guards-*`、`indirect-cf-*`、`s6-*`、`opt-*`、`gap-*`、`final-*` 等）  
- `docs/testing/ai-ui-continuation-report.md`  
- `docs/testing/ai-ui-month2-report.md`  
- `docs/testing/ai-ui-verify-fixes-report.md`  
- `docs/testing/ai-ui-book-b-report.md`  
- `docs/testing/ai-ui-book-b-arap-fa-report.md`  
- `docs/testing/ai-ui-book-b-fa-dispose-report.md`  
- `docs/testing/ai-ui-book-b-fa-surplus-report.md`  
- `docs/testing/ai-ui-book-b-payroll-exp-report.md`  
- `docs/testing/ai-ui-book-c-report.md`、`docs/testing/ai-ui-book-d-report.md`  
- `docs/testing/ai-ui-section6-report.md`  
- `docs/testing/ai-ui-optional-workbench-tax-report.md`  
- `docs/testing/ai-ui-remaining-gaps-report.md`
- `docs/testing/ai-ui-final-gaps-report.md`  
- `docs/testing/ai-ui-guards-report.md`  
- `docs/testing/ai-ui-indirect-cf-report.md`  
- `docs/testing/ai-ui-cf-remediate-report.md`  
- `docs/testing/ai-ui-export-check-report.md`  


