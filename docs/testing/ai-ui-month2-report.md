# AI UI 第二月续测报告（扩大脚本注入）

- 时间：2026-09-30T23:48:28.754Z
- 登录/业务写入：允许脚本注入（in-page/Node fetch：draft→submit→audit→post、CF specify、结转）
- UI：报表截图、月结向导结账确认
- 汇总：PASS=11 WARN=8 FAIL=3

| ID | 状态 | 说明 |
|---|---|---|
| LOGIN-admin | PASS | 脚本注入登录 admin |
| LOGIN-reviewer | PASS | 脚本注入登录 ai_reviewer |
| M2-V01 | PASS | 记-1 status=completed sender=true |
| CF-M2-V02 | WARN | 无待指定非现金行 |
| M2-V02 | PASS | 记-2 status=completed sender=true |
| M2-V03 | PASS | 记-3 status=completed sender=true |
| CF-M2-V04 | WARN | 无待指定非现金行 |
| M2-V04 | PASS | 记-4 status=completed sender=true |
| CF-M2-V05 | WARN | 无待指定非现金行 |
| M2-V05 | PASS | 记-5 status=completed sender=true |
| M2-SB-BEFORE-CARRY | PASS | {"1002":{"expect":188000,"signed":188000,"bal":188000,"ok":true},"1122":{"expect":40000,"signed":40000,"bal":40000,"ok":true},"1405":{"expect":8000,"signed":8000,"bal":8000,"ok":true},"1601":{"expect":12000,"signed":12000,"bal":12000,"ok":true},"2001":{"expect":-40000,"signed":-40000,"bal":-40000,"ok":true},"2202":{"expect":-5000,"signed":-5000,"bal":-5000,"ok":true}} |
| CARRY-GEN-qm_jz_sr | WARN | Internal Server Error / Internal Server Error |
| CARRY-GEN-qm_jz_cbfy | WARN | Internal Server Error / Internal Server Error |
| M2-STEP1 | PASS | 本期未过账凭证与断号均已清理，可进入下一步 |
| M2-STEP2 | WARN | 固定资产折旧 不适用 本期无应计提折旧的资产。 计提折旧 刷新 必做损益结转 未完成 结转科目按账套会计准则自动匹配：小企业用 5401/5402/5601–5603/5711 等；企业会计制度用 5401/5405/5501–5503/5601 等。有余额才生成分录。 含主营业务成本（5401/6401） 编码 名称 状态 操作 qm_jz_sr 2-结转收入 未生成 生成并过账 qm_jz_c |
| M2-CLOSE-UI | WARN | m2 cannot leave carry |
| CARRY-API-qm_jz_sr | PASS | /api/settlementcarry/generate-voucher 2105444658062557185 |
| CARRY-POST2-qm_jz_sr | FAIL | 没有可以过账的凭证（需为已审核且未过账状态） |
| CARRY-API-qm_jz_cbfy | PASS | /api/settlementcarry/generate-voucher 2105444658326798337 |
| CARRY-POST2-qm_jz_cbfy | FAIL | 没有可以过账的凭证（需为已审核且未过账状态） |
| M2-STEP1 | WARN | 尚不能进入下一步：未过账凭证 6 张 |
| FATAL | FAIL | Error: m2 step1 blocked     at uiMonthClose (file:///workspace/financial-cloud-ui/scripts-ai-ui-month2.mjs:276:61)     at async file:///workspace/financial-cloud-ui/scripts-ai-ui-month2.mjs:515:7 |


---

# 第二月结账收尾

- 时间：2026-09-30T23:49:50.158Z
- PASS=8 WARN=0 FAIL=0

| ID | 状态 | 说明 |
|---|---|---|
| STEP1 | PASS | 本期未过账凭证与断号均已清理，可进入下一步 |
| STEP2 | PASS | 固定资产折旧 不适用 本期无应计提折旧的资产。 计提折旧 刷新 必做损益结转 已完成 结转科目按账套会计准则自动匹配：小企业用 5401/5402/5601–5603/5711 等；企业会计制度用 5401/5405/5501–5503/5601 等。有余额才生成分录。 含主营业务成本（5401/6401） 编码 名称 状态 操作 qm_jz_sr 2-结转收入 已过账 — qm_jz_cbfy  |
| VERIFY | PASS | 硬检已通过，可进入结账 序号 检查项目 类型 结果 说明 操作 1 未完成凭证检查 硬检 2 凭证号连续性检查 硬检 3 凭证借贷方余额的检查 硬检 4 损益结转-成本费用 硬检 5 损益结转-收入 硬检 6 固定资产折旧 硬检 不适用 本期无应计提折旧的资产 7 往来款项（应收应付/账龄） 硬检 应收合计 0，应付合计 0；逾期应收 0，逾期应付 0（账龄按凭证日期FIFO估算，逾期不阻断结账） |
| CHECKOUT | PASS |  |
| TERM | PASS | 当前账期：2026年03月 |
| HIST-BANK | PASS | jan=160000 feb=188000 |
| HIST-PROFIT | PASS | jan=-40000 feb=-53000 |
| SETTLE | PASS | [{"yp":"2026-01","status":6},{"yp":"2026-02","status":6},{"yp":"2026-03","status":1}] |
