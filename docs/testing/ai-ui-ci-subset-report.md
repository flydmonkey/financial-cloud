# AI UI CI 子集报告

- **开始**：2026-10-01T04:51:36.221Z
- **API**：`http://127.0.0.1:2154`
- **结论**：SKIPPED（缺少 `AI-UI-20260930` 账套夹具）
- **账套总数**：0
- **命中**：（无）
- **关联**：见 `docs/testing/ai-ui-mergeable-wrapup-report.md`

## 阻塞说明

本子集依赖已存在的 `AI-UI-20260930` 系列账套（A/B/C 等）及脚本硬编码实体（工资员、固资卡等），
不是从空库冷启动的 e2e。GitHub Actions 默认 `mysql` 初始化后没有这些夹具。

空库仅按名建空账套会让门闸变绿、随后子集 FAIL，故 CI 继续 fixture-gated SKIP。

- 本地/Cloud Agent：保留夹具后执行 `AI_UI_REQUIRE_BOOKS=1 npm run test:ai-ui:ci-subset`
- CI：`AI_UI_REQUIRE_BOOKS=0` 无夹具时 SKIP（exit 0）；有夹具则实跑 5 套件

AI_UI_REQUIRE_BOOKS=1（本机门闸复测确认无夹具时 exit 1）
