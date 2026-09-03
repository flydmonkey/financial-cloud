# 文档索引

财务云（financial-cloud）项目文档。

## 产品文档（现状 + 差距）

以代码为准的产品说明书，并对照《小微企业财务软件产品需求文档》给出差距与路线。

| 文档 | 说明 |
|------|------|
| [product/README.md](product/README.md) | 产品文档索引、阅读路径、术语表 |
| [product/00-overview.md](product/00-overview.md) | 产品总览：定位、架构、账套隔离、首页 |
| [product/01-account-book.md](product/01-account-book.md)～[10-system-admin.md](product/10-system-admin.md) | 十大模块分册 |
| [product/20-gap-analysis.md](product/20-gap-analysis.md) | 与 PRD 逐条差距 + P0/P1/P2 |
| [product/21-roadmap.md](product/21-roadmap.md) | 迭代路线（对齐代账商业化与 PRD 版本） |

## 模块文档（技术）

| 文档 | 说明 |
|------|------|
| [modules/fixed-assets.md](modules/fixed-assets.md) | 固定资产：类别、卡片、折旧、变动、报表 |
| [modules/ledger-and-reports.md](modules/ledger-and-reports.md) | 账簿菜单、总账、费用明细、现金流量表 |
| [modules/platform.md](modules/platform.md) | 认证、包结构、配置与基础设施 |

## 测试与质量

| 文档 | 说明 |
|------|------|
| [testing/financial-cloud-voucher-report-test-cases.md](testing/financial-cloud-voucher-report-test-cases.md) | 凭证与三大报表测试用例 |
| [quality-dashboard.md](quality-dashboard.md) | 质量快照（单测、E2E、Lint） |

## 数据与科目

| 文档 / 文件 | 说明 |
|-------------|------|
| [subject-import-compatibility.md](subject-import-compatibility.md) | 标准科目导入兼容性（脚本自动生成，勿手改） |
| [小企业会计准则科目.xlsx](小企业会计准则科目.xlsx) | 标准科目模板（standard_id=1） |
| [企业会计制度科目.xlsx](企业会计制度科目.xlsx) | 标准科目模板（standard_id=2） |
| [../sql/README.md](../sql/README.md) | 数据库全量初始化 |

## 前端

见 [financial-cloud-ui/readme.md](../financial-cloud-ui/readme.md)。
