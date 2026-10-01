# 财务云 AI UI 全流程测试报告

- **环境**：`http://127.0.0.1:3154`（后端 `:2154`，MySQL `:3307`）
- **执行时间**：2026-09-30 / 2026-10-01（UTC）
- **版本**：财务云 v1.1.0
- **基线文档**：`docs/testing/ai-ui-full-process-test-prompt.md`
- **测试标识**：`AI-UI-20260930`
- **账套**：`AI-UI-20260930-主账套A`（小企业会计准则，启用凭证审核）
- **登录/写入**：用户允许**脚本注入**（登录 + Node/in-page fetch：draft→submit→audit→post、CF specify、结转）；月结向导与报表截图走 UI
- **脚本**：`financial-cloud-ui/scripts-ai-ui-continuation.mjs`、`scripts-ai-ui-month2.mjs`、`scripts-ai-ui-book-b.mjs`、`scripts-ai-ui-book-b-journal-ext.mjs`、`scripts-ai-ui-book-b-arap-fa.mjs`、`scripts-ai-ui-book-b-fa-dispose.mjs`、`scripts-ai-ui-book-b-payroll-exp.mjs`、`scripts-ai-ui-book-c.mjs`、`scripts-ai-ui-book-d.mjs`
- **明细**：`docs/testing/ai-ui-continuation-report.md`、`docs/testing/ai-ui-month2-report.md`、`docs/testing/ai-ui-book-b-report.md`、`docs/testing/ai-ui-book-b-journal-ext-report.md`、`docs/testing/ai-ui-book-b-arap-fa-report.md`、`docs/testing/ai-ui-book-b-fa-dispose-report.md`、`docs/testing/ai-ui-book-b-payroll-exp-report.md`、`docs/testing/ai-ui-book-c-report.md`、`docs/testing/ai-ui-book-d-report.md`、`docs/testing/ai-ui-cf-begin-cash-fix-report.md`

---

## 最终结论：**主路径闭环；剩余可选深路径**

主账套已完成建账→期初→V01–V08→CF 指定→V04 反操作→1 月结转/月结→2 月五笔→2 月结转/月结；账期现为 **2026-03**。历史快照抽查：1 月银行 160,000 / 本年利润 40,000；2 月银行 188,000 / 本年利润 53,000。

**缺陷修复**：
1. **BUG-TZ-DATE（P0）** — 已修
2. **BUG-CF-UI-SIGN（P1）** — 已修
3. **OBS-CARRY-STALE-POINTER** — 删除凭证时清理结转指针
4. **BUG-TERM-CROSS-BOOK（P1）** — 已修
5. **OBS-CF-BEGIN-CASH-FEB** — 已修（上期 CF 期末实时重算；2 月期初 160,000）
6. **OBS-CF-AR-ADJ** — 闭环为口径说明（应收+预付 ⇒ −18,000）
7. **BUG-JEXT-REVERSE-POST** — 冲销日期钳到开放账期 + 负金额流水回写；复测冲销过账 **PASS**（19/19）

专项 B 5.1 核心+扩展（含红冲过账/未达 500）、固资深路径、工资/报销、C/D、守卫、CF 勾稽、导出内容级校验已完成。可选：盘盈入账 book-surplus（护主卡未跑）。

---

## 执行汇总

| 分类 | 通过 | 失败 | 跳过 |
|---|---:|---:|---:|
| 登录建账期初 | 8 | 0 | 1 |
| 1 月凭证/报表/反操作/月结 | 主路径通过 | — | V04 CF 项待补 |
| 2 月凭证/结转/月结/历史快照 | 主路径通过 | — | 2 月 CF 指定未齐 |
| P0/P1 修复验证 | TZ+CF UI | — | — |
| 专项 B 出纳日记账 5.1 | 核心+扩展 PASS | — | 见 journal-ext 报告 |
| 专项 B 往来/固资 5.2–5.3 | 核心+清理/盘点深路径 PASS | — | 盘盈入账未跑（护主卡）；两 P1 已修 |
| 专项 B 工资/报销 5.4–5.5 | 核心路径 PASS | — | 公式空 WARN；摘要长度 OBS |
| 专项 C 关闭凭证审核 | PASS（免审核流） | — | — |
| 反结账/闭账守卫 + 导出入口 | PASS | — | 导出仅 smoke（xlsx 非空） |
| 现金流量/间接法点测 | 补全后主路径 PASS | — | BEGIN-CASH/AR-ADJ 已闭环 |
| 专项 D 年末 | PASS（损益+年终结转+跨年） | — | BUG-TERM-CROSS-BOOK 已修 |
| 导出内容级校验 | PASS（xlsx+pdf） | — | 2026-01 样本 |

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

### OBS-EXP-SUMMARY-LEN（P2）
`voucher_item.summary` 仅 64 字符；报销一键生成摘要过长时 `Data truncation`。缩短报销人/事由后 PASS（见 B 5.5）。


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

### 5.3 固定资产 — PASS（含清理/盘点深路径）
- 类别+卡片原值 12,000 / 残值 0% / 12 月直线法 / 费用 5602.02；UI 购入凭证过账（1601/1002）
- 2026-01 计提折旧 1,000 过账后：原值 12,000 / 累计折旧 1,000 / 净值 **11,000**
- **深路径补测**（独立卡，主卡未动）：`B-ASSET-DISP-002` 清理→DISPOSED+处置净损失凭证；盘点单完成（盘盈 preview / 盘亏下账）；终总账 **1601=12,000**
- 缺陷：**BUG-FA-CHECK-DEFICIT-SUMMARY** / **BUG-FA-DISPOSE-VOUCHER-DATE** 已在 `FixedAssetService` 修复（摘要截断 + 凭证日期钳到开放账期）
- **明细**：`docs/testing/ai-ui-book-b-fa-dispose-report.md`；截图 `bookb-fa-disp-*` / `bookb-fa-check-*`
- 截图 `bookb-fa-*`（核心折旧）+ 深路径截图见上

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

### 未测（B 其余）
盘盈入账（book-surplus）故意未跑以保护主卡；日记账回写/红冲/未达项 500 已测（见 journal-ext）


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

1. OBS-CF-BEGIN-CASH-FEB / OBS-CF-AR-ADJ 根因修复  
2. （可选）盘盈入账 book-surplus 全量（当前仅 preview，护主卡） 

---

## 证据

- `/opt/cursor/artifacts/screenshots/`（`m2-*`、`verify-cf-*`、`bookb-*`、`bookb-jext-*`、`bookb-fa-disp-*`、`bookb-fa-check-*`、`bookb-pay-*`、`bookb-exp-*`、`bookc-*`、`bookd-*`、`guards-*`、`indirect-cf-*` 等）  
- `docs/testing/ai-ui-continuation-report.md`  
- `docs/testing/ai-ui-month2-report.md`  
- `docs/testing/ai-ui-verify-fixes-report.md`  
- `docs/testing/ai-ui-book-b-report.md`  
- `docs/testing/ai-ui-book-b-arap-fa-report.md`  
- `docs/testing/ai-ui-book-b-fa-dispose-report.md`  
- `docs/testing/ai-ui-book-b-payroll-exp-report.md`  
- `docs/testing/ai-ui-book-c-report.md`、`docs/testing/ai-ui-book-d-report.md`  
- `docs/testing/ai-ui-guards-report.md`  
- `docs/testing/ai-ui-indirect-cf-report.md`  
- `docs/testing/ai-ui-cf-remediate-report.md`  
- `docs/testing/ai-ui-export-check-report.md`  



