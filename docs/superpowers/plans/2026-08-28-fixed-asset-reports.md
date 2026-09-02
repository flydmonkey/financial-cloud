# 固定资产二期（折旧报表）Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 只读折旧明细表（类别分组+小计）与汇总表，数据来自 `fixed_asset_depr` + 卡片。

**Architecture:** `FixedAssetDepreciationReportRules` 算单卡金额；`FixedAssetDepreciationReportService` 组装明细行/分组小计/汇总；Controller 挂 `/api/fixed-asset/report`；前端两页 + 菜单 seed。

**Tech Stack:** Java/Spring、MyBatis-Plus、Vue3 Element Plus、既有 Excel 导出工具（若有）

---

### Task 1: Rules + 单测

**Files:**
- Create: `financial-cloud/src/main/java/com/financial/cloud/util/FixedAssetDepreciationReportRules.java`
- Create: `financial-cloud/src/test/java/com/financial/cloud/util/FixedAssetDepreciationReportRulesTest.java`

覆盖：期初累计、本期、本年、期末净值；跨月区间。

### Task 2: Report Service + Controller

**Files:**
- Create DTOs under `dto/fixedasset/`
- Create `FixedAssetDepreciationReportService.java`
- Create `FixedAssetReportController.java`

### Task 3: Frontend + 菜单

**Files:**
- `views/fixed-asset/depreciation-detail.vue`
- `views/fixed-asset/depreciation-summary.vue`
- `api/fixed-asset/report.ts`
- Update `sql/seed/fixed_asset_menu.sql` + apply script
- Link from `depreciation.vue` success → detail

### Task 4: 验证

- `mvn -Dtest=FixedAssetDepreciationReportRulesTest test`
- 重启后端；菜单可见；有计提数据时明细/汇总金额一致
