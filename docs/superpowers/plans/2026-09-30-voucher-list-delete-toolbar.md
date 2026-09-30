# 凭证列表工具栏一级删除 Implementation Plan

> **For agentic workers:** Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Promote batch delete to a primary toolbar button on voucher list with clear disabled tooltip.

**Architecture:** UI-only change in `voucher-index.vue`; reuse `handleDelete` / `isDeletable` / selection state.

**Tech Stack:** Vue 3 + Element Plus

---

### Task 1: Toolbar delete button

**Files:**
- Modify: `financial-cloud-ui/src/views/voucher/voucher-index.vue`
- Spec: `docs/superpowers/specs/2026-09-30-voucher-list-delete-toolbar-design.md`

- [x] Add computed `canToolbarDelete` from selected deletable vouchers
- [x] Insert primary danger「删除」between 过账 and 更多; wrap with tooltip on disable
- [x] Keep「更多」内删除 item
- [x] Manual / Playwright smoke: select draft → delete; select posted → disabled
- [ ] Commit
