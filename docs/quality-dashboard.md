# 质量仪表盘

> 最后更新：2026-09-29  
> 环境：本机 Cloud Agent（`financial-cloud` + `financial-cloud-ui`）；E2E 未在本轮全量复跑

## 总览

| 维度 | 结果 |
|------|------|
| 后端单测（`./mvnw test`） | **通过**（BUILD SUCCESS，2026-09-29） |
| TypeScript（`npm run typecheck`） | **129 error**（较 2026-09-02 记录的 ~121 略升；CI 仍 `continue-on-error`） |
| ESLint（`npm run lint`） | **36 error** / **915 warning**（规则较严；error 未清零） |
| Playwright E2E | 约 **40** 个 `e2e/*.spec.ts`（本轮未全量复跑） |
| API 冒烟（`tools/smoke-api.mjs`） | 未本轮复跑 |

## 分模块（历史基线，仍适用）

| 模块 | API 冒烟 | E2E | 说明 |
|------|----------|-----|------|
| auth | ✓ | smoke | 低 |
| voucher | ✓ | 多条 | 中 |
| statement | ✓ | 多条 | 报表平衡已修 |
| settlement | ✓ | 多条 | 月结向导已落地 |
| dashboard | ✓ | 有 | statistics API |
| config | ✓ | 有 | 期初/辅助核算 |
| arap / journal / hr | ✓ | 有 | 往来核销、日记账、薪资最小闭环 |
| 其余 | — | — | 按模块增补 |

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

- TypeScript ~129 error：分散在 voucher-edit、cash-flow 编辑抽屉、idm、settlement 等；**不阻塞**吞吐 V2 功能刀，纳入质量债队列
- ESLint：36 error + 大量 warning；功能刀不要求一次清零
- 吞吐 V2 回归门禁见 [throughput-v2-backlog](superpowers/plans/2026-09-29-throughput-v2-backlog.md)
- 账套权益核对（人工）：资产负债表与科目余额一致检查仍建议发版前抽检
