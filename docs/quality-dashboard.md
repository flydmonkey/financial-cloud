# 质量仪表盘

> 最后更新：2026-09-02  
> 环境：后端 `localhost:2154`，前端 `localhost:3154`

## 总览

| 维度 | 结果 |
|------|------|
| API 冒烟（`tools/smoke-api.mjs`） | 7 探针 |
| Playwright E2E | 35 spec 文件 |
| 后端单测 | 凭证 + 结账 + 报表 + book/journal/hr |
| TypeScript | 251 error（原 728） |
| ESLint | 114 error / 698 warning |

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

# API 冒烟
node tools/smoke-api.mjs

# E2E（需 2154 + 3154）
cd financial-cloud-ui && npm run test:e2e
```

## 已知后续

- 账套 4103/4104 权益数据建议人工核对
- TypeScript ~251 error、ESLint ~114 error（组件级问题待续）
- CI：`.github/workflows/ci.yml`（push 时跑 E2E）
