# 第二轮独立审查：候选发布证据门禁

审查日期：2026-10-03。审查对象：`release-candidate-evidence-gate` 的 proposal、spec、design、tasks、候选证据说明、核验器、来源采集模块、会计验收运行器、可信 profile、合成 fixture 和三份 Python 工具测试。

最终结论：独立审查发现 1 个需要修正的问题，最终实现已修复并通过闭合复核。在本次检查范围内未发现剩余必要修正；下文保留修复前复现、修复后证据及代码快照。该结论仅适用于工具实现，不表示真实候选的外部发布条件已经满足。

## 信任边界与执行范围

本次以离线核验为信任边界：检查文件结构、内容摘要、候选/运行身份、覆盖范围及具名复核要求。外部 CI 导出、实际服务观察、真实签字和恢复资料的真实性由原指定复核人确认；不把无法独立认证整套伪造资料当作本轮漏洞。

审查仅读取仓库中的实现和证据。动态复现使用 Python 标准库、`release_evidence_fixtures.create_fixture` 和 `verify_release_evidence.assess_bundle`，仅写临时目录；没有调用 collector、验收 runner、真实服务、网络、数据库、Playwright、npx、OpenSpec 或提交命令。仓库中审查者只编辑本报告。

检查重点包括：缺失或错误字段是否导致 false `releaseReady`；整数与布尔值混淆；原运行摘要与实际报告内容摘要交叉核对；CI、会计角色签字和恢复核对范围；脏源码与前后漂移；同实例恢复；open release blocker 和未经会计认可的金额差异；绝对/遍历/符号链接路径；显式输出覆盖候选、原证据或可信 profile；报告及来源摘要是否暴露凭据。

## REV-02-001：原验收运行没有绑定其实际观察环境（P1）

修复前位置：`tools/verify_release_evidence.py` 的 `_provenance`、`_environments` 与 `_acceptance`（约 410–570 行）。核验器核对了原 summary 的 runId、源码、产物和报告内容摘要，但 acceptance.environmentId 只需对应一份相同候选身份的环境记录。环境记录未引用具体 runId/原 summary，原 summary 的 database、api、frontend、declaredEnvironment 未与观察记录交叉核对。

离线复现步骤：

1. 在 `TemporaryDirectory` 创建完整合成 fixture，调用 `assess_bundle`，得到 `releaseReady=True`。
2. 只修改第一个 acceptance 引用的原 summary，令其 `api` 与 `frontend` 为 `https://other.example.invalid` 对应地址，令 `database` 为 `financial_cloud_e2e_DIFFERENT`，并写入相同值的 `declaredEnvironment`。
3. 更新该 summary 的引用 SHA-256，保留候选、原报告、运行身份及现有 reviewed environment。
4. 再次调用 `assess_bundle`，实际输出为 `releaseReady=True`，`blockers=[]`。

复现只使用本地合成文件；没有冒充真实外部证据。它表明真实验收在另一服务或数据源运行时，可以套用同候选的无关环境观察记录，仍被误判为来源充分。原完整 fixture 不含声明环境也能通过，进一步说明绑定字段缺失没有阻塞。

建议修正：具名复核环境记录明确绑定所覆盖的 `runId` 与完整原 provenance 引用/内容摘要；各验收组核对该绑定，并交叉核对原 summary 的 API、生产前端、库名和可获取 host/port，存在源码组件前端时也要求对应观察绑定。缺失或不一致应产生 `insufficient_scope` 或 `identity_mismatch`，继续保留离线技术复核的信任边界。

主任务已接受并修复此问题。最终位置：`_environments`（约 424 行）要求 `runs:[{runId,provenance}]`，核对原 provenance 文件内容摘要及 runId；`_runtime_binding`（约 566 行）核对该验收组所用 runId/摘要，再核对原 summary 和 declaredEnvironment 的端点、库名、已知 host/port 和可选 componentFrontend；`_acceptance` 调用该语义核验。

独立标准库临时文件复查结果：

| 变体 | 结果与关键条件 |
| --- | --- |
| 完整合成 fixture | `releaseReady=true`，无 blocker，显式标记 synthetic |
| 修复前原复现（修改原 summary，更新 acceptance 引用摘要） | `releaseReady=false`；观察 run 摘要不匹配，且 API/前端/库名分别 `identity_mismatch` |
| 主动更新 environment.runs 中的 provenance 摘要，再分别错置 API、frontend、database | 三项均 `releaseReady=false`；只由对应 `binding.run.*` / `binding.declaredEnvironment.*` 的语义 `identity_mismatch` 阻塞，未靠陈旧摘要失败遮掩 |
| 更新观察 run 摘要后错置已知 databaseHost 或 databasePort | 均 `releaseReady=false`，对应字段 `identity_mismatch` |
| 缺少 declaredEnvironment | `releaseReady=false`，API/前端/库名分别 missing |
| 缺少环境 runs | `releaseReady=false`，观察范围不足且验收 observedRun 不匹配 |
| 仅把环境覆盖 runId 改成 other-run，保持所有原摘要不变 | `releaseReady=false`，仅 `acceptance.000.binding.observedRun` 与 `environments.000.runs.000.identity` 为 `identity_mismatch` |
| declaredEnvironment 与原 summary/观察库名矛盾 | `releaseReady=false`，声明库名 `identity_mismatch` |
| 已提供但未实际观察绑定的 componentFrontend | `releaseReady=false`，组件端点 `identity_mismatch` |

