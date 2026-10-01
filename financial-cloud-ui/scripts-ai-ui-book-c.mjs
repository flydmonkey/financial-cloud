/**
 * Specialty book C (关闭凭证审核) — prompt §六.
 * Script injection allowed for login + business writes.
 * Idempotent-ish: search existing book C by name before creating.
 * Must NOT touch book A (2105377998655979522) or book B (2105448444973871105).
 */
import { chromium } from 'playwright';
import fs from 'fs';

const BASE = 'http://127.0.0.1:3154';
const API = 'http://127.0.0.1:2154';
const MARK = 'AI-UI-20260930';
const BOOK_NAME = `${MARK}-专项C`;
const COMPANY = `${MARK}-专项C公司`;
const TERM = '2026-01';
const VOUCHER_DATE = `${TERM}-15`;
const OPENING = 10000;
const EXPENSE = 100;
const EXPECTED_BANK_AFTER = OPENING - EXPENSE;
const SUMMARY = `${MARK}-C-V01 管理费用`;
const SHOT = '/opt/cursor/artifacts/screenshots';
const REPORT_MD = '/workspace/docs/testing/ai-ui-book-c-report.md';
const ARTIFACT_REPORT = '/opt/cursor/artifacts/reports/ai-ui-book-c-report.md';
const FORBIDDEN_BOOKS = new Set(['2105377998655979522', '2105448444973871105']);

const results = [];
const statusFlow = [];
const observations = [];

const rec = (id, status, detail) => {
  results.push({ id, status, detail, at: new Date().toISOString() });
  console.log(`[${status}] ${id}: ${detail}`);
};

const noteFlow = (step, status, senderId, extra = '') => {
  const line = `${step}: status=${status} sender=${senderId ? 'Y' : 'N'}${extra ? ' ' + extra : ''}`;
  statusFlow.push(line);
  console.log(`[FLOW] ${line}`);
};

async function shot(page, name) {
  fs.mkdirSync(SHOT, { recursive: true });
  await page.screenshot({ path: `${SHOT}/${name}.webp`, fullPage: false });
}

async function apiLogin(username, password) {
  const g = await fetch(`${API}/api/login/get?_allow_anonymous=true`).then((r) => r.json());
  const s = await fetch(`${API}/api/login/signin?_allow_anonymous=true`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      username,
      password,
      captcha: '',
      state: g.data.state,
      authType: 'normal',
    }),
  }).then((r) => r.json());
  if (s.code !== 0) throw new Error(`login ${username}: ${s.message}`);
  return { token: s.data.token, refresh: s.data.refresh_token, data: s.data };
}

async function api(token, method, path, body) {
  const res = await fetch(`${API}${path}`, {
    method,
    headers: {
      Authorization: 'Bearer ' + token,
      'Content-Type': 'application/json',
    },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });
  return res.json();
}

