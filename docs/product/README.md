# 产品文档索引

> **文档性质**：以代码现状（as-built）为基准的产品说明书，并对照《小微企业财务软件产品需求文档（带原型完整版）》给出差距与路线。  
> **产品名称**：财务云（financial-cloud / jinbooks）  
> **文档版本**：2026-09-03  
> **适用对象**：产品、开发、测试、代账交付、交接

## 阅读路径

| 角色 | 建议顺序 |
|------|----------|
| 产品 / 立项 | [00-overview](00-overview.md) → [20-gap-analysis](20-gap-analysis.md) → [21-roadmap](21-roadmap.md) |
| 新同学上手 | [00-overview](00-overview.md) → 按业务模块 01～10 |
| 代账交付 | [01-account-book](01-account-book.md) → [03-voucher](03-voucher.md) → [06-settlement](06-settlement.md) → [05-statement](05-statement.md) |
| 开发对接 | 对应模块分册的「数据模型」「核心接口」章节 + [../modules/](../modules/) 技术文档 |

## 文档地图

### 总览

| 文档 | 说明 |
|------|------|
| [00-overview.md](00-overview.md) | 产品定位、角色、模块架构、技术架构、账套隔离、首页工作台 |

### 模块分册

| 文档 | 模块 |
|------|------|
| [01-account-book.md](01-account-book.md) | 账套管理与初始化向导 |
| [02-basic-settings.md](02-basic-settings.md) | 科目、辅助核算、期初余额、准则模板、系统参数 |
| [03-voucher.md](03-voucher.md) | 凭证录入、审核、过账、模板、打印 |
| [04-ledger.md](04-ledger.md) | 总账、明细账、科目余额表、凭证汇总 |
| [05-statement.md](05-statement.md) | 资产负债表、利润表、现金流量表、费用明细 |
| [06-settlement.md](06-settlement.md) | 期末结转、结账、反结账、往期锁定 |
| [07-fixed-asset.md](07-fixed-asset.md) | 固定资产卡片、折旧、变动、清理 |
| [08-cashier-journal.md](08-cashier-journal.md) | 出纳日记账：账户、流水、汇总 |
| [09-payroll.md](09-payroll.md) | 薪资、个税、社保公积金、工资凭证 |
| [10-system-admin.md](10-system-admin.md) | 用户、权限、审计、安全策略、机构 |
| [11-arap.md](11-arap.md) | 往来 L1+L2：余额、明细、对账单、账龄（核销未做） |

### 差距与路线

| 文档 | 说明 |
|------|------|
| [20-gap-analysis.md](20-gap-analysis.md) | 与 PRD 逐条对照 + P0/P1/P2 分级 |
| [21-roadmap.md](21-roadmap.md) | 迭代建议，对齐 openspec 与 PRD V1.1～V1.3 |

## 状态标记约定

模块分册中每条能力使用三档标记：

| 标记 | 含义 |
|------|------|
| **已实现** | 前后端闭环可用，有路由/接口/表证据 |
| **部分实现** | 主路径可用，但边界、合规或导出等能力缺失 |
| **未实现** | 代码中无对应页面、接口或业务表 |

不确定之处标注「待确认」，并说明依据。

## 术语表

| 术语 | 含义 |
|------|------|
| 账套 (Book) | 单家企业独立的财务数据载体，含科目、凭证、报表等 |
| 机构 (Institution) | 部署级租户/品牌配置，按域名解析；非完整 SaaS 多租户 |
| 准则 (Standard) | 会计准则模板（小企业会计准则 / 企业会计制度） |
| 辅助核算 (AssistAcc) | 科目延伸维度：项目、客户、供应商、部门、员工、存货 |
| 过账 / 记账 (sender) | 审核通过后写入科目余额的操作 |
| 结账 (checkout) | 月末锁定账期并推进 `current_account_date` |
| 反结账 (uncheckout) | 撤销最近一期结账副作用 |
| 期末结转 (carry) | 按模板生成损益/成本等结转凭证 |

## 维护约定

1. **以代码为准**：描述须能落到文件路径、类名、表名或路由；不写无法验证的承诺。
2. **改功能先改文档**：模块行为变更时，同步更新对应分册与差距清单。
3. **技术细节分流**：认证、包结构等见 [modules/platform.md](../modules/platform.md)；固定资产技术细节见 [modules/fixed-assets.md](../modules/fixed-assets.md)。
4. **对照 PRD**：外部需求文档路径为用户本机 `小微企业财务软件产品需求文档（带原型完整版）.md`；差距以 [20-gap-analysis.md](20-gap-analysis.md) 为准。

## 相关资产

| 资产 | 路径 |
|------|------|
| 仓库 README | [../../README.md](../../README.md) |
| OpenSpec 变更 | [../../openspec/changes/](../../openspec/changes/) |
| 数据库初始化 | [../../sql/README.md](../../sql/README.md) |
| E2E 测试 | `financial-cloud-ui/e2e/` |
