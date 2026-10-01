# AI UI CI 子集报告

- **开始**：2026-10-01T03:49:26.865Z
- **API**：`http://127.0.0.1:2154`
- **结论**：SKIPPED（缺少 `AI-UI-20260930` 账套夹具）
- **账套总数**：1
- **命中**：（无）

## 阻塞说明

本子集依赖已存在的 `AI-UI-20260930` 系列账套（A/B/C 等），
不是从空库冷启动的 e2e。GitHub Actions 默认 `mysql` 初始化后没有这些夹具。

- 本地/Cloud Agent：保留夹具后执行 `npm run test:ai-ui:ci-subset`
- CI：脚本语法检查 + 本 job 的夹具探测会写出本报告；完整子集需夹具或 `workflow_dispatch` 自备环境

AI_UI_REQUIRE_BOOKS=0