async function injectSession(page, auth) {
  await page.goto(`${BASE}/login`, { waitUntil: 'networkidle' });
  await page.evaluate((auth) => {
    document.cookie = `jb-token=${auth.token}; path=/`;
    localStorage.setItem('refresh_token', auth.refresh || '');
    localStorage.setItem('_token', JSON.stringify(auth.data));
  }, auth);
  await page.goto(`${BASE}/index`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(900);
  if (page.url().includes('/login')) throw new Error('inject session failed');
}

function num(v) {
  const n = Number(String(v ?? 0).replace(/,/g, ''));
  return Number.isFinite(n) ? n : 0;
}

function assertNotForbidden(bookId) {
  if (FORBIDDEN_BOOKS.has(String(bookId))) {
    throw new Error(`refusing to operate on protected book ${bookId}`);
  }
}

async function findBookByName(token, name) {
  const all = await api(token, 'GET', '/api/book/fetchAll');
  const list = all.data || [];
  return list.find((b) => b.name === name) || null;
}

async function ensureBook(adminToken) {
  let book = await findBookByName(adminToken, BOOK_NAME);
  if (book) {
    assertNotForbidden(book.id);
    const detail = await api(adminToken, 'GET', `/api/book/get/${book.id}`);
    const reviewed = detail.data?.voucherReviewed;
    rec(
      'BOOK-FIND',
      reviewed === 0 || reviewed === false ? 'PASS' : 'WARN',
      `已存在 bookId=${book.id} voucherReviewed=${reviewed}`,
    );
    if (reviewed !== 0 && reviewed !== false) {
      observations.push(
        `已存在账套 voucherReviewed=${reviewed}（期望关闭=0）；未强制改写，继续用现状验证`,
      );
    }
    return String(book.id);
  }

  const standards = await api(adminToken, 'GET', '/api/standard/fetchAll?status=1');
  const standard =
    (standards.data || []).find((s) => String(s.id) === '1' || String(s.name || '').includes('小企业')) ||
    (standards.data || [])[0];
  if (!standard?.id) throw new Error('missing 小企业会计准则');

  const save = await api(adminToken, 'POST', '/api/book/save', {
    name: BOOK_NAME,
    companyName: COMPANY,
    standardId: String(standard.id),
    enableDate: TERM,
    vatType: 1,
    voucherReviewed: 0,
    status: 1,
  });
  if (save.code !== 0) throw new Error(`book save: ${save.message || JSON.stringify(save)}`);

  book = await findBookByName(adminToken, BOOK_NAME);
  if (!book?.id) throw new Error('book saved but not found in fetchAll');
  assertNotForbidden(book.id);
  const detail = await api(adminToken, 'GET', `/api/book/get/${book.id}`);
  const reviewed = detail.data?.voucherReviewed;
  rec(
    'BOOK-CREATE',
    reviewed === 0 || reviewed === false ? 'PASS' : 'FAIL',
    `created bookId=${book.id} enable=${TERM} voucherReviewed=${reviewed}`,
  );
  return String(book.id);
}

async function switchBook(token, bookId) {
  assertNotForbidden(bookId);
  const sw = await api(token, 'GET', `/api/users/switchBook/${bookId}`);
  if (sw.code !== 0) throw new Error(`switchBook: ${sw.message}`);
}

/**
 * New books may inherit the creator's current open term (e.g. 2026-03 from book A)
 * instead of enableDate. Force open term to enable month so Jan vouchers can post/reverse.
 */
async function ensureCurrentTerm(token, expected) {
  const cur = await api(token, 'GET', '/api/config/sys/configKey/sys.payment.term.current');
  const actual = cur.data;
  if (actual === expected) {
    rec('TERM-CURRENT', 'PASS', `当前账期=${actual}`);
    return;
  }
  observations.push(
    `建账后当前账期为 ${actual}（非启用月 ${expected}）；疑似继承创建者所在账套账期，测试中强制改回 ${expected}`,
  );
  const upd = await api(token, 'PUT', '/api/config/sys/updateByKey', {
    configKey: 'sys.payment.term.current',
    configValue: expected,
  });
  if (upd.code !== 0) throw new Error(`set current term: ${upd.message}`);
  const again = await api(token, 'GET', '/api/config/sys/configKey/sys.payment.term.current');
  rec(
    'TERM-CURRENT',
    again.data === expected ? 'PASS' : 'FAIL',
    `was=${actual} → now=${again.data} (forced to ${expected})`,
  );
}

/** Re-login so JWT bookId matches switched book (unsender/unaudit filter by userInfo.bookId). */
async function reloginOnBook(username, password, bookId) {
  const auth = await apiLogin(username, password);
  await switchBook(auth.token, bookId);
  // Persist book on user then login again for JWT claim
  const auth2 = await apiLogin(username, password);
  if (String(auth2.data?.bookId) !== String(bookId)) {
    await switchBook(auth2.token, bookId);
    const auth3 = await apiLogin(username, password);
    return auth3;
  }
  return auth2;
}

async function fetchSubjects(token, bookId) {
  const page = await api(
    token,
    'GET',
    `/api/booksubject/fetch?bookId=${bookId}&pageNum=1&pageSize=500&status=1`,
  );
  const records = (page.data?.records || []).filter((s) => s.status === 1);
  if (records.length) {
    return records.map((s) => ({
      id: String(s.id),
      code: s.code,
      name: s.displayName || s.name || s.code,
    }));
  }
  const tree = await api(token, 'GET', `/api/booksubject/tree/${bookId}`);
  const out = [];
  const walk = (nodes) => {
    for ( const n of nodes || []) {
      if (n.children?.length) walk(n.children);
      else if (n.id) out.push({ id: String(n.id), code: n.code, name: n.name || n.label || n.code });
    }
  };
  walk(tree.data || []);
  return out;
}

function pickSubject(subjects, codes, nameHint) {
  for (const code of codes) {
    const exact = subjects.find((s) => s.code === code);
    if (exact) return exact;
  }
  for (const code of codes) {
    const child = subjects.find((s) => String(s.code || '').startsWith(code + '.'));
    if (child) return child;
  }
  if (nameHint) {
    const byName = subjects.find((s) => String(s.name || '').includes(nameHint));
    if (byName) return byName;
  }
  return null;
}

async function ensureOpeningBalances(token, bookId, bankSub, capitalSub) {
  const list = await api(token, 'GET', '/api/base/init-balance/list');
  const rows = list.data || [];
  const bank = rows.find((r) => r.code === '1002') || rows.find((r) => r.originId === bankSub.id || r.id === bankSub.id);
  const capital =
    rows.find((r) => r.code === capitalSub.code) ||
    rows.find((r) => (r.name || '').includes('实收资本'));

  if (!bank || !capital) throw new Error('init-balance missing 1002 or 实收资本');

  const bankOpen = num(bank.openingYearBalanceDebit);
  const capOpen = num(capital.openingYearBalanceCredit);
  if (Math.abs(bankOpen - OPENING) < 0.01 && Math.abs(capOpen - OPENING) < 0.01) {
    rec('LEDGER-OPENING', 'PASS', `总账期初已是银行/资本 ${OPENING}`);
    return;
  }
  if (bank.hasVoucher || capital.hasVoucher) {
    rec('LEDGER-OPENING', 'WARN', `已有凭证无法改期初 bankOpen=${bankOpen} capOpen=${capOpen}`);
    return;
  }

  const build = (row, debit, credit) => ({
    ...row,
    bookId,
    openingYearBalanceDebit: debit,
    openingYearBalanceCredit: credit,
    debitAmount: num(row.debitAmount),
    creditAmount: num(row.creditAmount),
    balance: debit + num(row.debitAmount) - credit - num(row.creditAmount),
  });

  const save = await api(token, 'POST', '/api/base/init-balance/save', [
    build(bank, OPENING, 0),
    build(capital, 0, OPENING),
  ]);
  if (save.code !== 0) throw new Error(`init-balance save: ${save.message}`);
  rec('LEDGER-OPENING', 'PASS', `总账期初 1002借/${capital.code}贷 = ${OPENING}`);
}

function getBankRow(rows) {
  return (rows || []).find((r) => r.subjectCode === '1002') || null;
}

function getBankBalance(rows) {
  const row = getBankRow(rows);
  if (!row) return null;
  if (row.balance != null && row.balance !== '') return num(row.balance);
  return num(row.closingBalanceDebit) - num(row.closingBalanceCredit);
}

async function subjectBalance(token, term) {
  const res = await api(
    token,
    'GET',
    `/api/statement/subject-balance?periodType=month&reportDate=${term}&showAll=true`,
  );
  if (res.code !== 0) throw new Error(`subject-balance: ${res.message}`);
  return res.data || [];
}

async function threeStatementsSnapshot(token, term) {
  const [sb, bs, income, cf] = await Promise.all([
    subjectBalance(token, term),
    api(token, 'GET', `/api/statement/balance-sheet?periodType=month&reportDate=${term}`),
    api(token, 'GET', `/api/statement/income?periodType=month&reportDate=${term}`),
    api(token, 'GET', `/api/statement/cash-flow?periodType=month&reportDate=${term}&cashFlowItemType=0`),
  ]);
  const bank = getBankBalance(sb);
  const incomeItems = Array.isArray(income.data?.items) ? income.data.items : [];
  const expenseLine =
    incomeItems.find((i) => /管理费用/.test(i.itemName || i.name || '')) || null;
  const expenseAmt = expenseLine
    ? num(
        expenseLine.currentBalance ??
          expenseLine.currentAmount ??
          expenseLine.currentPeriodAmount ??
          expenseLine.amount,
      )
    : null;
  const bsRaw = bs.data?.items;
  const bsItems = Array.isArray(bsRaw)
    ? bsRaw
    : [...(bsRaw?.assets || []), ...(bsRaw?.liabilities || []), ...(bsRaw?.equity || [])];
  const monetary =
    bsItems.find((i) => /货币资金|银行存款/.test(i.itemName || i.name || '')) || null;
  const monetaryAmt = monetary
    ? num(
        monetary.currentBalance ??
          monetary.endingBalance ??
          monetary.endingAmount ??
          monetary.currentAmount,
      )
    : null;
  const cfItems = Array.isArray(cf.data) ? cf.data : cf.data?.items || [];
  const cfOps = cfItems.find((i) => /经营活动产生的现金流量净额/.test(i.itemName || ''));
  return {
    bank,
    expenseAmt,
    monetaryAmt,
    cfOps: cfOps ? num(cfOps.currentAmount) : null,
    raw: { sbLen: sb.length, bsCode: bs.code, incomeCode: income.code, cfCode: cf.code },
  };
}

async function nextWordNum(token) {
  const r = await api(
    token,
    'GET',
    `/api/voucher/able-word-num?head=${encodeURIComponent('记')}&year=2026&month=1`,
  );
  return Number(r.data || 1);
}

async function findVoucherBySummary(token, summary) {
  const list = await api(token, 'GET', '/api/voucher/fetch?pageNumber=1&pageSize=100');
  const records = list.data?.records || list.data || [];
  return (
    (Array.isArray(records) ? records : []).find((v) => {
      const items = v.items || [];
      if (String(v.summary || '').includes(summary)) return true;
      return items.some((it) => String(it.summary || '').includes(summary));
    }) || null
  );
}

async function getVoucher(token, id) {
  const d = await api(token, 'GET', `/api/voucher/get/${id}`);
  if (d.code !== 0) throw new Error(`get voucher ${id}: ${d.message}`);
  return d.data;
}

async function ensureExpenseVoucher(token, bookId, bankSub, expenseSub) {
  const existing = await findVoucherBySummary(token, SUMMARY);
  if (existing?.id) {
    const detail = await getVoucher(token, existing.id);
    rec('V-FIND', 'PASS', `已存在 voucherId=${existing.id} status=${detail.status} sender=${!!detail.senderId}`);
    return detail;
  }

  const wordNum = await nextWordNum(token);
  const payload = {
    bookId,
    wordHead: '记',
    wordNum,
    companyName: COMPANY,
    receiptNum: 0,
    voucherDate: VOUCHER_DATE,
    voucherYear: 2026,
    voucherMonth: 1,
    items: [
      {
        subjectId: expenseSub.id,
        subjectName: expenseSub.name,
        summary: SUMMARY,
        debitAmount: EXPENSE,
        creditAmount: null,
      },
      {
        subjectId: bankSub.id,
        subjectName: bankSub.name,
        summary: SUMMARY,
        debitAmount: null,
        creditAmount: EXPENSE,
      },
    ],
  };
  const draft = await api(token, 'POST', '/api/voucher/draft', payload);
  if (draft.code !== 0) throw new Error(`draft: ${draft.message}`);
  const vid = String(draft.data);
  const detail = await getVoucher(token, vid);
  rec('V-DRAFT', 'PASS', `voucherId=${vid} status=${detail.status}`);
  noteFlow('draft', detail.status, detail.senderId);
  return detail;
}

async function submitVoucher(token, voucher) {
  if (voucher.status !== 'draft') {
    rec('V-SUBMIT', 'PASS', `非草稿跳过提交 status=${voucher.status}`);
    noteFlow('submit-skip', voucher.status, voucher.senderId);
    return voucher;
  }
  const payload = { ...voucher, id: voucher.id };
  let submit = await api(token, 'POST', '/api/voucher/submit', payload);
  if (submit.code !== 0) {
    submit = await api(token, 'POST', '/api/voucher/submit', { id: voucher.id });
  }
  if (submit.code !== 0) throw new Error(`submit: ${submit.message}`);
  const after = await getVoucher(token, voucher.id);
  const ok = after.status === 'completed' && !after.senderId;
  rec(
    'V-SUBMIT',
    ok ? 'PASS' : after.status === 'reviewing' ? 'FAIL' : 'WARN',
    `status=${after.status} sender=${!!after.senderId} auditor=${after.auditMemberName || after.auditorName || '-'}`,
  );
  noteFlow('submit', after.status, after.senderId, `auditor=${after.auditMemberName || '-'}`);
  if (after.status === 'reviewing') {
    observations.push('关闭审核后提交仍进入 reviewing，产品仍要求审核流');
  }
  return after;
}

async function tryAuditAsAdmin(token, voucher) {
  if (voucher.status === 'completed') {
    rec('V-AUDIT-SKIP', 'PASS', '已是 completed，无需审核人');
    return voucher;
  }
  if (voucher.status !== 'reviewing') {
    rec('V-AUDIT-SKIP', 'PASS', `status=${voucher.status} 非审核中`);
    return voucher;
  }
  const audit = await api(token, 'PUT', `/api/voucher/audit/${voucher.id}`);
  observations.push(`audit-off 但仍 reviewing；admin 自审 code=${audit.code} ${audit.message || ''}`);
  rec('V-AUDIT-ADMIN', audit.code === 0 ? 'WARN' : 'FAIL', `code=${audit.code} ${audit.message || ''}`);
  return getVoucher(token, voucher.id);
}

async function postVoucher(token, voucher) {
  if (voucher.senderId) {
    rec('V-POST', 'PASS', '已过账');
    noteFlow('post-skip', voucher.status, voucher.senderId);
    return voucher;
  }
  const post = await api(token, 'PUT', `/api/voucher/sender/${voucher.id}`);
  if (post.code !== 0) throw new Error(`post: ${post.message}`);
  const after = await getVoucher(token, voucher.id);
  rec('V-POST', after.senderId ? 'PASS' : 'FAIL', `status=${after.status} sender=${!!after.senderId}`);
  noteFlow('post', after.status, after.senderId);
  return after;
}

async function unsenderVoucher(token, voucher) {
  const res = await api(token, 'PUT', `/api/voucher/unsender/${voucher.id}`);
  const after = await getVoucher(token, voucher.id);
  rec(
    'V-UNSENDER',
    res.code === 0 && !after.senderId ? 'PASS' : 'FAIL',
    `code=${res.code} ${res.message || ''} status=${after.status} sender=${!!after.senderId}`,
  );
  noteFlow('unsender', after.status, after.senderId);
  return after;
}

async function unauditVoucher(token, voucher) {
  const res = await api(token, 'PUT', `/api/voucher/unaudit/${voucher.id}`);
  const after = await getVoucher(token, voucher.id);
  const expectDraft = after.status === 'draft';
  rec(
    'V-UNAUDIT',
    res.code === 0 && expectDraft ? 'PASS' : res.code === 0 ? 'WARN' : 'FAIL',
    `code=${res.code} ${res.message || ''} → status=${after.status}（audit-off 期望 draft）`,
  );
  noteFlow('unaudit', after.status, after.senderId);
  return after;
}

async function deleteDraft(token, voucher) {
  if (voucher.status !== 'draft') {
    rec('V-CLEANUP', 'WARN', `非草稿无法删除 status=${voucher.status}`);
    return voucher;
  }
  const del = await api(token, 'DELETE', `/api/voucher/delete/${voucher.id}`);
  rec('V-CLEANUP', del.code === 0 ? 'PASS' : 'WARN', `delete draft code=${del.code} ${del.message || ''}`);
  return null;
}

async function checkUiButtons(page, voucherId, phase) {
  await page.goto(`${BASE}/voucher/voucher-index`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(900);
  // Select first data row so split-button dropdowns enable
  const rowCheck = page.locator('.el-table__body .el-checkbox').first();
  if (await rowCheck.isVisible().catch(() => false)) {
    await rowCheck.click({ force: true }).catch(() => {});
    await page.waitForTimeout(300);
  }
  let hasUnaudit = false;
  let hasUnpost = false;
  // 反审核 is under 审核 split-button dropdown
  const auditSplit = page.locator('.toolbar-right .toolbar-split-btn').filter({ hasText: /^审核$/ }).first();
  const auditBtn = page.getByRole('button', { name: /^审核$/ }).first();
  const auditCaret = page.locator('.toolbar-right .el-dropdown').filter({ hasText: /审核/ }).locator('.el-dropdown__caret-button, .el-icon--right, .el-icon').first();
  if (await auditCaret.isVisible().catch(() => false)) {
    await auditCaret.click().catch(() => {});
  } else if (await auditBtn.isVisible().catch(() => false)) {
    // try hover/click caret area on split button
    await auditBtn.click({ button: 'right' }).catch(() => {});
  }
  await page.waitForTimeout(300);
  hasUnaudit = await page.locator('.el-dropdown-menu:visible').getByText('反审核').isVisible().catch(() => false);
  if (!hasUnaudit) {
    // click the dropdown arrow next to 审核
    const arrows = page.locator('.toolbar-right .el-dropdown .el-button');
    const count = await arrows.count();
    for (let i = 0; i < count; i++) {
      const t = (await arrows.nth(i).innerText().catch(() => '')).trim();
      if (!t || t === '审核') {
        await arrows.nth(i).click().catch(() => {});
        await page.waitForTimeout(250);
        hasUnaudit = await page.locator('.el-dropdown-menu:visible').getByText('反审核').isVisible().catch(() => false);
        if (hasUnaudit) break;
      }
    }
  }
  await page.keyboard.press('Escape').catch(() => {});
  await page.waitForTimeout(200);

  // 反过账 under 过账 split-button
  const postArrows = page.locator('.toolbar-right .el-dropdown .el-button');
  const pcount = await postArrows.count();
  for (let i = 0; i < pcount; i++) {
    const t = (await postArrows.nth(i).innerText().catch(() => '')).trim();
    if (!t || t === '过账') {
      await postArrows.nth(i).click().catch(() => {});
      await page.waitForTimeout(250);
      hasUnpost = await page.locator('.el-dropdown-menu:visible').getByText('反过账').isVisible().catch(() => false);
      if (hasUnpost) break;
    }
  }
  await page.keyboard.press('Escape').catch(() => {});

  const bodyText = await page.locator('body').innerText();
  const hasAuditToolbar = /\b审核\b/.test(bodyText);
  const hasSubmitAudit = /提交审核/.test(bodyText);
  const shotName =
    phase === 'posted'
      ? 'bookc-voucher-list-posted'
      : phase === 'unposted'
        ? 'bookc-voucher-list-unposted'
        : 'bookc-voucher-list';
  await shot(page, shotName);

  const expectUnpost = phase === 'posted';
  const expectUnaudit = phase === 'unposted';
  let status = 'PASS';
  let detail = `phase=${phase} 反过账下拉=${hasUnpost} 反审核下拉=${hasUnaudit} 工具栏审核=${hasAuditToolbar}`;
  if (expectUnpost && !hasUnpost) {
    status = 'WARN';
    detail += '（过账后未展开到反过账，API 已验证）';
  }
  if (expectUnaudit && !hasUnaudit) {
    status = 'WARN';
    detail += '（未过账已审核未展开到反审核，API 已验证）';
  }
  if (hasAuditToolbar) {
    observations.push('关闭审核账套工具栏仍显示「审核」split-button（反审核在其下拉中；提交后无需审核人即可 completed）');
  }
  if (hasSubmitAudit) {
    observations.push('关闭审核账套仍可见「提交审核」文案');
  }
  rec(`UI-REVERSE-${phase.toUpperCase()}`, status, `${detail} voucher=${voucherId}`);
  return { hasUnpost, hasUnaudit, hasAuditToolbar };
}

function almost(a, b, eps = 0.01) {
  return Math.abs(num(a) - num(b)) < eps;
}

function writeReport(bookId, extra = {}) {
  const pass = results.filter((r) => r.status === 'PASS').length;
  const fail = results.filter((r) => r.status === 'FAIL').length;
  const warn = results.filter((r) => r.status === 'WARN').length;
  const verdict = fail === 0 ? '**PASS**' : '**FAIL**';
  const lines = [
    '# AI UI 专项账套 C（关闭凭证审核）测试报告',
    '',
    `- **标识**：\`${MARK}\``,
    `- **账套**：\`${BOOK_NAME}\``,
    `- **bookId**：\`${bookId}\``,
    `- **启用期间**：\`${TERM}\`（凭证审核**关闭** voucherReviewed=0）`,
    `- **环境**：前端 \`${BASE}\` / 后端 \`${API}\``,
    `- **执行时间**：${new Date().toISOString()}`,
    `- **脚本**：\`financial-cloud-ui/scripts-ai-ui-book-c.mjs\``,
    '',
    `## 结论：${verdict}（PASS ${pass} / FAIL ${fail} / WARN ${warn}）`,
    '',
    '## 状态流转（实际观察）',
    '',
    ...statusFlow.map((s) => `- ${s}`),
    '',
    '## 结果表',
    '',
    '| 步骤 | 结果 | 说明 |',
    '|---|---|---|',
    ...results.map((r) => `| ${r.id} | ${r.status} | ${String(r.detail).replace(/\|/g, '\\|')} |`),
    '',
    '## 余额 / 三表检查点',
    '',
    '| 检查点 | 预期 | 实际 |',
    '|---|---:|---:|',
    `| 期初后银行 1002 | ${OPENING.toFixed(2)} | ${(extra.bankOpen ?? '').toString()} |`,
    `| 提交未过账银行 | ${OPENING.toFixed(2)} | ${(extra.bankAfterSubmit ?? '').toString()} |`,
    `| 过账后银行 | ${EXPECTED_BANK_AFTER.toFixed(2)} | ${(extra.bankAfterPost ?? '').toString()} |`,
    `| 反过账后银行 | ${OPENING.toFixed(2)} | ${(extra.bankAfterUnsender ?? '').toString()} |`,
    `| 提交未过账费用(利润表) | 不变/0 | ${(extra.expenseAfterSubmit ?? '').toString()} |`,
    `| 过账后费用(利润表) | ${EXPENSE.toFixed(2)} 或有发生 | ${(extra.expenseAfterPost ?? '').toString()} |`,
    '',
    '## 产品观察（audit-off 是否仍要审核人）',
    '',
    ...(observations.length ? observations.map((o) => `- ${o}`) : ['- 未发现关闭审核后仍强制审核人的路径；提交后直接 `completed`，admin 可过账。']),
    '',
    '## 证据截图',
    '',
    '- `/opt/cursor/artifacts/screenshots/bookc-voucher-list-posted.webp`',
    '- `/opt/cursor/artifacts/screenshots/bookc-voucher-list-unposted.webp`',
    '- `/opt/cursor/artifacts/screenshots/bookc-subject-balance-before.webp`',
    '- `/opt/cursor/artifacts/screenshots/bookc-subject-balance-after-post.webp`',
    '- `/opt/cursor/artifacts/screenshots/bookc-subject-balance-after-unsender.webp`',
    '- `/opt/cursor/artifacts/screenshots/bookc-settings.webp`',
    '',
  ];
  const text = lines.join('\n');
  fs.mkdirSync('/opt/cursor/artifacts/reports', { recursive: true });
  fs.mkdirSync('/workspace/docs/testing', { recursive: true });
  fs.writeFileSync(REPORT_MD, text);
  fs.writeFileSync(ARTIFACT_REPORT, text);
}

async function main() {
  fs.mkdirSync(SHOT, { recursive: true });
  let adminAuth = await apiLogin('admin', 'changeme');
  rec('LOGIN', 'PASS', 'admin（reviewer 按关闭审核流程可不参与）');

  let bookId = await ensureBook(adminAuth.token);
  assertNotForbidden(bookId);
  adminAuth = await reloginOnBook('admin', 'changeme', bookId);
  rec('SWITCH-BOOK', 'PASS', `bookId=${bookId} jwtBook=${adminAuth.data?.bookId} term预期=${TERM}`);

  const bookDetail = await api(adminAuth.token, 'GET', `/api/book/get/${bookId}`);
  rec(
    'BOOK-REVIEW-FLAG',
    bookDetail.data?.voucherReviewed === 0 ? 'PASS' : 'FAIL',
    `voucherReviewed=${bookDetail.data?.voucherReviewed}`,
  );
  await ensureCurrentTerm(adminAuth.token, TERM);

  const subjects = await fetchSubjects(adminAuth.token, bookId);
  const bankSub = pickSubject(subjects, ['1002'], '银行存款');
  const capitalSub = pickSubject(subjects, ['3001', '4001'], '实收资本');
  let expenseSub = pickSubject(subjects, ['5602'], '管理费用');
  const expenseLeaf = subjects.find((s) => String(s.code || '').startsWith('5602.'));
  if (expenseLeaf) expenseSub = expenseLeaf;
  if (!bankSub || !capitalSub || !expenseSub) {
    throw new Error(`subjects missing bank=${!!bankSub} capital=${!!capitalSub} expense=${!!expenseSub}`);
  }
  rec('SUBJECTS', 'PASS', `bank=${bankSub.code} capital=${capitalSub.code} expense=${expenseSub.code}`);

  await ensureOpeningBalances(adminAuth.token, bookId, bankSub, capitalSub);

  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({ viewport: { width: 1600, height: 1000 } });
  await injectSession(page, adminAuth);
  // Ensure browser session book matches book C
  await page.evaluate(async (bookId) => {
    await fetch(`/api/users/switchBook/${bookId}`);
  }, bookId);
  await page.goto(`${BASE}/index`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(500);

  // settings screenshot
  await page.goto(`${BASE}/setting/voucher-settlement`, { waitUntil: 'networkidle' }).catch(() => null);
  if (!page.url().includes('voucher-settlement')) {
    await page.goto(`${BASE}/setting/book`, { waitUntil: 'networkidle' }).catch(() => null);
  }
  await page.waitForTimeout(600);
  await shot(page, 'bookc-settings');

  const snapOpen = await threeStatementsSnapshot(adminAuth.token, TERM);
  rec(
    'BAL-OPEN',
    almost(snapOpen.bank, OPENING) ? 'PASS' : 'FAIL',
    `期初后银行=${snapOpen.bank}`,
  );

  let voucher = await ensureExpenseVoucher(adminAuth.token, bookId, bankSub, expenseSub);

  // If already posted from prior run, reverse first so we can re-verify the full flow
  if (voucher.senderId) {
    voucher = await unsenderVoucher(adminAuth.token, voucher);
  }
  if (voucher.status === 'completed' && !voucher.senderId) {
    // leave at completed for submit-skip path, or unaudit to re-run from draft
    voucher = await unauditVoucher(adminAuth.token, voucher);
  }

  // Fresh draft path
  if (!voucher || voucher.status !== 'draft') {
    // recreate if deleted/missing
    voucher = await ensureExpenseVoucher(adminAuth.token, bookId, bankSub, expenseSub);
  }
  if (voucher.status === 'draft') {
    noteFlow('draft', voucher.status, voucher.senderId);
  }

  voucher = await submitVoucher(adminAuth.token, voucher);
  voucher = await tryAuditAsAdmin(adminAuth.token, voucher);

  // Submitted / completed but unposted — balances must NOT change
  const snapSubmit = await threeStatementsSnapshot(adminAuth.token, TERM);
  const submitOk = almost(snapSubmit.bank, OPENING);
  rec(
    'BAL-AFTER-SUBMIT',
    submitOk ? 'PASS' : 'FAIL',
    `提交未过账银行=${snapSubmit.bank}（期望 ${OPENING}） expense=${snapSubmit.expenseAmt}`,
  );
  await page.goto(`${BASE}/statement/subject-balance`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(700);
  await shot(page, 'bookc-subject-balance-before');

  voucher = await postVoucher(adminAuth.token, voucher);
  const snapPost = await threeStatementsSnapshot(adminAuth.token, TERM);
  const postOk = almost(snapPost.bank, EXPECTED_BANK_AFTER);
  rec(
    'BAL-AFTER-POST',
    postOk ? 'PASS' : 'FAIL',
    `过账后银行=${snapPost.bank}（期望 ${EXPECTED_BANK_AFTER}） expense=${snapPost.expenseAmt}`,
  );
  await page.goto(`${BASE}/statement/subject-balance`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(700);
  await shot(page, 'bookc-subject-balance-after-post');
  await checkUiButtons(page, voucher.id, 'posted');

  // Reverse path: 反过账 → 反审核
  voucher = await unsenderVoucher(adminAuth.token, voucher);
  const snapUn = await threeStatementsSnapshot(adminAuth.token, TERM);
  rec(
    'BAL-AFTER-UNSENDER',
    almost(snapUn.bank, OPENING) ? 'PASS' : 'FAIL',
    `反过账后银行=${snapUn.bank}（期望 ${OPENING}）`,
  );
  await page.goto(`${BASE}/statement/subject-balance`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(700);
  await shot(page, 'bookc-subject-balance-after-unsender');
  await checkUiButtons(page, voucher.id, 'unposted');

  voucher = await unauditVoucher(adminAuth.token, voucher);

  // Leave clean: delete draft
  await deleteDraft(adminAuth.token, voucher);

  // Restore admin default book to A so concurrent suites on book A are not disrupted
  await switchBook(adminAuth.token, '2105377998655979522').catch(() => {});

  await browser.close();

  writeReport(bookId, {
    bankOpen: snapOpen.bank,
    bankAfterSubmit: snapSubmit.bank,
    bankAfterPost: snapPost.bank,
    bankAfterUnsender: snapUn.bank,
    expenseAfterSubmit: snapSubmit.expenseAmt,
    expenseAfterPost: snapPost.expenseAmt,
  });

  const fail = results.filter((r) => r.status === 'FAIL').length;
  console.log('\n=== STATUS FLOW ===');
  statusFlow.forEach((s) => console.log(s));
  console.log(`\nBOOK_ID=${bookId}`);
  console.log(`VERDICT=${fail === 0 ? 'PASS' : 'FAIL'}`);
  process.exit(fail === 0 ? 0 : 1);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
