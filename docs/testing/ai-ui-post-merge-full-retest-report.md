# AI UI 合入 main 后全量复测报告

- **基线**：`origin/main` @ `52ebaed`（Merge PR #17）
- **环境**：前端 `http://127.0.0.1:3154` / 后端 `http://127.0.0.1:2154` / MySQL `:3307`
- **账套**：保留 `AI-UI-20260930` 系列（A/B/C/D/封存探针）
- **开始**：2026-10-01T03:09:39.316Z
- **结束**：2026-10-01T03:12:06.057Z
- **编排脚本**：`financial-cloud-ui/scripts-ai-ui-post-merge-full.mjs`

## 结论：**PASS**（套件 9/9 通过；步骤合计 PASS 130 / FAIL 0 / WARN 16）

合入 PR #17（`52ebaed`）后，用已有 `AI-UI-20260930` 账套做全量复测。无 FAIL。WARN 主要为：工资基数回退（产品设计）、间接法脚本按「本期」口径对比 YTD 字段（已知口径差，非回归）、以及部分 UI 抽屉无 V02 行（闭账期不可改 CF，guard **513009** 已记 PASS）。

## 套件结果

| 套件 | 结果 | PASS | FAIL | WARN | 耗时 |
|---|---|---:|---:|---:|---:|
| §六 校验/批量/权限 | PASS | 23 | 0 | 0 | 11s |
| 剩余细项（工资/固资/辅助/待办） | PASS | 17 | 0 | 3 | 23s |
| 收尾细项（累计预扣/模板/账龄/互斥） | PASS | 20 | 0 | 0 | 14s |
| 可选：工作台/封存/税费 | PASS | 21 | 0 | 0 | 35s |
| 反结账/闭账守卫 | PASS | 10 | 0 | 0 | 10s |
| 导出内容级校验 | PASS | 6 | 0 | 0 | 8s |
| 历史缺陷修复抽检 | PASS | 6 | 0 | 1 | 10s |
| 间接法现金流量抽检 | PASS | 8 | 0 | 12 | 5s |
| 专项 B 日记账扩展 | PASS | 19 | 0 | 0 | 31s |

## 观察

- **remaining-gaps**：OBS-PAY-BASE-FALLBACK：缴费基数清空后 createTable 仍用回退基数算薪，无「基数缺失」硬拒
- **remaining-gaps**：固资复制卡保留：id=2105495398843940866 code=B-ASSET-SUR-BUMP-副本6
- **final-gaps**：OBS-PAY-HIST-NO-CASCADE：已确认工资明细改应发不重算个税，也不自动重算后续月累计；update 还会清空凭证关联字段
- **final-gaps**：互斥探测曾生成草稿计提 2105495475033473026（原过账计提 2105457005623885825 仍在）；测后删除草稿
- **optional**：税费测算：企税=利润总额×税率可独立验算；增值税依赖应交税费科目发生，本夹具为 0；UI 明示非正式申报
- **optional**：增值税申报表：由税费测算推导的参考表，非正式外部申报

## 日志

- `/tmp/ai-ui-post-merge/scripts-ai-ui-section6.log`
- `/tmp/ai-ui-post-merge/scripts-ai-ui-remaining-gaps.log`
- `/tmp/ai-ui-post-merge/scripts-ai-ui-final-gaps.log`
- `/tmp/ai-ui-post-merge/scripts-ai-ui-optional-workbench-tax.log`
- `/tmp/ai-ui-post-merge/scripts-ai-ui-guards.log`
- `/tmp/ai-ui-post-merge/scripts-ai-ui-export-check.log`
- `/tmp/ai-ui-post-merge/scripts-ai-ui-verify-fixes.log`
- `/tmp/ai-ui-post-merge/scripts-ai-ui-indirect-cf.log`
- `/tmp/ai-ui-post-merge/scripts-ai-ui-book-b-journal-ext.log`

## 明细子报告

- `docs/testing/ai-ui-section6-report.md`
- `docs/testing/ai-ui-remaining-gaps-report.md`
- `docs/testing/ai-ui-final-gaps-report.md`
- `docs/testing/ai-ui-optional-workbench-tax-report.md`
- `docs/testing/ai-ui-guards-report.md`
- `docs/testing/ai-ui-export-check-report.md`
- （及 verify-fixes / indirect-cf / book-b-journal-ext 既有报告，若脚本有写出）
