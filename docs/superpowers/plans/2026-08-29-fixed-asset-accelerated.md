# 加速折旧 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans (or subagent-driven-development). Steps use TDD.

**Goal:** 增加双倍余额递减、年数总和两种折旧方法并接入计提与前端选项。

**Architecture:** 纯函数扩展 `FixedAssetDepreciationRules`；枚举 + 计提分支；UI 下拉同步。

**Tech Stack:** Java 17、JUnit 5、Vue 3、Element Plus

---

### Task 1: Rules + 单测

**Files:**
- Modify: `financial-cloud/src/main/java/com/financial/cloud/util/FixedAssetDepreciationRules.java`
- Modify: `financial-cloud/src/test/java/com/financial/cloud/util/FixedAssetDepreciationRulesTest.java`

- [ ] 先写 DDB/SYD 失败用例
- [ ] 实现 `yearsFromMonths`、`doubleDecliningAmount`、`sumOfYearsAmount`
- [ ] 单测通过

### Task 2: 枚举 + 计提接入

**Files:**
- Modify: `DepreciationMethod.java`
- Modify: `FixedAssetDepreciationService.java`（`calcAmount`）
- Modify: `FixedAssetErrorCode` / `MessageKeys` / messages_*.properties
- Modify: `FixedAssetService.toVo`（月折旧预览）

- [ ] 枚举两项 + isDepreciable
- [ ] 年限非法抛 `ACCELERATED_LIFE_INVALID`
- [ ] calcAmount 分支

### Task 3: 前端选项

**Files:**
- Modify: `card.vue`、`category.vue`（方法下拉 + 变动下拉；期数显示条件含新方法）

- [ ] 选项与期数表单项

### Task 4: 验证

- [ ] `mvn -Dtest=FixedAssetDepreciationRulesTest test`
- [ ] 打包重启后端
