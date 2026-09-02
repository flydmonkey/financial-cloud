# 固定资产卡片复制 / 导出 / 导入

## 范围

- **复制**：`POST /api/fixed-asset/card/copy/{id}`
- **导出**：`GET /api/fixed-asset/card/export`
- **模板**：`GET /api/fixed-asset/card/import-template`
- **导入**：`POST /api/fixed-asset/card/import`（multipart `excelFile`）

## 导入规则

- 列与导出一致；模板含一行示例
- 编码已存在 → 跳过计入失败（不覆盖）
- 类别按编码；部门按名称；方法支持中文标签或枚举名；科目按编码（可回退 1601/1602）
- 返回 success / failed / errors[{row,code,message}]
