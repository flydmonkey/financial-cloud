# 凭证与结账参数独立页 Implementation Plan

> **For agentic workers:** Implement in-repo; tests: SettlementServiceTest + BookServiceTest.

**Goal:** Dedicated voucher/settlement params page with review toggle and optional ARAP verify hint.

**Architecture:** Config key + Book.voucherReviewed; dedicated GET/PUT; Vue page under 基础设置.

**Tech Stack:** Spring Boot, Vue 3, config table, resources menu patch.

## Global Constraints

- Hard month-end gates stay non-configurable.
- Missing ARAP config means enabled (true).
- Dual-edit voucherReviewed with books/edit.vue.
