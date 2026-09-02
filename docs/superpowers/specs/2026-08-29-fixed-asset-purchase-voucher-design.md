# 固定资产购入凭证

## 范围

新建卡片 `save` 成功后自动生成一张草稿购入凭证；编辑/复制/导入不生成。

## 分录

- 借：固定资产科目 — 原值
- 借：税金科目 — 税额（>0 时；科目 `taxSubjectId`，缺省进项税候选）
- 贷：购入对方 — 原值 + 税额（`purchaseCounterpartSubjectId`，缺省 1002/1001）

缺必要科目 → 整笔新增回滚。落库 `purchase_voucher_id`。
