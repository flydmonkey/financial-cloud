# 自主迭代第 2 轮：候选版本证据核验

日期：2026-10-03。OpenSpec：`release-candidate-evidence-gate`；路线见 [产品迭代路径](../product/23-self-iteration-roadmap.md)。本轮基于 `4571047d0b836848190e8952785e8590cee606d9` 上的未提交工作区实现并验证 Python 工具；最终代码字节哈希、命令结果和证据文件摘要保存于 `.e2e-run/iteration2-evidence.json`。本轮未提交、推送或发布。

2026-10-03 收口补记：6 条要求已同步 `release-candidate-evidence` 主规格，全部 28 项主规格严格校验通过；11/11 任务完成后归档至 `openspec/changes/archive/2026-10-03-release-candidate-evidence-gate/`。另已实查第一轮 `4571047` 的完整 [CI #157](https://github.com/flydmonkey/financial-cloud/actions/runs/37029890945) 成功；该结果对应第一轮提交，不覆盖第二轮未提交工具代码。

## 实现结果

- `tools/verify_release_evidence.py` 离线核验候选、可信 profile、原运行来源、报告完整范围、实际环境绑定、CI、会计签字、独立实例恢复及问题台账；输出 JSON/Markdown 和每个阻塞条件的下一步。
- `tools/release_evidence_provenance.py` 采集测试前后完整提交与源码状态、文件清单指纹、可获取的 JAR/生产前端身份、脱敏声明环境及变化情况；传入设置和本地产物始终属于 declared。
- 现有会计验收运行器在成功、部分失败、启动异常与中断时保存原报告/日志哈希和最终状态，缺失败统计字段不能默认为零。人工签字保持 pending，运行器不生成发布就绪结论。
- `tools/release_evidence_profile.json` 对应当前 9 组会计验收和独立账套隔离的全部具体 case；证据包不能自行降低范围。`tools/release_evidence_fixtures.py` 可生成完整/缺项样例，全部显式标记 synthetic。

协议与命令见 [候选证据核验说明](release-candidate-evidence.md)，独立审查见 [审查记录](self-iteration-02-review.md)。工具默认不写文件，显式报告输出不能覆盖原证据、整个 bundle 或可信 profile；核验器不启动服务、执行验收、联网或连接数据库。

## 必要验证

| 验证 | 实际结果 | 证据 |
|---|---|---|
| `python -m unittest discover -s tools -p test_release_evidence.py` | 92 通过，0 跳过 | `.e2e-run/iteration2-release-evidence-tests.log` |
| `python -m unittest discover -s tools -p test_accounting_acceptance.py` | 22 通过，0 跳过 | `.e2e-run/iteration2-accounting-tools-tests.log` |
| `python -m unittest discover -s tools -p test_release_evidence_provenance.py` | 12 通过，0 跳过 | `.e2e-run/iteration2-provenance-tests.log` |
| 合计 | **126 项通过，0 失败/错误/跳过** | `.e2e-run/iteration2-evidence.json` |
| OpenSpec change 严格校验 | 通过 | `.e2e-run/iteration2-openspec-validation.log` |
| `git diff --check` | 通过；保留仓库既有 LF/CRLF 提示 | 主会话实际命令输出 |

负例覆盖：旧 SHA、脏源码/来源漂移、产物不符、缺来源摘要、假绿总计、组/case 遗漏与重复、字段缺失/错误、失败/跳过/flaky/重试、错误服务/数据源绑定、CI 冒烟或跳过、签字缺角色、未获会计认可的金额差异、同实例恢复和未闭合发布阻塞项。

路径验证包括绝对路径、UNC、drive-relative、目录穿越、硬链接输出别名，以及本机实际创建 Windows directory junction 后的 resolve 逃逸。外部文件读取保护断言和全部输入证据前后哈希检查均通过。测试使用临时文件和 mocked 验收进程，没有业务数据库写入。

## 实际 CLI 集成验证

| 输入 | 实际退出码与结论 | 证据 |
|---|---|---|
| 完整合成资料 | 0；`releaseReady=true, synthetic=true`，841 条条件满足 | `.e2e-run/iteration2-complete-assessment.json` / `.md` |
| 缺项合成资料 | 1；同一次输出 CI/会计/独立恢复三项缺失 | `.e2e-run/iteration2-missing-assessment.json` / `.md` |
| 实际本地快照与未改变的首轮历史资料 | 1；`releaseReady=false, synthetic=false`，41 条阻塞条件 | `.e2e-run/iteration2-current-assessment.json` / `.md` |
| 不支持的根协议版本 | 2；输入错误 | `.e2e-run/iteration2-invalid-cli.log` |

完整合成资料仅说明核验程序的成功路径，不代表真实候选已发布就绪。缺项及实际本地资料都得到预期阻塞结果；不是测试失败。

实际本地演示保留首轮 `9758a9a` 上未提交工作区的原始身份，只引用原工资报告与原证据 JSON 的未变副本，不给历史结果补写当前 SHA。原文件摘要前后相同，证明见 `.e2e-run/iteration2-current-evidence/historical-integrity-check.json`。本地当前源码为 dirty，可获取的产物仍是 declared，没有实际运行绑定。

## 独立审查与修复

独立 agent 发现并复核闭合 `REV-02-001`：原验收运行可能套用同候选的无关环境观察记录。环境记录现在必须绑定 runId 与原始来源摘要，API/前端/库名、已知 host/port 及组件前端声明与实际观察逐项核对。主动更新引用哈希但仍错置端点或数据源的反例也被拒绝。

审查者另行核对 75 个 profile case 与当前 TS 定义逐项一致、9 组 runner 映射及 CI 必需步骤一致；检查了不调用进程/网络/数据库的核验器边界、原证据不变和报告输出保护。最终检查范围内没有剩余必要修正。

## 状态与外部依赖

R0-002 工具实现、fixture 验证和独立审查完成，11 项实施任务已闭合。75 个 case 的 profile 是 **测试定义集合，本轮未执行这 75 个业务场景**；也未重复后端 517 项、前端全量检查或正式业务验收。

真实候选仍缺：

- 干净候选、对应生产产物和实际服务/数据源运行绑定。
- 对应候选的完整会计及账套隔离实际重验、实际完整远端 CI。
- 真实完整月份试账、会计/操作/技术签字及问题台账复核。
- 独立目标实例恢复，业务数据、附件、账簿和三表核对。

本轮工具验证完成，不解除以上发布条件。来源真实性继续依赖具名复核人根据实际原资料确认，离线工具不提供独立真实性认证或部署授权。
