# 候选版本证据核验（v1）

本入口离线核对一个明确候选的发布证据。它不运行验收、联网查询 CI、连接数据库、生成签字或部署。核验通过仅表示提供的资料满足本地验收 profile；外部来源真实性和观察记录仍依赖具名人员复核。

## 使用与结果

```powershell
python tools/verify_release_evidence.py --bundle <证据目录>/candidate.json
python tools/verify_release_evidence.py --bundle <证据目录>/candidate.json --json-output <输出目录>/assessment.json --markdown-output <输出目录>/assessment.md
```

默认向 stdout 输出 JSON；仅显式输出参数写报告，输出不能覆盖输入证据。退出码 0 为资料就绪，1 为仍有阻塞，2 为根文件不可解析/协议不支持等输入错误。每个条件包含状态、证据引用、原因和下一步。状态为 `satisfied`、`missing`、`failed`、`identity_mismatch`、`insufficient_scope` 或 `pending_review`。

这里的发布就绪必须区分真实候选与合成测试样例。合成样例使用 `synthetic: true`，仅验证核验逻辑，不能作为真实 CI、会计认可或恢复演练证据。

完整和缺项样例可在两个新建或空目录生成；命令只写合成文件：

```powershell
python tools/release_evidence_fixtures.py --output <新的完整样例目录>
python tools/release_evidence_fixtures.py --output <新的缺项样例目录> --missing
```

分别调用核验命令，完整样例返回 0 且 `synthetic:true`，缺项样例返回 1 并同时列出 CI、会计签字和独立恢复缺失。真实 Windows junction 逃逸和证据前后内容不变在工具测试中验证。

## 根协议

证据目录是 `candidate.json` 的父目录。所有引用均为 `{ "path": "目录内相对路径", "sha256": "64 位小写十六进制" }`；绝对路径、目录穿越、符号链接逃逸均拒绝。JSON 使用 UTF-8。支持材料须非空，除结构化记录外也可以是原日志、签字文件或匿名核对附件。

```json
{
  "schemaVersion": 1,
  "synthetic": false,
  "candidate": {
    "candidateId": "由实际候选定义",
    "commitSha": "40位完整提交SHA",
    "source": {"state": "clean", "fingerprint": "源码清单SHA256"},
    "artifacts": {
      "backend": {"sha256": "实际JAR SHA256"},
      "frontend": {
        "kind": "production",
        "sha256": "生产前端文件清单摘要",
        "manifest": {"path": "artifacts/frontend.json", "sha256": "文件内容摘要"}
      }
    }
  },
  "profile": {"id": "jinbooks-release-v1", "version": 2, "sha256": "可信profile文件摘要"},
  "evidence": {
    "acceptance": [],
    "environments": [],
    "ci": {"path": "ci.json", "sha256": "..."},
    "accountant": {"path": "accountant.json", "sha256": "..."},
    "restore": {"path": "restore.json", "sha256": "..."},
    "issueReview": {"path": "issues.json", "sha256": "..."}
  }
}
```

上例是字段说明，不是可通过的证据。缺项不得用当前 SHA、假签字或历史绿色结果补齐。脏源码或未知源码状态阻塞发布资料就绪，但不影响正常隔离环境研发验收。

所有运行和外部记录都携带相同 `identity`：

```json
{
  "candidateId": "候选编号",
  "commitSha": "40位完整提交SHA",
  "sourceFingerprint": "源码清单SHA256",
  "backendSha256": "实际JAR SHA256",
  "frontendSha256": "生产前端文件清单摘要"
}
```

每个值必须与 candidate 相符。不同运行可以共同提供同一候选资料，不能混合不同版本、源码或产物。

## 文件清单、来源和验收 profile

生产前端 manifest 为 `{kind:"production", files:[{path,size,sha256}], sha256}`。files 按相对 path 排序，路径唯一且安全、size 为非负整数；清单摘要为 `json.dumps(files, sort_keys=True, separators=(',', ':'), ensure_ascii=False)` UTF-8 字节的 SHA-256。candidate.frontend.sha256 与清单摘要相同，manifest 引用摘要校验整个 JSON 文件。产物实际运行与数据源绑定须另有观察记录，清单本身不能证明服务使用了这些文件。

