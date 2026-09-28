# Tasks: 账套备份与恢复

## 1. 表规格与导出

- [ ] 1.1 定义 `BackupTableSpec` 声明式清单（表名、过滤方式、FK 边），覆盖 design §2 全部包含表与显式排除表
- [ ] 1.2 `BookBackupService.export`：逐表查询 → JSONL → sha256 → manifest → ZIP
- [ ] 1.3 `POST /api/book/backup/export` 控制器（权限校验、Content-Disposition 文件名）
- [ ] 1.4 导出审计：history_event 记录

## 2. 恢复

- [ ] 2.1 manifest 解析与校验（formatVersion、表齐全、行数、sha256），失败即拒
- [ ] 2.2 新账套壳创建（复用账套初始化，元信息取自 manifest.book）
- [ ] 2.3 ID 重映射器：按表分配新主键 + FK 边改写；人员/部门外键置空
- [ ] 2.4 单事务灌数 + 失败整体回滚（含账套壳）
- [ ] 2.5 `POST /api/book/backup/restore`（multipart）+ 审计

## 3. 前端

- [ ] 3.1 账套列表行操作「导出备份」（下载 ZIP）
- [ ] 3.2 「恢复备份」上传对话框：选择 ZIP → 确认 → 展示新账套结果并刷新列表

## 4. 测试与文档

- [ ] 4.1 单测：备份包完整性（表清单、行数、校验和）
- [ ] 4.2 单测：往返一致性（导出→恢复→凭证数/余额表关键金额比对）
- [ ] 4.3 单测：篡改 manifest / 缺表 / 版本不符全部拒绝且无残留写入
- [ ] 4.4 单测：schema 守卫——所有含 book_id 的表在规格清单或排除清单中
- [ ] 4.5 更新 docs/product/20-gap-analysis.md §1.5/§8.3 与 21-roadmap.md S3 状态
