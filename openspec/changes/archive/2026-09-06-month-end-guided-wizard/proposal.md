## Why

专业月结向导（`professional-month-end-close`）已具备硬门槛与四步骨架，但会计仍须跳转凭证列表/期末结转页完成过账、断号整理与结转过账；系统校验失败时常仅出现全局 error toast，无法在向导内定位并修复。代账月结需**单页五步引导**：在步骤内完成凭证整理与结转过账，校验结果内联展示并可跳回所属步骤。

## What Changes

- 将月结向导扩展为 **五步**：人工确认 → **凭证整理** → 计提与结转 → 系统校验 → 结账；每步「下一步」受步骤就绪门禁约束。
- **凭证整理**（新步骤）：向导内列出未过账凭证、批量提交/审核/过账；断号检查与一键整理；两项均通过才可进入计提结转。
- **计提与结转**：向导内折旧计提 + 必做损益结转逐行**生成并过账**；`qm_jz_cbfy` 明确含**主营业务成本**（小企业 `5401` / 企业 `6401` 科目别名）。
- **系统校验**：`verify` 结果内联展示；失败硬检项提供「去处理」跳回所属步骤；预期 verify 业务失败**不得**以全局 error toast 作为唯一反馈（`silentError`）。
- **Non-goals**：独立销售成本结转 `qm_jz_xscb` 硬门槛、独立年结入口、向导内完整凭证编辑器。

## Capabilities

### New Capabilities

- （无独立 capability；本 change 增量扩展 `month-end-close`。）

### Modified Capabilities

- `month-end-close`: 五步引导、凭证整理步骤门禁、向导内过账/断号/结转过账、静默 verify UX、`qm_jz_cbfy` 主营业务成本科目覆盖说明。

## Impact

- **前端**：`settle-period.vue` 拆分为 `wizard/*` 五步组件；`useMonthEndWizard` 集中门禁与动作；`Request.ts` / `settlement.ts` 支持 `silentError`。
- **后端**：`SubjectCodeCompat` 6401→5401 别名回归测试；`qm_jz_cbfy` 结转科目覆盖（已有 alias 机制）。
- **文档**：`docs/product/06-settlement.md` 五步流程与 UX 说明。
- **测试**：`SubjectCodeCompatTest`；E2E/smoke 覆盖步骤门禁与 verify 内联（Task 6）。