来源采集的 `sourceBefore`/`sourceAfter` 保存完整 Git SHA、tracked/untracked 状态、非 ignored 文件清单、同样 canonical JSON 算法的 fingerprint、采集时间及是否稳定。保留完整采集清单和执行材料用于技术复核；candidate 的 fingerprint 引用实际采集值。采集期间或测试前后源码变化会阻塞发布身份。核验器不自行执行 Git，也不把源码指纹当作独立认证。

`tools/release_evidence_profile.json` 是由维护者审查的可信范围，证据包不能自行删减。当前 profile 为 `jinbooks-release-v1`、version 2，根协议仍为 schemaVersion 1。会计测试定义包含 9 组验收的 50 个 case 和账套隔离的 25 个 case，共 75 个；这是 **`--list` 得到的定义集合，未执行测试**。case 身份为 spec 文件 basename、project 和 describe 层级 + 测试 title；不包含最外层文件 suite 标题。未来变更测试时更新并审查 profile，不把历史数量固定为合格条件。

维护 profile 时，在明确候选代码和 `E2E_ENABLE_UI=1` 下运行对应 spec 的 `playwright test --list --reporter=json`，保存 UTF-8 原始定义，逐项比对 profile 的组/spec/project/titlePath；`--list` 不运行 global setup 或业务测试，结果中的 skipped 不可冒充执行报告。CI profile 固定完整 job/step 范围，PR 冒烟不能替代完整验收。

## 自动验收记录

`evidence.acceptance` 数组中每一组记录：`group`、`identity`、`runId`、`startedAt`、`finishedAt`、`environmentId`、`exitCode`、`report` 和 `provenance` 引用。必须覆盖 profile 的全部组且无重复；每组可以有独立运行。原始 Playwright JSON 须有 stats、errors、suites、具体 test/results，具体 case 与完整范围对应，首个尝试成功且无失败、跳过、flaky 或重试；统计与实际结果一致。仅 `summary.passed=true` 不构成证据。

`provenance` 指向实际运行时生成的原始摘要，须有 `schemaVersion:1`、runId、开始/完成时间、`status:"completed"`、sourceBefore/sourceAfter、artifacts/artifactsAfter、sourceChanged/artifactsChanged 和 groups。测试前后均为 clean，提交、源码指纹及产物身份与候选一致，无变化；找到对应 group 的原始 report.sha256、退出码和运行身份，与外层记录及实际报告交叉核对。sourceChanged/artifactsChanged 为 unknown 不能证明稳定。原摘要 report.path 相对其原运行目录，组包可以保留子目录，但内容摘要必须相同。历史摘要没有这些来源或属于旧 SHA 时，不能给外层填当前 SHA 后通过。

现有 `run_accounting_acceptance.py` 补充来源、产物、声明环境（含可选源码组件地址）、报告/日志哈希和结束状态，仍逐组执行独立验收、拒绝业务库名。它不收集或推定人工签字，`reviewedBinding` 为 unknown、`releaseReady` 始终 false。记录传入的库名/URL 和本地 JAR 属于 declared；运行器成功只能说明其自动测试通过。账套隔离的独立运行也必须保存相同来源协议，不能仅引用无来源的历史隔离报告。

## 新增 CI 验证范围

第五轮在原有 CI 范围上增加 `verification-tools` 和 `payroll-mysql-it` 两个独立 job，PR 与 main push 都必须执行；profile version 2 同时追加这两个 job 及其 11 个必需 step，保留原有检查。旧 CI 只有 backend、frontend 与会计 job，即使全部绿色，也会因缺少新增范围而阻塞当前 profile 的完整性判断。

`verification-tools` 分别运行证据工具、SQL 离线边界、初始化与证据安全三组，保存逐项结果、原始日志和测试前后源码身份。第五轮本地实际通过 126、13、81 项，共 220 项。它们使用文件与模拟输入，不连接数据库；合成完整候选通过仅是工具反例验证。

`payroll-mysql-it` 固定 MySQL `9.4.0-oraclelinux9`，从当前仓库 DDL 在不存在的专库建立空结构，并记录实际版本、隔离级别、索引和锁观察权限。prepare 生成本次运行标识，Maven 将它与完整 checkout SHA、源码指纹写入原 XML；verify 核对完整 21 项 case、退出码、日志、数据库事实及 UTC 时间顺序，拒绝旧 XML 重新绑定或仅声明 `passed=true` 的环境记录。成功或失败的步骤都会尝试上传证据；服务或 checkout 之前失败可能尚无文件，缺必需文件仍为失败。

