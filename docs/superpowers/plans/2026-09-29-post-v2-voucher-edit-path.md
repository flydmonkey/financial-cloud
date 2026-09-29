# Post-V2 · 凭证改证路径 Implementation Plan

**Goal:** Productize non-draft voucher edit paths (posted / reviewed / reviewing).

**Spec:** [2026-09-29-daizhang-post-v2-design.md](../specs/2026-09-29-daizhang-post-v2-design.md)

## Tasks

- [x] P1 编辑页已过账只读 + 引导 + 反过账/红冲
- [x] P2 列表修改对锁定态强制 readonly
- [x] P3 `VoucherService.modifyBlockedReason` + 单测
- [x] P4 文档 03/08
- [x] P5 已审核/审核中引导 + 反审核/撤回 + 后端对齐
- [x] P6 质量仪表盘 + backlog 关账

## Evidence

- `voucherWorkspace.test.ts` posted/review helpers
- `VoucherServiceTest#modifyBlockedReason_postedCancelledAndClosed`
- UI: `voucher-edit.vue` alerts；`voucher-index.vue` `shouldOpenVoucherReadonly`
