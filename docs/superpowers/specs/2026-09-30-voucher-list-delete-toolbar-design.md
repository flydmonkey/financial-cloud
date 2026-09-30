# 凭证列表 · 工具栏一级删除

> Date: 2026-09-30  
> Status: **approved** — 账务走查打磨；方案 1  
> Owner: product  
> Related: 账务走查（删除入口难找）；登录加固 [#8](https://github.com/flydmonkey/financial-cloud/pull/8)

## 1. 问题

做账员在凭证列表勾选后找不到批量删除：入口藏在「更多」下拉；走查时误判为无删除能力。行内删除图标已存在，但批量路径不显眼。

## 2. 目标

工具栏提供与「审核 / 过账」同级的一级「删除」按钮；禁用态说明可删规则；不改变删除业务规则与 API。

## 3. 方案（已选）

| 项 | 决定 |
|----|------|
| 位置 | `voucher-index` 工具栏右侧，「过账」与「更多」之间 |
| 样式 | `type="danger"` 文本按钮「删除」 |
| 启用 | 勾选中至少一条 `isDeletable` 凭证 |
| 禁用 tooltip | 「仅暂存/待过账且当期及以后凭证可删」 |
| 「更多」内删除 | **保留**（避免习惯断档） |
| 行内删除 | 不动 |
| 点击行为 | 现有 `handleDelete()` + 确认框 + `deleteBatch` |

## 4. Non-goals

- 不改 `isDeletable` / 后端删除校验
- 不重排审核、过账、更多菜单其它项
- 不做跨账套结账或新模块
- 不扩大到按钮级权限铺开

## 5. 验收

1. 勾选可删草稿 → 一级「删除」可点 → 确认后删除成功并刷新列表  
2. 未勾选或仅勾选已过账 → 按钮禁用，悬停见规则说明  
3. 「更多 → 删除」与行内删除仍可用  
4. 审核 / 过账按钮行为无回归  

## 6. 实现要点

- 文件：`financial-cloud-ui/src/views/voucher/voucher-index.vue`
- 计算 `canToolbarDelete`（所选中可删数 > 0）
- `el-tooltip` 包住禁用按钮（Element Plus 需包一层 span 才能对 disabled 显示 tip）
