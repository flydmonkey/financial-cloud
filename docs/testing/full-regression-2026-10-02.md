# 合入 main 前的全量测试（2026-10-02）

当前工作分支为 main。本次将资产负债表真实性、损益结转、全模块账套隔离及对应验收改动作为一个提交合入 main。临时脚本、运行日志、技能目录和测试上传数据不纳入提交。

| 验证 | 结果 | 日志 |
| --- | --- | --- |
| `mvn -o test` | 450 通过，0 失败、0 跳过 | `.e2e-run/full-backend-tests.log` |
| `npm run typecheck` | 通过 | `.e2e-run/full-frontend-checks.log` |
| `npm run test:unit` | 23 通过，0 失败、0 跳过 | 终端输出；首次启动因系统账号查询 ENOMEM 失败，单独复跑通过 |
| `npm run build` | 通过 | `.e2e-run/full-frontend-build.log` |
| `npm run test:e2e:full` | 211 通过，0 失败、0 跳过，全部 47 个测试文件 | `.e2e-run/full-e2e-isolated.log` |
| 补丁与规格 | 暂存补丁空白检查及 OpenSpec 严格校验通过 | Git / OpenSpec 输出 |

直接运行 `playwright test` 首次出现 3 个失败、2 个跳过及 14 个未运行：两套 Golden 数据集要求空白账套，而年末套件会重置到 12 月，污染后续跨期测试。新增 `tools/run_full_e2e.py` 和 `test:e2e:full`，顺序执行普通套件、资产负债表 Golden、利润表 Golden、年末套件，每组重置隔离库；分别 192、11、6、2 项通过。原有断言保留。

数据库为 `financial_cloud_e2e_20261001`，全量执行器强制检查隔离库名前缀。没有操作原业务库或部署服务。本次合入是当前 main 的本地提交，未推送远端。
