# 经典凭证打印 · 恢复版接入清单

> 基座：`a78dbf6` 恢复后的 `voucher-edit.vue`（勿再基于被覆盖的精简版）

## 目标

点「打印」打开与 `docs/voucher-print-preview-classic.html` 同版式的静态页；编辑态过次页 DOM 暂保留但不走打印入口。

## 已做

- [x] `onPrint` → `localStorage` + `voucher-print-classic.html?autoprint=1`
- [x] `mode=print` → `location.replace` 到同一静态页
- [x] 打印 payload 补齐 `companyName`、科目叶名（`normalizeSubjectLeafName`）
- [x] docs 样张 CSS 同步到 `public/voucher-print-classic.html`

## 刻意未做（避免搅乱恢复版）

- 不删除 `buildPrintSheets` / iframe / 过次页打印 CSS（死代码可后续清理）
- 不改结算/反结账
- 不把分页语义改回「过次页」——经典页用同号续页 + 末页大写/备注

## 验收

1. 凭证编辑页点打印 → 新开经典页，C5/B5 横向、边距「无」
2. 超过 6 行 → 续页标题「记账凭证（续）」，合计/备注/签名仅末页
3. 编辑、暂存、保存、科目辅助核算行为与恢复版一致
