## Context

See proposal.md — Why. Product decisions locked with the bookkeeping owner (2026-09-02):

| Topic | Decision |
|-------|----------|
| After save | Stay on current voucher (review); no auto-new |
| Prev/next order | Voucher date + word head + word number |
| Dirty leave | Allow leave without saving (confirm); cancel keeps edits |
| Rows on screen | Grow past 6; print still paginates |
| Subject entry | Code blind-type + Enter primary |
| Header status | Professional defaults (balance + status visible) |
| List role | Search / batch audit / filter only |
| Dialog | Removed as primary edit; always full-page workspace |
| Layout | WYSIWYG vs classic print + strong Tab order |
| Multi-user | No conflict prompt in v1 |

Today `voucher-edit` save pushes list; list often opens edit in dialog. Classic print is separate HTML.

## Goals / Non-Goals

**Goals:**
- Full-page continuous workspace per locked decisions above.
- WYSIWYG sheet layout + Tab/coding speed.
- Neighbor navigation by date+word without requiring list filter context.

**Non-Goals:**
- Modal voucher editor.
- Auto-create blank voucher after save.
- Multi-user conflict detection.
- Changing audit/posting APIs or print pagination rules (reuse classic print).
- Reworking list batch-audit features beyond “open → full page”.

## Decisions

### 1. Stay after save; explicit 新建
- Refresh `formData` / route id on success; never `router.push` list; never auto-blank.

### 2. Neighbor order = date + word
- Canonical sort: `voucherDate ASC`, `wordHead ASC`, `wordNum ASC` within book.
- Prefer API or fetch that can resolve prev/next by this order (not filtered list order). List filters do not redefine neighbor sequence.

### 3. Dirty prompt = discard or cancel
- MessageBox: 不保存离开 / 取消（可选附带「去保存」说明，但不强制保存才能走）。

### 4. No dialog editor
- `voucher-index` open/edit/new → route to full-page workspace only.
- Remove or stop using `props.dialog` as primary path; clean dead dialog wiring if unused.

### 5. Layout: WYSIWYG + keyboard
- Sheet regions mirror print; entry table grows; focus/Tab through summary → subject code → aux (if any) → debit/credit.
- Subject: prioritize code input + Enter to commit leaf subject (keep search as secondary).

### 6. Header status (professional default)
- Show: short word (`记 n 号`), date, unit, attachments, **借贷平衡** indicator, voucher **状态** (draft/reviewed/posted as today).
- Do not clutter with secondary analytics.

### 7. Rows vs print
- Screen: unbounded rows (virtualize later only if performance needs).
- Print: unchanged classic 6-line pages.

## Risks / Trade-offs

- [Removing dialog breaks callers] → Audit `dialog` usages; redirect all to route.
- [Date+word neighbors ignore list filter] → Document: list filter finds a voucher; arrows walk full book chronology (user-confirmed).
- [WYSIWYG vs dense coding] → Keep code+Enter and Tab as first-class; visual sheet must not bury focus.
- [Large edit.vue] → Extract sheet layout subcomponents if needed; preserve business methods.

## Migration Plan

1. Front-end only default behavior.
2. No DB migration.
3. Rollback: restore list dialog open + save→list navigation.

## Open Questions

- None material for v1 (dialog prev/next and sort assumptions resolved by owner answers).
