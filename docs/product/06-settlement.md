# 06 · 期末结转与月结

> 状态：专业月结**五步引导向导**已落地（人工确认 → 凭证整理 → 计提结转 → 系统校验 → 结账；步骤内过账/断号/结转/校验，静默 verify UX）。往来 L1+L2+L3 已接入系统校验（逾期仅警告，不阻断结账）。

## 1. 模块定位

以**月结**关闭当前开放账期：五步引导向导推进**人工确认 → 凭证整理 → 计提与结转 → 系统校验 → 结账**，锁定账期并推进下一月。支持守卫条件下的反结账。不提供独立「年终结账」入口；12 月结转本年利润仍作为当月结转模板。

## 2. 典型场景

1. 会计打开「月结」向导，当前账期五步推进：**人工确认 → 凭证整理 → 计提与结转 → 系统校验 → 结账**。
2. **人工确认**：勾选银行/存货/税控等人工项（本期不系统检；往来已进系统校验）。
3. **凭证整理**：向导内列出未过账凭证，批量提交/审核/过账；执行断号检查与一键整理；两项均通过才可下一步。
4. **计提与结转**：向导内计提固定资产折旧（如有）；对必做损益结转（`qm_jz_sr`、`qm_jz_cbfy`；12 月另有 `qm_jz_bnlr`）逐行**生成并过账**；`qm_jz_cbfy` 含**主营业务成本**（小企业 `5401` / 企业会计制度对应成本科目，余额非零时参与结转）。独立模板 `qm_jz_xscb`（结转销售成本）与 `jt_zj`（期末计提折旧模板）已退役：前者并入 `qm_jz_cbfy`，后者改由固定资产模块计提。
5. **系统校验**：调用 `verify`，结果表内联展示；失败项提供「去处理」跳回所属步骤；**不以全局 error toast 作为唯一反馈**（`silentError`）。
6. **结账**：确认后 `checkout`；成功后账期推进下一月（往来逾期仅警告，仍可结账）。
7. 发现上月错误，在允许条件下反结账后修改再结。

## 3. 功能清单

| 能力 | 状态 | 说明 |
|------|------|------|
| 月结五步引导向导 | **已实现** | `settle-period.vue` + `wizard/*`：人工确认 → 凭证整理 → 计提结转 → 系统校验 → 结账；步骤门禁 + 向导内过账/断号/结转 |
| 向导内凭证整理 | **已实现** | `StepVoucherPrep`：未过账列表、批量过账、断号检查与整理 |
| 向导内计提结转 | **已实现** | `StepAccrueCarry`：折旧 + 必做结转生成并过账；`qm_jz_cbfy` 含主营业务成本（5401/6401） |
| 静默 verify UX | **已实现** | `verify({ silentError: true })`；`StepVerify` 内联结果表 + 「去处理」跳转 |
| 期末结转模板列表 | **已实现** | `fetchcarry` |
| 按模板生成结转凭证 | **已实现** | `generate-voucher` |
| 删除结转凭证 | **已实现** | `DELETE /delete/{voucherId}` |
| 账期列表 | **已实现** | `settle-list.vue` |
| 结账硬门槛 verify | **已实现** | 未过账、断号、借贷平衡、必做结转、折旧 N/A-or-done |
| 结账 checkout | **已实现** | 服务端再次硬检；报表快照、余额结转、推进账期 |
| 反结账 uncheckout | **已实现** | OpenSpec `settlement-uncheckout`；含日记账期初恢复 |
| 往期数据锁定 | **已实现** | 含凭证新建 `save`/`update` 拒绝早于开放账期 |
| 往来应收应付 / 账龄摘要 | **已实现** | verify 项「往来款项」；逾期 `warning=true` 且 `result=true`，不硬阻断 |
| 往来核销 L3 | **已实现** | 开项匹配；逾期仍不硬阻断 |
| 独立年结入口 | **不做** | 对齐金蝶仅月结 |

## 4. 页面与路由

| 页面 | 路由 | 组件 |
|------|------|------|
| 账期列表 | `/settlement/settle-list` | `settle-list.vue` |
| 月结五步向导 | `/settlement/settle-period` | `settle-period.vue`（`wizard/StepManual` … `StepCheckout`） |
| 期末结转 | `/settlement/carry-forward` | `carry-forward.vue` |

## 5. 数据模型

| 表 | 用途 |
|------|------|
| `settlement` | 结账记录（book_id + year_period 等） |
| `settlement_carryforward` | 结转与凭证关联 |
| `voucher`（`carry_forward`） | 标记结转凭证 |
| `config` / 账套 `current_account_date` | 当前账期 |
| `journal_account.prev_opening_balance` 等 | 反结账恢复期初 |

## 6. 核心接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/settlement/fetch` | 账期状态列表 |
| GET | `/api/settlement/verify` | 结账硬检（含 hard/applicable/reason/warning） |
| GET | `/api/settlement/checkout` | 结账（内嵌硬检） |
| POST | `/api/settlement/uncheckout` | 反结账 |
| GET | `/api/settlementcarry/fetchcarry` | 结转项 |
| POST | `/api/settlementcarry/generate-voucher` | 生成结转凭证 |
| DELETE | `/api/settlementcarry/delete/{voucherId}` | 删除结转凭证 |

服务：`SettlementService`、`SettlementCarryService`、`MonthEndCloseRules`、`ArapService.monthEndSummary`。  
前端：`src/api/book/settlement.ts`。

## 7. 业务规则与约束

### 月结硬门槛（未通过不可结账）

1. 当前账期无未过账凭证（已作废除外）。
2. 凭证号连续性通过。
3. 借贷合计相等。
4. 必做结转已生成：`qm_jz_sr`、`qm_jz_cbfy`（含主营业务成本 `5401`/`6401` 别名）；12 月另要求 `qm_jz_bnlr`（本年利润无余额则为 N/A）。`verify` 硬检项 4 以「已生成结转凭证」为准；未过账结转凭证由硬检项 1（未过账凭证）拦截。**向导步骤 3（计提与结转）**以「已生成并过账」为完成态，方可进入系统校验。
5. 有应折旧资产时须已计提折旧；无则为 N/A。

规则见 `MonthEndCloseRules`（模板无 `required_for_close` 列，按编码白名单）。

### 系统往来摘要（不硬阻断）

应收/应付期末合计与账龄逾期金额写入 verify「往来款项」说明；有逾期时打警告标，`result` 仍为通过，结账不被阻断。账龄为按凭证日期 FIFO 估算，**非核销账龄**。

### 人工确认（不阻断系统 verify）

银行调节、存货、税控勾稽等：UI 确认即可；不作为 hard fail。

### 反结账

依据 OpenSpec `settlement-uncheckout`：仅相邻最近已结月；下期有凭证/流水则拒绝；事务回滚快照与日记账期初。

### 往期锁定

- 凭证期间须 ≥ 当前开放账期。
- `save` / `update` 新建与修改均拦截早于开放账期的写入。

## 8. 已知缺口 / 后续

- 银行余额调节、税费向导不在本期。
- 逾期是否升级为可配置硬阻断，待产品策略再议。

## 9. 证据索引

- `SettlementController`、`SettlementCarryController`
- `SettlementService.checkout/uncheckout/verify`、`MonthEndCloseRules`、`SubjectCodeCompat`
- `financial-cloud-ui/src/views/settlement/settle-period.vue`、`wizard/*`
- `openspec/changes/month-end-guided-wizard/`、`openspec/specs/month-end-close/spec.md`
- E2E：`settlement*.spec.ts`、`arap.spec.ts`、`carry-forward-flow.spec.ts`