此修复已闭合 REV-02-001，未发现通过更新观察文件哈希重新绕过端点/数据源语义核对的路径。

## Profile 与真实测试定义的独立核对

审查未运行清单生成命令。只读比对 `.e2e-run/release-evidence-inventory.json`、已保存的 `.e2e-run/release-evidence-profile-audit.json`、当前可信 profile、runner 的 AST `GROUPS` 定义和当前 TS 测试源文件：

- 75 个唯一 `(file, project, titlePath)` 完全一致；9 组会计验收为 50 个 case，单独账套隔离为 25 个 case。
- 9 组 runner 的 group/spec 映射与 profile 一致；profile 的额外组为 `book-isolation`。
- 75 个 inventory 的 file/line/title 与当前 TS 源行逐项匹配，未发现标题漂移。
- profile SHA-256 为 `3fe1a4f19e57c3092367c2774c32a69c7c4261f9679d8f982048b52cf5d4f0ab`；inventory 为 `34bd42e5ad7c049afc473de68e108ea60cba3a815a9f653b2af7933dd94af842`；workflow 为 `b133503489753e5935040491013e3021886193b81578d5e7c52cb61ca9ca80d3`，均与已有审计记录匹配。
- inventory 中没有任何执行 `results`，stats 为 `expected=0, skipped=75, unexpected=0, flaky=0`。这是 `--list` 得到的定义集合，不能作为真实验收成功证据。

CI profile 的必需 backend/frontend/e2e job 及必需 step 名称与当前 `.github/workflows/ci.yml` 一致；e2e 必需完整范围不能由 PR smoke 替代。当前 workflow 的 Vite 运行也不能单独证明生产前端实际验收。

## 离线测试与只读检查

中间版本执行：`python -B -m unittest discover -s tools -p test_release_evidence.py -q`。当时结果为 85 项、1 项失败：新增 Markdown 一致性断言没有反解 Windows 路径的双反斜线。已反馈并修正断言；未认定为高影响实现漏洞。

最终版本读取主任务保存的实际结果：`.e2e-run/iteration2-release-evidence-tests.log` 为 92 项通过，`.e2e-run/iteration2-provenance-tests.log` 为 12 项通过，`.e2e-run/iteration2-accounting-tools-tests.log` 为 22 项通过，合计 126 项，均 `OK`、无 skip。审查者未重复执行 collector/runner 测试。真实 Windows junction 逃逸测试使用受保护 `read_bytes` 断言，证明外部文件未被读取；硬链接输出别名保护也包含在最终离线测试中。

最终核验器的 AST 导入检查通过：仅标准库文件/JSON/摘要/格式处理，无 subprocess、网络、数据库、collector 或 runner 导入。独立的完整 fixture 和全部阻塞变体在每次 `assess_bundle` 前后核对所有输入文件 SHA-256，均未变化；完整 CLI 显式写出 bundle 外的 JSON/Markdown，两份内容与同一 assessment 相等，全部原输入摘要保持不变。只读边界检查与路径/输出保护未发现剩余必要修正。

补充文档复核完成：候选证据说明新增的 `release_evidence_fixtures.py --output` 与 `--missing` 两条命令，和生成器必需参数、新建/空目录保护及合成标记一致。缺项生成逻辑只移除 CI、accountant、restore 三类引用；随后完整/缺项核验的预期退出码分别为 0/1，与已验证行为一致。此次仅核对文档和已冻结实现，未重新运行测试；下表更新该说明文件的最终摘要，代码/profile/test 摘要保持不变。

## 版本快照与局限

中间审查版 `verify_release_evidence.py` SHA-256：`4c914288862811584c542bed1829421744a64003139e5255ce61869f83de708e`，对应环境绑定修复前的核验实现。

最终审查版文件 SHA-256（以字节内容计算）：

| 文件 | SHA-256 |
| --- | --- |
| `tools/verify_release_evidence.py` | `9de060bae49bcc6b03b6b47e5fcaabdbba063ea3b64ce3783a0bd487d03be545` |
| `tools/release_evidence_provenance.py` | `3e4a1c8bd1aeb8d266248a14d64a8b173b7e93f7cbd2776df947496e134e16b9` |
| `tools/run_accounting_acceptance.py` | `8010eef0c0c1325eaea33cdf5b4efc887eb0631a517e4762722361becb026a75` |
| `tools/release_evidence_profile.json` | `3fe1a4f19e57c3092367c2774c32a69c7c4261f9679d8f982048b52cf5d4f0ab` |
| `tools/release_evidence_fixtures.py` | `df7f5a88508c1ec82546386b4cfdff56dc849fb08cc822a1c16226ae9daf0a62` |
| `tools/test_release_evidence.py` | `32776d1c9322784634a61ff9dab461d9d5357d7606dfec1deba91fea5caacb71` |
| `tools/test_release_evidence_provenance.py` | `b5fdecabd5d9b0d97682bd73419017634a60f55d2dc117980dc10e6febf9d83c` |
| `tools/test_accounting_acceptance.py` | `4ad1442b6636600cb692482d65a7dce374a5a2ca3dfb9274e34cd735e39ca5e9` |
| `docs/testing/release-candidate-evidence.md` | `cf62e09231cae78c44a473c939deafa89ff8953b5d90bec9eeced5d67b0bfe55` |

本次没有验证真实候选已发布就绪，也没有执行真实完整 CI、干净候选生产产物验收、真实完整月份签字或独立实例恢复。合成 fixture 的成功仅证明核验行为；实际发布依赖仍须单独收集并复核。
