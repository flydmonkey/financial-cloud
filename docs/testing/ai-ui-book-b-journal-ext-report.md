# AI UI 专项账套 B — 5.1 出纳日记账扩展

- **标识**：`AI-UI-20260930`
- **账套**：`AI-UI-20260930-专项B`
- **bookId**：`2105448444973871105`
- **启用期间**：`2026-01`（凭证审核开启）
- **环境**：前端 `http://127.0.0.1:3154` / 后端 `http://127.0.0.1:2154`
- **执行时间**：2026-10-01T01:19:26.179Z
- **脚本**：`financial-cloud-ui/scripts-ai-ui-book-b-journal-ext.mjs`

## 结论：**5.1 扩展 PASS**（PASS 19 / FAIL 0 / WARN 0 / BLOCK 0）

## 场景验收（§5.1 扩展）

| # | 场景 | 结果 | 证据要点 |
|---|---|---|---|
| 1 | 未过账关联凭证改额 → 流水回写+余额重算 | PASS | 50→80；日记账 12080 |
| 2 | 草稿凭证删除 → 流水解绑 | PASS | voucherId=null；流水保留 |
| 3 | 已过账凭证红字冲销 → 反向流水+余额 | PASS | 反向收入 30；日记账回 12000；冲销过账 PASS |
| 4 | 企业已付银行未付 500 未达项 | PASS | 对账单 12500 − 500 → 调节后 12000；账面 12000；差额 0 |

## 基线快照（变更前）

| 项目 | 值 |
|---|---:|
| 日记账账户余额 | 12000 |
| 总账 1002 | 7644.26 |
| 对账单余额 | 12000 |
| 对账差额 | 0 |

## 结果表

| 步骤 | 结果 | 说明 |
|---|---|---|
| LOGIN | PASS | admin + ai_reviewer |
| SWITCH-BOOK | PASS | bookId=2105448444973871105 term=2026-01 |
| CLEANUP-PRIOR | PASS | no prior JEXT leftovers |
| SUBJECTS | PASS | revenue=5001 expense=5602.01 |
| BASELINE | PASS | journal=12000 gl1002=7644.26 stmt=12000 diff=0 |
| EDIT-REWRITE | PASS | entry income 50→80（期望 80）；journal 12080（期望 12080） |
| UNBIND-DELETE | PASS | del=删除成功; voucherId=null; income=80; journal=12080 |
| EDIT-CLEAN | PASS | restored journal=12000（期望 12000） |
| CF-6306 | PASS | 无待指定 CF 行（跳过） |
| REV-SRC-POST | PASS | sender=true |
| REV-PRE | PASS | journal=11970 gl=7614.26 |
| REV-CREATE | PASS | reverseVoucherId=2105467552046034945 voucherDate=2026-01-01 |
| REV-JOURNAL | PASS | reverse entry dir=i income=30 tradeDate=2026-01-01 08:00:00; journal=12000（期望基线 12000） |
| REV-POST | PASS | submit=成功提交1条凭证, 忽略0条; audit=操作总数：1; 成功：1; 失败：0; 不存在项：0; post=操作总数：1; 成功：1; 失败：0 |
| REV-GL-RESTORE | PASS | gl1002=7644.26（基线 7644.26；若冲销落在其他账期则本期总账不回滚） |
| REV-CLEAN-RESTORE | PASS | cleanup journal=12000 gl=7644.26（期望 12000/7644.26） |
| RECON-OUTSTANDING-500 | PASS | book=12000 stmt=12500 ue=500 ui=0 adj=12000 diff=0（期望 book/adj=12000 stmt=12500 ue=500） |
| RECON-RESTORE | PASS | journal=12000 stmt=12000 diff=0 |
| FINAL-BASELINE | PASS | journal=12000（基线 12000） gl=7644.26（基线 7644.26） |

## 金额轨迹（相对基线）

| 检查点 | 预期 | 实际 |
|---|---:|---:|
| 改额后流水金额 | 80 | 80 |
| 改额后日记账余额 | 12080 | 12080 |
| 删草稿后 voucherId | null | null |
| 红冲后日记账余额 | 12000 | 12000 |
| 红冲过账后总账1002 | 7644.26 | 7644.26 |
| 未达项支出（企业已付银行未付） | 500 | 500 |
| 对账单 | 12500 | 12500 |
| 账面（日记账基线） | 12000 | 12000 |
| 调节后银行（12500−500） | 12000 | 12000 |
| 调节差额 | 0 | 0 |
| 终态日记账余额 | 12000 | 12000 |
| 终态总账1002 | 7644.26 | 7644.26 |

## 阻塞 / 缺陷

_无_

## 证据截图

- `/opt/cursor/artifacts/screenshots/bookb-ext-baseline.webp`
- `/opt/cursor/artifacts/screenshots/bookb-ext-edit-rewrite.webp`
- `/opt/cursor/artifacts/screenshots/bookb-ext-unbind.webp`
- `/opt/cursor/artifacts/screenshots/bookb-ext-reverse.webp`
- `/opt/cursor/artifacts/screenshots/bookb-ext-recon-outstanding.webp`
- `/opt/cursor/artifacts/screenshots/bookb-ext-final.webp`
