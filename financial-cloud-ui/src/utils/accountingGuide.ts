export type CheckAction = { step?: 1 | 2; path?: string; suggestion: string }

export function closingCheckAction(item: string): CheckAction {
  if (/未过账|未完成凭证/.test(item)) return { step: 1, suggestion: '打开未过账凭证，完成提交、由其他审核人审核后过账；被拒绝凭证先修改并重新提交。' }
  if (/凭证号|断号|连续/.test(item)) return { step: 1, suggestion: '在凭证整理步骤检查断号，再执行断号整理。' }
  if (/借贷|平衡/.test(item)) return { step: 1, suggestion: '核对凭证借贷金额和期初余额，修正异常凭证后重新检查。' }
  if (/结转/.test(item)) return { step: 2, suggestion: '在计提与结转步骤生成并过账；已有结转后新增业务时，补充结转剩余余额。' }
  if (/折旧/.test(item)) return { step: 2, suggestion: '完成本期资产折旧计提，再重新检查。' }
  if (/往来|应收|应付|账龄/.test(item)) return { path: '/arap/aging', suggestion: '核对账龄、往来明细与收付款核销；确认处理后返回月结重新检查。' }
  return { suggestion: '根据检查说明核对业务数据；仍无法定位时请联系账套管理员，处理后重新检查。' }
}

export function validYearPeriod(value: unknown): string {
  return typeof value === 'string' && /^\d{4}-(0[1-9]|1[0-2])$/.test(value) ? value : ''
}

export function previousYearPeriod(value: unknown): string {
  const term = validYearPeriod(value)
  if (!term) return ''
  const year = Number(term.slice(0, 4))
  const month = Number(term.slice(5, 7))
  return month === 1 ? `${year - 1}-12` : `${year}-${String(month - 1).padStart(2, '0')}`
}

export const accountingGuide = [
  { title: '1. 建账与检查', description: '确认企业、会计准则和启用月份。已有账套先核对顶栏名称，避免录错账套。', links: [{ label: '账套管理', path: '/books/index' }] },
  { title: '2. 导入期初', description: '下载期初模板，按末级科目编码填写并导入；先核对借贷平衡，再录业务凭证。', links: [{ label: '期初余额', path: '/config/initBalance/index' }] },
  { title: '3. 日常做账', description: '录入凭证或从凭证列表导入，核对金额与附件；启用审核时由其他审核人审核后过账。', links: [{ label: '录凭证', path: '/voucher/voucher-edit' }, { label: '凭证列表 / 导入', path: '/voucher/voucher-index' }] },
  { title: '4. 月末结账', description: '按向导完成人工核对、凭证整理、计提结转与系统校验；有阻塞先去处理再重新检查。', links: [{ label: '进入月结向导', path: '/settlement/settle-period' }] },
  { title: '5. 核对与交付', description: '选择已结月份核对三表和账簿，再导出账本包。账本包用于交付，数据备份另行操作。', links: [{ label: '核对资产负债表', path: '/statement/balance-sheet' }, { label: '导出账本包', path: '/settlement/settle-list' }] },
]
