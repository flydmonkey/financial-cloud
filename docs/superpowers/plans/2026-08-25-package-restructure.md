# 包路径重构 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `jinbooks` 单体按功能域重组为 `domain` / `dto` / `controller` / `repository` / `service` / `common` 五层包结构。

**Architecture:** 分 7 波迁移；每波移动文件、改 package/import、同步 MyBatis XML namespace 与配置；API URL 不变。

**Tech Stack:** Java 21, Spring Boot 4.1, MyBatis-Plus, Maven

## Global Constraints

- VO 并入 `dto.{功能}`（1A），保留 `*Vo` 类名
- Service 迁至 `service.{功能}`（2A）
- 不改 `@RequestMapping`、表名、SQL 语义
- 分波试点，从 voucher 开始
- 每波结束 `mvnw -DskipTests compile`（收尾 `package`）

**规格：** `docs/superpowers/specs/2026-08-25-package-restructure-design.md`

---

### Task 0: common 内核 + MyBatis 双扫

**Files:**
- Move: `entity/BaseEntity.java`, `entity/Message.java`, `entity/PageQuery.java` → `common/`
- Modify: `MybatisPlusConfiguration.java`, `application-jinbooks.properties`
- Global import replace across `jinbooks/src`

- [x] 移动三类到 `com.financial.cloud.common`
- [x] 全项目替换 import
- [x] `@MapperScan` 增加 `com.financial.cloud.repository`
- [x] `mapper-locations` 增加 `repository/**/xml` 通配
- [x] `mvnw -DskipTests compile`

### Task 1: voucher 全栈试点

**Files:**
- domain: `entity/voucher/*.java`（7 实体）→ `domain/voucher/`
- dto: `entity/voucher/dto/*` + `entity/voucher/vo/*` → `dto/voucher/`
- controller: `web/voucher/controller/*` → `controller/voucher/`
- repository: `persistence/mapper/Voucher*.java` → `repository/voucher/`
- service: `persistence/service/Voucher*.java` + `impl/*` → `service/voucher/`
- XML: 3 个 Voucher*.xml → `repository/voucher/xml/mysql/`

- [x] 移动 + 改 package
- [x] 全项目 import / XML namespace 替换
- [x] `type-aliases-package` 增加 `com.financial.cloud.domain.voucher`
- [x] 删除空 `entity/voucher`、`web/voucher`
- [x] `mvnw -DskipTests compile`

### Task 2–6: 后续波次

- [x] book → journal/statement → idm/permissions/security → config/hr/standard/history/report → auth + 收尾
- [x] 全量 `mvnw -DskipTests compile` BUILD SUCCESS
