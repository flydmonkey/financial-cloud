# AI UI 缺陷修复验证报告

- **时间**：2026-09-30T23:57Z～2026-10-01
- **修复提交**：`ec37ae8`（`DateUtils` Asia/Shanghai、CF UI 正数约定、结转凭证删除清指针）
- **账套**：`AI-UI-20260930-主账套A`（`2105377998655979522`）
- **脚本**：`financial-cloud-ui/scripts-ai-ui-verify-fixes.mjs`

## BUG-TZ-DATE（P0）— 已修复并验证

| 检查 | 结果 | 说明 |
|---|---|---|
| 开放账期首日 `2026-03-01` 暂存 | PASS | draft 成功，id 已删除清理 |
| 月中 `2026-03-15` 暂存 | PASS | 对照通过 |
| 单元测试 `DateUtilsTest` + `VoucherServiceTest` | PASS | 29 tests |

根因：`DateUtils.format` 在 UTC JVM 下把 GMT+8 的 `YYYY-MM-01` 格式成上月。现固定 `Asia/Shanghai`。

## BUG-CF-UI-SIGN（P1）— 已修复并验证

| 检查 | 结果 | 说明 |
|---|---|---|
| UI 选择「销售商品」自动填正数 | PASS | 探测凭证 autofill=`1,000.00` |
| 正数保存过平衡校验 | PASS | 消息「保存成功」，无「不平衡」 |
| 当月销售商品行 | PASS | 本月 `1,000.00`（探测后已删除凭证） |
| `cashFlowAssign` 单测 | PASS | `npm run test:unit` |

根因：抽屉对 `direction===2` 取负入库；报表按绝对值汇总却叠了负号。现入库绝对值，仅平衡校验对流出（dir=1）取负。

### 历史数据 remediation

闭账期无法再 API 改 CF。对主账套 4 条负余额执行 `ABS(cash_flow_balance)` 后，现金流量表本年累计：

| 项目 | YTD |
|---|---:|
| 销售商品收到的现金 | 50,000.00 |
| 购买商品支付的现金 | 8,000.00 |
| 购建固定资产支付的现金 | 12,000.00 |
| 取得借款收到的现金 | 40,000.00 |

仍缺：V04「支付其他」10,000；2 月 CF 指定（此前 WARN）。探测用 3 月 CF-VERIFY 凭证已反过账删除。

## OBS-CARRY-STALE-POINTER

`VoucherService.delete` 同步清理匹配的 `settlement_carryforward` 行（随 `ec37ae8`）。

## 证据截图

- `/opt/cursor/artifacts/screenshots/verify-cf-m3-ui.webp`
- `/opt/cursor/artifacts/screenshots/verify-cf-after-abs.webp`
