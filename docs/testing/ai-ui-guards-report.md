# AI UI Guards / Export Smoke（主账套 A）

- time: 2026-10-01T03:11:12.502Z
- bookId: 2105377998655979522

| ID | Status | Detail |
|---|---|---|
| SWITCH-A | PASS | bookId=2105377998655979522 |
| CLOSED-TARGET | INFO | vid=2105384073723641857 book=2105377998655979522 status=completed sender=2105387387939979264 |
| CLOSED-EDIT-BLOCK | PASS | code=2 msg=已结账期间不允许新增或修改凭证（当前开放账期 2026-03） |
| CLOSED-UNSENDER-BLOCK | PASS | code=2 msg=没有可以反过账的凭证（需为已过账且所在期间未结账） |
| UNCHECKOUT-NON-LATEST | PASS | code=2 msg=只能反结账最近已结期间[2026-02]，不能反[2026-01] |
| UNCHECKOUT-LATEST | PASS | msg=反结账完成，当前账期已回到[2026-02] |
| RECHECKOUT-FEB | PASS | code=0 msg=结账完成 |
| UI-UNCHECKOUT-ENTRY | PASS | onA=true hasBtn=true snip=财务云 当前账期：2026年03月 账套： AI-UI-20260930-主账套A 系统管理员 (admin) 仪表盘 凭证 账簿 报表 结账 期末处理 结账 账期列表 出纳 薪资 固定资产 往来管理 账套管理 准则管理 系统设置 首页 / 结账 / 账期列表 期末处理 结账 结账列表 选择年度 查询 导出本月账本包 月份 状态 操作 01 月 02 月 反结账 03 月 04 月 05 月 06 月 07 月 08 月 09 月 10 月 11 月 12 月 共 12 条  |
| UI-EXPORT-ENTRY | PASS | export=2 pdf=1 |
| EXPORT-DOWNLOAD | PASS | file=资产负载表2026-03 2026-10-01 03_11_11.xlsx size=11139 |
| EXPORT-CONTENT-SMOKE | PASS | name=资产负载表2026-03 2026-10-01 03_11_11.xlsx size=11139 |

## Notes

- First mistaken run hit 专项B session; book B income voucher was unsent then re-posted (`senderId` restored).
- This run forces `GET /api/users/switchBook/{bookA}` before API checks.
