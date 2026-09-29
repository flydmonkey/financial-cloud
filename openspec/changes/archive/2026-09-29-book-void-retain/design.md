# Design: book-void-retain

## Context

`BookStatusEnum`：0 禁用 / 1 启用 / 2 封存。封存已由 `BookSealGuard` 拦截写操作；删除已拒封存账套，但仍允许对「禁用且有凭证」的账套级联硬删。

## Goals

- 有业务数据 → 删除失败，引导封存。
- 空账套 → 禁用后可删（现状不变）。
- 不新增第三态；「作废留存」= 封存 + 删除闸。

## Non-Goals

- 软删除 `book` 行、回收站、跨实例 dump。
- 改变覆盖式恢复 / 克隆恢复。

## Decisions

1. **有数据判定**：`SELECT COUNT(1) FROM voucher WHERE book_id=?` > 0 即视为有业务数据（凭证是账史核心；空凭证但仅有科目壳仍允许删，与「空闲账套」直觉一致）。
2. **错误码**：新增 `BOOK_HAS_DATA_DELETE`（510020），文案提示「请先封存留存，不可删除有业务数据的账套」。
3. **UI**：删除失败时展示后端 message；可选在确认框补一句「有凭证的账套请使用封存」。

## Risks

- **[Risk] 仅查 voucher 漏网**（仅日记账/资产无凭证）→ 接受 v1；后续可扩到 BackupTableRegistry 抽样。
- **[Risk] 与测试库清理脚本冲突** → 测试需先清凭证或走禁用空套路径。
