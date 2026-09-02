# 质量仪表盘

> 最后更新：2026-09-02  
> 环境：后端 `localhost:2154`，前端 `localhost:3154`

## 总览

| 维度 | 结果 |
|------|------|
| API 冒烟（`tools/smoke-api.mjs`） | 7 场景 |
| Playwright E2E | 35 spec 文件 |
| 后端单测 | 凭证 + 结账 + 报表 + book/journal/hr |
| TypeScript | **121 error**（原 728 → 251 → 121） |
| ESLint | **0 error** / ~3864 warning（原宣称 114 error；规则已降为 warning 后仅剩样式债） |

## 分模块

| 模块 | API 冒烟 | E2E | 说明 |
|------|----------|-----|------|
| auth | ✓ | smoke | 低 |
| voucher | ✓ | 3 条 | 中 |
| statement | ✓ | 4 条 | 报表平衡已修 |
| settlement | ✓ | 3 条 | 结账年份默认已修 |
| dashboard | ✓ | 2 条 | statistics API |
| config | ✓ | 2 条 | 期初/辅助核算 |
| book / journal / hr | ✓ | API + 页面 | 路由拼接已修 |
| 其余 | ✓ | — | 低 |

## 复跑

```bash
# 后端单测
cd financial-cloud && ./mvnw test

# 前端类型 / Lint
cd financial-cloud-ui && npm run typecheck
cd financial-cloud-ui && npm run lint

# API 冒烟
node tools/smoke-api.mjs

# E2E（需 2154 + 3154）
cd financial-cloud-ui && npm run test:e2e
```

## 已知后续

- **账套 4103/4104 权益核对（人工）**：在演示/回归账套打开资产负债表，确认「实收资本 / 资本公积 / 盈余公积 / 未分配利润」与科目余额一致；不平则记入报表缺陷，不阻断发版
- TypeScript 剩余 ~121 error：集中在 `voucher-edit`、audit 页、cash-flow 编辑抽屉等；CI `typecheck` 仍为 `continue-on-error`
- ESLint：error 已清零；大量 `vue/html-*` / `max-attributes-per-line` warning 可后续 `--fix` 分批消化
- CI：`.github/workflows/ci.yml`（push 时跑 E2E）