本地原始执行记录和限制见 [第五轮记录](self-iteration-05-2026-10-03.md)。本地实际为 Windows、Java 25 和 `mvn -o`，并非已执行 workflow 的 Ubuntu、Java 21 与 `./mvnw` 环境。截至第五轮归档时未提交推送，也没有新远端 CI 结果；后续 main 集成提交的结论须按其实际 SHA 和远端运行另行核对。job 自产元数据不构成提供方独立认证；候选核验器检查 CI 原记录的 job/step 完整性，原始测试报告与执行来源仍需在实际候选资料中保留并复核。

## 实际环境绑定

`evidence.environments` 是 JSON 记录引用数组。每份记录包括：

- `environmentId`、`identity`、`instanceId`、`apiUrl`、`frontendUrl`、`frontendKind:"production"`。
- `datasource:{host,port,database,instanceId}`，对应实际服务数据源；不同库名不能自行证明独立实例。
- `observation:{method,observedAt}`，描述实际观察方法，不能只写 declared/unknown。
- `review:{reviewer,reviewedAt,conclusion:"approved"}`，具名技术复核与结论。
- `runs:[{runId,provenance:{path,sha256}}]`，覆盖的实际运行及其完整原始摘要；每组验收的原摘要必须包含在观察范围内。
- 至少一个 `supportingEvidence` 引用，支持启动/部署产物、实际服务和数据源的对应关系。

当前没有已验证的运行身份 API，所以实际观察和技术复核是必要输入。仅 URL、健康响应或传入库配置会得到范围/来源不足。输出隐藏连接凭据和原始签字内容。

原运行摘要的 database/api/frontend 及 declaredEnvironment 同名字段必须与这份观察一致。采集的 databaseHost/databasePort 若已知也须匹配观察的数据源；unknown 不能自动当作观察值。可选 componentFrontend 由源码组件服务提供时，观察同时记录 componentFrontendUrl，并复核相同源码指纹。给测试换另一服务、库或前端后，不能沿用旧观察材料或仅修改证据包外层 environmentId。

## 外部发布证据

各 JSON 原记录必须包含对应候选的完整 identity、approved review 及至少一个 supportingEvidence。核验读取这些原记录并核对哈希和具体字段，不能仅相信外层声明。

| 记录 | 额外必需字段与结论 |
|---|---|
| ci | provider、runId、url、headSha、workflow、conclusion:"success"；jobs 数组含 name/conclusion/steps，必需 job 和 step 均 success |
| accountant | month:"YYYY-MM"、realMonth:true、completeMonth:true；comparisons 含 item/outcome/accountantApproved；signoffs 含 role/name/signedAt/evidence，角色覆盖 accountant/operator/technical |
| restore | source/target 各含 instanceId/datasourceId，均为独立身份；backup 文件引用；comparisons 含 item/outcome:"matched"，范围覆盖业务数据、附件、账簿和三表 |
| issueReview | issues 含 id/releaseBlocking/status/reviewed/owner/resolutionPlan；differences 含 id/explanation/accountantApproved；所有发布阻塞项已 closed 且 reviewed，金额差异已说明且获会计认可 |

accountant.comparisons 按 profile 核对期初、凭证、工资、资产、往来、余额/账簿、三表、月结、交付恢复，outcome 为 matched 或 explained，accountantApproved 为 true。signoffs 的 evidence 引用指向实际签字材料，不能代填。无问题/差异可以提供空数组，但台账复核、身份和支持材料仍必需；非阻塞 open 问题保留责任人与计划，不自动假称 closed。

真实月份试账与问题记录沿用 [试账模板](accountant-month-pilot.md)，独立实例恢复范围见 [便携备份说明](portable-book-backup.md)。恢复丢失、金额错误、跨账套访问、无法结账属于发布阻塞；其余证据齐全仍不能覆盖这些问题。

## 历史证据和当前状态

首轮工资修复已提交为 `4571047`，历史 517 项后端测试与工资 API 回归是在 `9758a9a` 上未提交工作区采集。它们保留原始身份，不改绑现在的提交。历史完整 50 项会计验收和 25 项隔离报告也不自动代表未来候选通过。

当前候选的实际完整 CI、干净代码与生产产物运行绑定、完整会计/隔离重验、真实月试账签字、问题台账复核及第二实例恢复仍待实际材料。工具实现与 fixture 验证完成可以单独交付；这些外部发布条件须真实执行和复核后更新。
