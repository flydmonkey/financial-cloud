# 质量仪表盘

> 最后更新：2026-08-27  
> 环境：后端 `localhost:2154`，前端 `localhost:3154`

## 总览

| 维度 | 结果 |
|------|------|
| API 冒烟（34 探针） | 34/34 通过 |
| Playwright E2E | 18/18 通过（含页面探针） |
| TypeScript | 261 error（原 728，-64%） |
| ESLint | 114 error / 698 warning（原 398 / 11378） |
| 后端单测 | 凭证 + 结账 + 报表 seed |
| Controller | ~57 个，多数无单测 |

## 分模块

| 模块 | API 冒烟 | E2E | 说明 |
|------|----------|-----|------|
| auth | 1/1 | smoke | 低 |
| voucher | 3/3 | 3 条 | 中 |
| statement | 5/5 | 4 条 | 报表平衡已修 |
| settlement | 2/2 | 3 条 | 结账年份默认已修 |
| dashboard | 5/5 | 部分 | bookId 顺序已修 |
| book / journal / hr | 全过 | API + 页面探针 | 中 |
| 其余 | 全过 | — | 低 |

## 复跑

```bash
# 后端单测
cd financial-cloud && ./mvnw.ps1 test

# E2E（需 2154 + 3154）
cd financial-cloud-ui && npm run test:e2e

# API 冒烟（本地 scripts/module-smoke-api.ps1，未入库）
```

## 已知后续

- 账套 4103/4104 权益数据建议人工核对
- TypeScript ~261 error、ESLint ~114 error（已做首轮基础设施清理，剩余多为组件级问题）
- 资产负债表总计调平会在服务端日志记录差额
