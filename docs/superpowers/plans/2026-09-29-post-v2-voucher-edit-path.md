# Post-V2 · 过账后改证路径 Implementation Plan

**Goal:** Productize posted-voucher edit path (W2-2 leftover).

**Spec:** [2026-09-29-daizhang-post-v2-design.md](../specs/2026-09-29-daizhang-post-v2-design.md)

## Tasks

- [x] P1 编辑页只读 + 引导 + 反过账/红冲
- [x] P2 列表修改对已过账强制 readonly
- [x] P3 `VoucherService.modifyBlockedReason` 产品文案 + 单测
- [x] P4 文档 03/08 + backlog

## Evidence

- `voucherWorkspace.test.ts` posted guidance helpers
- `VoucherServiceTest#modifyBlockedReason_postedCancelledAndClosed`
- UI: `voucher-edit.vue` alert；`voucher-index.vue` handleUpdate
