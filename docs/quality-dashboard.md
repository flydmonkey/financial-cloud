# 质量仪表盘

> 最后更新：2026-10-02（自主迭代第 1 轮，工资关联完整性）
> 本轮环境：新建 `financial_cloud_e2e_20261002_iteration1`，WSL Java21 后端2264，关闭自动备份。未正式发布；远端 CI、真实会计签字与第二服务器恢复仍待验收。

## 本轮实际验证

| 维度 | 结果 |
|------|------|
| 全部后端单元测试 | 517 通过，0 失败/错误/跳过，含新增工资服务测试 30 项 |
| 工资端到端 API 回归 | 1 个完整场景通过，0 失败/跳过/重试，覆盖草稿更正、过账保护、混批拒绝及金额不变 |
| 后端构建 | 独立输出目录构建成功，使用新构建服务执行回归 |
| 范围 ESLint / 独立代码审查 | 通过；失败半提交风险已修复 |
| OpenSpec 严格检查 | 本轮变更通过 |
| 完整前端/会计/隔离回归、远端 CI | 本轮未运行，保持历史证据与待验收状态 |

详见 [本轮验证及环境记录](testing/self-iteration-01-2026-10-02.md)。额外 E2E strict TypeScript 检查暴露既有 helper 类型问题，未记为通过；正常 Playwright 运行及范围 ESLint通过。下表为2026-09-29历史记录，不表示当前候选版本 CI 已成功。

## 2026-09-29 历史总览

| 维度 | 结果 |
|------|------|
| 后端单测（`./mvnw test`） | **通过**（BUILD SUCCESS，2026-09-29） |
| TypeScript（`npm run typecheck`） | **0 error**（由 ~129 清零；CI **硬门禁**） |
| ESLint（`npm run lint`） | **0 error** / **~905 warning** |
| 前端单测（`npm run test:unit`） | voucherWorkspace + voucherPrint；CI 纳入 frontend-check |
| GitHub CI（[#5](https://github.com/flydmonkey/financial-cloud/pull/5)） | **backend-test / frontend-check 绿**（e2e 仅 push main） |
| Playwright E2E | 约 **40** 个 `e2e/*.spec.ts`（本轮未全量复跑） |
| API 冒烟（`tools/smoke-api.mjs`） | 未本轮复跑 |

## 分模块（历史基线，仍适用）

| 模块 | API 冒烟 | E2E | 说明 |
|------|----------|-----|------|
| auth | ✓ | smoke | 低 |
| voucher | ✓ | 多条 | 中；Post-V2 改证路径已产品化 |
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

# 前端类型 / Lint / 单测
cd financial-cloud-ui && npm run typecheck
cd financial-cloud-ui && npm run lint
cd financial-cloud-ui && npm run test:unit

# API 冒烟
node tools/smoke-api.mjs

# E2E（需 2154 + 3154）
cd financial-cloud-ui && npm run test:e2e
```

## 已知后续

- TypeScript **已清零**；CI 对 `typecheck` 失败即红（不再 `continue-on-error`）
- ESLint：0 error，大量 warning；功能刀不要求清零 warning
- 吞吐 V2 / Post-V2 / TS：**程序已关闭** — [throughput-v2-backlog](superpowers/plans/2026-09-29-throughput-v2-backlog.md)
- 账套权益核对（人工）：资产负债表与科目余额一致检查仍建议发版前抽检
- 近端不以银行余额调节增强 / 税费向导 / 移动端 / AI / 税局直连扩容
