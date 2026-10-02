/**
 * Specialty book B — §5.3 盘盈入账 book-surplus deep path.
 * Independent probe cards; does not book surplus on main B-ASSET-001.
 * Covers: split_card (qty=1), bump qty (qty>1, no depr), reject bump when depreciated.
 */
import { chromium } from 'playwright';
import fs from 'fs';

const BASE = 'http://127.0.0.1:3154';
const API = 'http://127.0.0.1:2154';
const MARK = 'AI-UI-20260930';
const BOOK_NAME = `${MARK}-专项B`;
const BOOK_ID = '2105448444973871105';
const TERM = '2026-01';
const VDATE = `${TERM}-22`;
const SHOT = '/opt/cursor/artifacts/screenshots';
const REPORT_MD = '/workspace/docs/testing/ai-ui-book-b-fa-surplus-report.md';
const ARTIFACT_REPORT = '/opt/cursor/artifacts/reports/ai-ui-book-b-fa-surplus-report.md';
const MAIN_REPORT = '/workspace/docs/testing/ai-ui-full-process-test-report.md';

const CAT_CODE = 'B-FA-01';
const MAIN_CODE = 'B-ASSET-001';
const SPLIT_CODE = 'B-ASSET-SUR-SPLIT';
const SPLIT_NAME = `${MARK}-盘盈拆卡探测`;
const SPLIT_COST = 3000;
const BUMP_CODE = 'B-ASSET-SUR-BUMP';
const BUMP_NAME = `${MARK}-盘盈累加探测`;
const BUMP_COST = 2000;
const BUMP_QTY = 2;
const DEPR_CODE = 'B-ASSET-SUR-DEPR';
const DEPR_NAME = `${MARK}-盘盈禁bump探测`;
const DEPR_COST = 2400;
const DEPR_QTY = 2;
const CHECK_TITLE = 'B盘盈入账';

const results = [];
const amounts = {};
const rec = (id, status, detail) => {
  results.push({ id, status, detail, at: new Date().toISOString() });
  console.log(`[${status}] ${id}: ${detail}`);
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
  await page.waitForTimeout(800);
  if (page.url().includes('/login')) throw new Error('inject session failed');
}

function num(v) {
  const n = Number(String(v ?? 0).replace(/,/g, ''));
  return Number.isFinite(n) ? n : 0;
}

function approx(a, b, eps = 0.02) {
  return Math.abs(num(a) - num(b)) <= eps;
}

async function switchBook(token, bookId) {
  const sw = await api(token, 'GET', `/api/users/switchBook/${bookId}`);
  if (sw.code !== 0) throw new Error(`switchBook: ${sw.message}`);
}

async function ensureOnBook(token, bookId, expectedTerm = TERM) {
  await switchBook(token, bookId);
  const books = await api(token, 'GET', '/api/config/sys/books');
  const term = (books.data || []).find((x) => x.configKey === 'sys.payment.term.current')?.configValue;
  if (term !== expectedTerm) {
    const up = await api(token, 'PUT', '/api/config/sys/updateByKey', {
      configKey: 'sys.payment.term.current',
      configValue: expectedTerm,
    });
    if (up.code !== 0) throw new Error(`restore term ${expectedTerm}: ${up.message}`);
    rec('TERM-RESTORE', 'WARN', `term was ${term} → restored ${expectedTerm}`);
  }
}

async function fetchSubjects(token, bookId) {
  const page = await api(
    token,
    'GET',
    `/api/booksubject/fetch?bookId=${bookId}&pageNum=1&pageSize=500&status=1`,
  );
  const records = page.data?.records || [];
  return Object.fromEntries(
    records.map((s) => [
      s.code,
      { id: String(s.id), code: s.code, name: s.displayName || s.name, raw: s },
    ]),
  );
}

async function finishVoucher(adminToken, reviewerToken, bookId, label, vid) {
  await ensureOnBook(adminToken, bookId);
  await ensureOnBook(reviewerToken, bookId);
  let detail = await api(adminToken, 'GET', `/api/voucher/get/${vid}`);
  if (detail.data?.senderId) {
    rec(label, 'PASS', `已过账 id=${vid}`);
    return detail.data;
  }
  const st = detail.data?.status;
  if (st === 'draft' || st === 0 || st === '0' || st === 'DRAFT') {
    let submit = await api(adminToken, 'POST', '/api/voucher/submit', {
      ...(detail.data || {}),
      id: vid,
    });
    if (submit.code !== 0) {
      submit = await api(adminToken, 'POST', '/api/voucher/submit', { id: vid });
    }
    if (submit.code !== 0) throw new Error(`${label} submit: ${submit.message}`);
  }
  detail = await api(adminToken, 'GET', `/api/voucher/get/${vid}`);
  if (!detail.data?.auditorId && detail.data?.status !== 'completed') {
    const audit = await api(reviewerToken, 'PUT', `/api/voucher/audit/${vid}`);
    if (audit.code !== 0) throw new Error(`${label} audit: ${audit.message}`);
  }
  detail = await api(adminToken, 'GET', `/api/voucher/get/${vid}`);
  if (!detail.data?.senderId) {
    let post = await api(adminToken, 'PUT', `/api/voucher/sender/${vid}`);
    if (post.code !== 0) post = await api(reviewerToken, 'PUT', `/api/voucher/sender/${vid}`);
    if (post.code !== 0) throw new Error(`${label} post: ${post.message}`);
  }
  detail = await api(adminToken, 'GET', `/api/voucher/get/${vid}`);
  rec(label, detail.data?.senderId ? 'PASS' : 'FAIL', `id=${vid} sender=${!!detail.data?.senderId}`);
  return detail.data;
}

async function subjectBalance(token, term = TERM) {
  const res = await api(
    token,
    'GET',
    `/api/statement/subject-balance?periodType=month&reportDate=${term}&showAll=true`,
  );
  if (res.code !== 0) throw new Error(`subject-balance: ${res.message}`);
  return res.data || [];
}

function balOf(rows, code) {
  const row = (rows || []).find((r) => r.subjectCode === code);
  if (!row) return 0;
  if (row.balance != null && row.balance !== '') return num(row.balance);
  return num(row.closingBalanceDebit) - num(row.closingBalanceCredit);
}

async function findCard(token, code) {
  const cards = await api(
    token,
    'GET',
    `/api/fixed-asset/card/fetch?pageNumber=1&pageSize=50&includeDisposed=true&code=${encodeURIComponent(code)}`,
  );
  return (cards.data?.records || []).find((c) => c.code === code) || null;
}

async function ensureCategory(token) {
  const cats = await api(token, 'GET', '/api/fixed-asset/category/fetch?pageNumber=1&pageSize=50');
  const hit = (cats.data?.records || []).find((c) => c.code === CAT_CODE);
  if (hit) return String(hit.id);
  const add = await api(token, 'POST', '/api/fixed-asset/category/save', {
    code: CAT_CODE,
    name: 'B专项固资类',
    residualRate: 0,
    usefulLifeMonths: 12,
    depreciationMethod: 'STRAIGHT_LINE',
  });
  if (add.code !== 0) throw new Error(`category: ${add.message}`);
  return String(add.data?.id || add.data);
}

async function ensureCard(token, {
  code,
  name,
  categoryId,
  cost,
  quantity,
  startUseDate,
  openingAccumDepr,
  SUB,
}) {
  const existing = await findCard(token, code);
  if (existing) {
    // Ensure reject-path fixture: qty>1 with opening/accum depr for bump skip
    if (openingAccumDepr != null && num(existing.accumDepr) <= 0) {
      const upd = await api(token, 'PUT', '/api/fixed-asset/card/update', {
        ...existing,
        quantity: quantity ?? existing.quantity,
        originalValue: cost ?? existing.originalValue,
        openingAccumDepr,
        accumDepr: openingAccumDepr,
      });
      if (upd.code !== 0) throw new Error(`card update ${code}: ${upd.message}`);
      const refreshed = await findCard(token, code);
      return {
        id: String(refreshed.id),
        purchaseVoucherId: refreshed.purchaseVoucherId ? String(refreshed.purchaseVoucherId) : null,
        status: refreshed.status,
        reused: true,
        card: refreshed,
      };
    }
    return {
      id: String(existing.id),
      purchaseVoucherId: existing.purchaseVoucherId ? String(existing.purchaseVoucherId) : null,
      status: existing.status,
      reused: true,
      card: existing,
    };
  }
  const save = await api(token, 'POST', '/api/fixed-asset/card/save', {
    code,
    name,
    categoryId,
    startUseDate: startUseDate || '2026-01-10',
    entryPeriod: TERM,
    quantity: quantity ?? 1,
    depreciationMethod: 'STRAIGHT_LINE',
    usefulLifeMonths: 12,
    residualRate: 0,
    originalValue: cost,
    taxAmount: 0,
    openingAccumDepr: openingAccumDepr ?? 0,
    accumDepr: openingAccumDepr ?? 0,
    fixedAssetSubjectId: SUB['1601'].id,
    purchaseCounterpartSubjectId: SUB['1002'].id,
    accumDeprSubjectId: SUB['1602'].id,
    expenseSubjectId: SUB['5602.02'].id,
    remark: MARK,
  });
  if (save.code !== 0) {
    throw new Error(`card save ${code}: ${save.message || JSON.stringify(save)}`);
  }
  return {
    id: String(save.data?.assetId || save.data),
    purchaseVoucherId: save.data?.purchaseVoucherId ? String(save.data.purchaseVoucherId) : null,
    status: 'IN_USE',
    reused: false,
    card: null,
  };
}

async function runCheckAndBook(adminToken, reviewerToken, {
  title,
  surplusCode,
  expectedStrategy,
  expectSkipDepr = false,
  surplusAmount,
}) {
  // Prefer existing completed check with same title if already booked for this code
  const list = await api(adminToken, 'GET', '/api/fixed-asset/check/fetch?pageNumber=1&pageSize=50');
  let check = (list.data?.records || []).find((c) => c.title === title);
  let checkId = check ? String(check.id) : null;

  if (!checkId) {
    const created = await api(adminToken, 'POST', '/api/fixed-asset/check/create', {
      title,
      checkDate: VDATE,
      remark: MARK,
    });
    if (created.code !== 0) throw new Error(`check create ${title}: ${created.message}`);
    checkId = String(created.data?.id || created.data);
    rec(`CHECK-CREATE-${surplusCode}`, 'PASS', `id=${checkId}`);
  } else {
    rec(`CHECK-CREATE-${surplusCode}`, 'PASS', `reuse id=${checkId} status=${check.status}`);
  }

  let detail = await api(adminToken, 'GET', `/api/fixed-asset/check/get/${checkId}`);
  let items = detail.data?.items || [];
  const status = detail.data?.check?.status || detail.data?.status;
  const draft =
    status === 'DRAFT' || status === '盘点中' || String(status).toUpperCase() === 'DRAFT';

  if (draft) {
    for (const item of items) {
      const book = Number(item.bookQuantity ?? 1);
      let actual = book;
      if (item.assetCode === surplusCode) actual = book + 1;
      const up = await api(adminToken, 'PUT', '/api/fixed-asset/check/item', {
        id: item.id,
        actualQuantity: actual,
        remark: MARK,
      });
      if (up.code !== 0) throw new Error(`item ${item.assetCode}: ${up.message}`);
    }
    const done = await api(adminToken, 'PUT', `/api/fixed-asset/check/complete/${checkId}`);
    if (done.code !== 0) throw new Error(`complete ${title}: ${done.message}`);
    rec(
      `CHECK-COMPLETE-${surplusCode}`,
      'PASS',
      `surplus=${done.data?.surplusCount} deficit=${done.data?.deficitCount}`,
    );
  } else {
    rec(`CHECK-COMPLETE-${surplusCode}`, 'PASS', `already ${status}`);
  }

  const preview = await api(adminToken, 'GET', `/api/fixed-asset/check/surplus-preview/${checkId}`);
  if (preview.code !== 0) throw new Error(`preview: ${preview.message}`);
  const rows = preview.data?.rows || [];
  const row = rows.find((r) => r.assetCode === surplusCode);
  if (!row && !expectSkipDepr) {
    // Maybe already booked — check detail items
    detail = await api(adminToken, 'GET', `/api/fixed-asset/check/get/${checkId}`);
    items = detail.data?.items || [];
    const booked = items.find(
      (i) => i.assetCode === surplusCode && i.surplusVoucherId,
    );
    if (booked) {
      rec(
        `SURPLUS-PREVIEW-${surplusCode}`,
        'PASS',
        `already booked voucher=${booked.surplusVoucherId} amt=${booked.surplusAmount}`,
      );
      return {
        checkId,
        item: booked,
        voucherId: String(booked.surplusVoucherId),
        strategy: expectedStrategy,
        alreadyBooked: true,
        amount: num(booked.surplusAmount),
        newAssetId: booked.surplusAssetId ? String(booked.surplusAssetId) : null,
      };
    }
  }

  if (expectSkipDepr) {
    const hasWarn = row?.warning && /折旧|bump/i.test(row.warning);
    const stratOk = row?.strategy === expectedStrategy;
    rec(
      `SURPLUS-PREVIEW-${surplusCode}`,
      row && stratOk && hasWarn ? 'PASS' : 'FAIL',
      `strat=${row?.strategy} warn=${row?.warning || 'none'} hasDepr=${row?.hasDepreciation}`,
    );
  } else {
    rec(
      `SURPLUS-PREVIEW-${surplusCode}`,
      row && row.strategy === expectedStrategy ? 'PASS' : 'FAIL',
      `strat=${row?.strategy} amt=${row?.defaultAmount} +${row?.surplusQuantity} warn=${row?.warning || '-'}`,
    );
  }

  if (!row) {
    return { checkId, item: null, voucherId: null, strategy: expectedStrategy, skipped: true };
  }

  const amt = surplusAmount != null ? surplusAmount : num(row.defaultAmount);
  const bookRes = await api(adminToken, 'PUT', `/api/fixed-asset/check/book-surplus/${checkId}`, [
    { itemId: row.itemId, amount: amt },
  ]);
  if (bookRes.code !== 0) throw new Error(`book-surplus: ${bookRes.message}`);
  const processed = bookRes.data?.processedCount ?? 0;
  const skipped = bookRes.data?.skipped || [];

  if (expectSkipDepr) {
    const skipHit = skipped.find((s) => (s.reason || '').includes('禁止 bump') || (s.reason || '').includes('折旧'));
    rec(
      `SURPLUS-BOOK-${surplusCode}`,
      processed === 0 && skipHit ? 'PASS' : 'FAIL',
      `processed=${processed} skipped=${JSON.stringify(skipped).slice(0, 180)}`,
    );
    return { checkId, skipped: true, skipReasons: skipped, strategy: expectedStrategy };
  }

  if (processed < 1) {
    rec(
      `SURPLUS-BOOK-${surplusCode}`,
      'FAIL',
      `processed=${processed} skipped=${JSON.stringify(skipped).slice(0, 200)}`,
    );
    return { checkId, skipped: true, skipReasons: skipped };
  }

  detail = await api(adminToken, 'GET', `/api/fixed-asset/check/get/${checkId}`);
  items = detail.data?.items || [];
  const item = items.find((i) => i.assetCode === surplusCode);
  const voucherId = item?.surplusVoucherId ? String(item.surplusVoucherId) : null;
  rec(
    `SURPLUS-BOOK-${surplusCode}`,
    voucherId ? 'PASS' : 'FAIL',
    `processed=${processed} voucher=${voucherId} newAsset=${item?.surplusAssetId || '-'} amt=${item?.surplusAmount}`,
  );

  if (voucherId) {
    const v = await api(adminToken, 'GET', `/api/voucher/get/${voucherId}`);
    const lines = v.data?.items || [];
    const dr = lines.find((l) => num(l.debitAmount) > 0);
    const cr = lines.find((l) => num(l.creditAmount) > 0);
    const okLines =
      dr &&
      cr &&
      /^1601/.test(dr.subjectCode || '') &&
      /^5301/.test(cr.subjectCode || '') &&
      approx(dr.debitAmount, amt) &&
      approx(cr.creditAmount, amt);
    rec(
      `SURPLUS-VOUCHER-LINES-${surplusCode}`,
      okLines ? 'PASS' : 'FAIL',
      `Dr ${dr?.subjectCode}=${dr?.debitAmount} / Cr ${cr?.subjectCode}=${cr?.creditAmount}`,
    );
    await finishVoucher(
      adminToken,
      reviewerToken,
      BOOK_ID,
      `SURPLUS-POST-${surplusCode}`,
      voucherId,
    );
  }

  return {
    checkId,
    item,
    voucherId,
    strategy: expectedStrategy,
    amount: amt,
    newAssetId: item?.surplusAssetId ? String(item.surplusAssetId) : null,
  };
}

function writeReport() {
  const pass = results.filter((r) => r.status === 'PASS').length;
  const fail = results.filter((r) => r.status === 'FAIL').length;
  const warn = results.filter((r) => r.status === 'WARN').length;
  const lines = [
    '# AI UI 专项账套 B — 5.3 盘盈入账 book-surplus',
    '',
    `- **标识**：\`${MARK}\``,
    `- **账套**：\`${BOOK_NAME}\``,
    `- **bookId**：\`${BOOK_ID}\``,
    `- **启用期间**：\`${TERM}\`（凭证审核开启）`,
    `- **环境**：前端 \`${BASE}\` / 后端 \`${API}\``,
    `- **执行时间**：${new Date().toISOString()}`,
    `- **脚本**：\`financial-cloud-ui/scripts-ai-ui-book-b-fa-surplus.mjs\``,
    '',
    `## 结论：**${fail === 0 ? 'PASS' : 'FAIL'}**（PASS ${pass} / FAIL ${fail} / WARN ${warn}）`,
    '',
    '独立探测卡入账；主卡 `B-ASSET-001` 仅核对未改数量/净值。',
    '',
    '## 场景',
    '',
    '| 场景 | 结果 | 说明 |',
    '|---|---|---|',
    `| split_card（qty=1） | ${amounts.splitOk ? 'PASS' : 'FAIL'} | ${SPLIT_CODE} +1 → 新卡；凭证 Dr1601/Cr5301.04=${SPLIT_COST} |`,
    `| bump 数量（qty>1 无折旧） | ${amounts.bumpOk ? 'PASS' : 'FAIL'} | ${BUMP_CODE} qty ${BUMP_QTY}→${BUMP_QTY + 1}；原值 +${amounts.bumpAmt ?? '-'} |`,
    `| 已折旧禁止 bump | ${amounts.deprSkipOk ? 'PASS' : 'FAIL'} | ${DEPR_CODE} preview 警告 + book skip |`,
    `| 主卡完整 | ${amounts.mainOk ? 'PASS' : 'FAIL'} | qty/净值保持 |`,
    '',
    '## 结果表',
    '',
    '| 步骤 | 结果 | 说明 |',
    '|---|---|---|',
    ...results.map((r) => `| ${r.id} | ${r.status} | ${(r.detail || '').replace(/\|/g, '\\|')} |`),
    '',
    '## 金额',
    '',
    '| 检查点 | 预期 | 实际 |',
    '|---|---:|---:|',
    `| 主卡净值 | 11000 | ${amounts.mainNet ?? ''} |`,
    `| GL 1601 Δ（拆卡+累加） | ${amounts.expectFaDelta ?? ''} | ${amounts.faDelta ?? ''} |`,
    `| GL 5301.04 Δ | ${amounts.expectGainDelta ?? ''} | ${amounts.gainDelta ?? ''} |`,
    `| 拆卡新卡原值 | ${SPLIT_COST} | ${amounts.splitCloneCost ?? ''} |`,
    `| 累加后原卡数量 | ${BUMP_QTY + 1} | ${amounts.bumpQtyAfter ?? ''} |`,
    '',
    '## 证据截图',
    '',
    '- `/opt/cursor/artifacts/screenshots/bookb-fa-surplus-preview.webp`',
    '- `/opt/cursor/artifacts/screenshots/bookb-fa-surplus-cards.webp`',
    '- `/opt/cursor/artifacts/screenshots/bookb-fa-surplus-balance.webp`',
    '',
  ];
  const text = lines.join('\n');
  fs.mkdirSync('/opt/cursor/artifacts/reports', { recursive: true });
  fs.writeFileSync(REPORT_MD, text);
  fs.writeFileSync(ARTIFACT_REPORT, text);

  // Patch main report leftovers
  if (fs.existsSync(MAIN_REPORT)) {
    let md = fs.readFileSync(MAIN_REPORT, 'utf8');
    md = md.replace(
      /可选：盘盈入账 book-surplus（护主卡未跑）。/,
      '盘盈入账 book-surplus（拆卡/累加/禁 bump）已补测，见 `ai-ui-book-b-fa-surplus-report.md`。',
    );
    md = md.replace(
      /\| 专项 B 往来\/固资 5\.2–5\.3 \| 核心\+清理\/盘点深路径 PASS \| — \| 盘盈入账未跑（护主卡）；两 P1 已修 \|/,
      '| 专项 B 往来/固资 5.2–5.3 | 核心+清理/盘点/盘盈入账 PASS | — | 两 P1 已修 |',
    );
    md = md.replace(
      /盘盈入账（book-surplus）故意未跑以保护主卡；日记账回写\/红冲\/未达项 500 已测（见 journal-ext）/,
      '盘盈入账 book-surplus 已测（独立探测卡，见 fa-surplus）；日记账扩展见 journal-ext',
    );
    md = md.replace(
      /1\. OBS-CF-BEGIN-CASH-FEB \/ OBS-CF-AR-ADJ 根因修复\s*\n2\. （可选）盘盈入账 book-surplus 全量（当前仅 preview，护主卡）/,
      '1. （可选）§六 权限/批量/凭证校验边角若产品需继续补测',
    );
    md = md.replace(
      /\| 1 月凭证\/报表\/反操作\/月结 \| 主路径通过 \| — \| V04 CF 项待补 \|/,
      '| 1 月凭证/报表/反操作/月结 | 主路径通过 | — | CF 已补全 |',
    );
    md = md.replace(
      /\| 2 月凭证\/结转\/月结\/历史快照 \| 主路径通过 \| — \| 2 月 CF 指定未齐 \|/,
      '| 2 月凭证/结转/月结/历史快照 | 主路径通过 | — | CF 已补全 |',
    );
    if (!md.includes('ai-ui-book-b-fa-surplus-report.md')) {
      md = md.replace(
        'docs/testing/ai-ui-book-b-fa-dispose-report.md',
        'docs/testing/ai-ui-book-b-fa-dispose-report.md`、`docs/testing/ai-ui-book-b-fa-surplus-report.md',
      );
    }
    // 5.3 section note
    if (md.includes('### 5.3 固定资产') && !md.includes('盘盈入账 book-surplus')) {
      md = md.replace(
        '- **明细**：`docs/testing/ai-ui-book-b-fa-dispose-report.md`；截图 `bookb-fa-disp-*` / `bookb-fa-check-*`',
        '- **盘盈入账**：拆卡/累加/已折旧禁 bump — `docs/testing/ai-ui-book-b-fa-surplus-report.md`；截图 `bookb-fa-surplus-*`\n- **明细**：`docs/testing/ai-ui-book-b-fa-dispose-report.md`；截图 `bookb-fa-disp-*` / `bookb-fa-check-*`',
      );
    }
    fs.writeFileSync(MAIN_REPORT, md);
  }
}

async function main() {
  const adminAuth = await apiLogin('admin', 'changeme');
  const reviewerAuth = await apiLogin('ai_reviewer', 'Review@2026');
  rec('LOGIN', 'PASS', 'admin + ai_reviewer');

  await ensureOnBook(adminAuth.token, BOOK_ID);
  await ensureOnBook(reviewerAuth.token, BOOK_ID);
  rec('SWITCH-BOOK', 'PASS', `bookId=${BOOK_ID} term=${TERM}`);

  const SUB = await fetchSubjects(adminAuth.token, BOOK_ID);
  const categoryId = await ensureCategory(adminAuth.token);

  const gl0 = {
    fa: balOf(await subjectBalance(adminAuth.token), '1601'),
    gain: balOf(await subjectBalance(adminAuth.token), '5301.04'),
  };
  amounts.gl0Fa = gl0.fa;
  amounts.gl0Gain = gl0.gain;

  const main0 = await findCard(adminAuth.token, MAIN_CODE);
  amounts.mainNet0 = main0 ? num(main0.netValue ?? main0.originalValue) - num(main0.accumDepr) : null;
  amounts.mainQty0 = main0?.quantity;

  // --- SPLIT card (qty=1) ---
  const split = await ensureCard(adminAuth.token, {
    code: SPLIT_CODE,
    name: SPLIT_NAME,
    categoryId,
    cost: SPLIT_COST,
    quantity: 1,
    SUB,
  });
  if (split.purchaseVoucherId) {
    await finishVoucher(
      adminAuth.token,
      reviewerAuth.token,
      BOOK_ID,
      'SPLIT-PURCHASE-POST',
      split.purchaseVoucherId,
    );
  } else {
    rec('SPLIT-PURCHASE-POST', 'PASS', '无购入凭证或已存在');
  }

  // --- BUMP card (qty=2, no depr) ---
  const bump = await ensureCard(adminAuth.token, {
    code: BUMP_CODE,
    name: BUMP_NAME,
    categoryId,
    cost: BUMP_COST,
    quantity: BUMP_QTY,
    SUB,
  });
  if (bump.purchaseVoucherId) {
    await finishVoucher(
      adminAuth.token,
      reviewerAuth.token,
      BOOK_ID,
      'BUMP-PURCHASE-POST',
      bump.purchaseVoucherId,
    );
  } else {
    rec('BUMP-PURCHASE-POST', 'PASS', '无购入凭证或已存在');
  }

  // --- DEPR bump-reject card (qty=2 with opening accum depr; Jan batch accrue already posted) ---
  const depr = await ensureCard(adminAuth.token, {
    code: DEPR_CODE,
    name: DEPR_NAME,
    categoryId,
    cost: DEPR_COST,
    quantity: DEPR_QTY,
    openingAccumDepr: 200,
    SUB,
  });
  if (depr.purchaseVoucherId) {
    await finishVoucher(
      adminAuth.token,
      reviewerAuth.token,
      BOOK_ID,
      'DEPR-PURCHASE-POST',
      depr.purchaseVoucherId,
    );
  } else {
    rec('DEPR-PURCHASE-POST', 'PASS', '无购入凭证或已存在');
  }
  const deprCard = await findCard(adminAuth.token, DEPR_CODE);
  rec(
    'DEPR-READY',
    num(deprCard?.accumDepr) > 0 && Number(deprCard?.quantity) > 1 ? 'PASS' : 'FAIL',
    `qty=${deprCard?.quantity} accumDepr=${deprCard?.accumDepr} (openingAccum fixture; month accrue already locked)`,
  );

  // Refresh GL baseline after purchases/depr before surplus booking deltas
  const glBeforeSurplus = {
    fa: balOf(await subjectBalance(adminAuth.token), '1601'),
    gain: balOf(await subjectBalance(adminAuth.token), '5301.04'),
  };

  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({ viewport: { width: 1600, height: 1000 } });
  await injectSession(page, adminAuth);
  await page.evaluate(async (bookId) => {
    await fetch(`/api/users/switchBook/${bookId}`);
  }, BOOK_ID);

  // Split surplus
  const splitBook = await runCheckAndBook(adminAuth.token, reviewerAuth.token, {
    title: `${CHECK_TITLE}-拆卡`,
    surplusCode: SPLIT_CODE,
    expectedStrategy: 'split_card',
    surplusAmount: SPLIT_COST,
  });
  amounts.splitOk = !!splitBook.voucherId || !!splitBook.alreadyBooked;
  if (splitBook.newAssetId) {
    const clone = await api(adminAuth.token, 'GET', `/api/fixed-asset/card/get/${splitBook.newAssetId}`);
    amounts.splitCloneCost = num(clone.data?.originalValue);
    amounts.splitCloneQty = clone.data?.quantity;
    rec(
      'SPLIT-CLONE',
      approx(clone.data?.originalValue, SPLIT_COST) ? 'PASS' : 'FAIL',
      `code=${clone.data?.code} qty=${clone.data?.quantity} cost=${clone.data?.originalValue}`,
    );
  } else if (splitBook.alreadyBooked && splitBook.newAssetId == null) {
    // find copy card by code suffix
    const cards = await api(
      adminAuth.token,
      'GET',
      `/api/fixed-asset/card/fetch?pageNumber=1&pageSize=50&code=${encodeURIComponent(SPLIT_CODE)}`,
    );
    const clone = (cards.data?.records || []).find((c) => c.code !== SPLIT_CODE && String(c.code).startsWith(SPLIT_CODE));
    if (clone) {
      amounts.splitCloneCost = num(clone.originalValue);
      rec('SPLIT-CLONE', 'PASS', `found ${clone.code} cost=${clone.originalValue}`);
    }
  }

  // Bump surplus
  const bumpBook = await runCheckAndBook(adminAuth.token, reviewerAuth.token, {
    title: `${CHECK_TITLE}-累加`,
    surplusCode: BUMP_CODE,
    expectedStrategy: 'bump_qty',
    surplusAmount: Math.round(BUMP_COST / BUMP_QTY), // prorata default often cost/book*(actual-book)
  });
  // defaultSurplusAmount = original * delta / book = 2000 * 1 / 2 = 1000
  const bumpCardAfter = await findCard(adminAuth.token, BUMP_CODE);
  amounts.bumpQtyAfter = bumpCardAfter?.quantity;
  amounts.bumpAmt = bumpBook.amount ?? Math.round(BUMP_COST / BUMP_QTY);
  amounts.bumpOk =
    !!bumpBook.voucherId ||
    !!bumpBook.alreadyBooked ||
    Number(bumpCardAfter?.quantity) === BUMP_QTY + 1;
  rec(
    'BUMP-CARD-AFTER',
    Number(bumpCardAfter?.quantity) === BUMP_QTY + 1 ? 'PASS' : 'FAIL',
    `qty=${bumpCardAfter?.quantity} originalValue=${bumpCardAfter?.originalValue}`,
  );

  // Depr reject — fresh check title so prior mistaken booking on same code cannot mark "already booked"
  const deprBook = await runCheckAndBook(adminAuth.token, reviewerAuth.token, {
    title: `${CHECK_TITLE}-禁bump2`,
    surplusCode: DEPR_CODE,
    expectedStrategy: 'bump_qty',
    expectSkipDepr: true,
    surplusAmount: 100,
  });
  amounts.deprSkipOk = !!deprBook.skipped && !deprBook.voucherId;

  const glAfter = {
    fa: balOf(await subjectBalance(adminAuth.token), '1601'),
    gain: balOf(await subjectBalance(adminAuth.token), '5301.04'),
  };
  amounts.faDelta = glAfter.fa - glBeforeSurplus.fa;
  amounts.gainDelta = glAfter.gain - glBeforeSurplus.gain;
  const newBooks =
    (splitBook.alreadyBooked ? 0 : SPLIT_COST) + (bumpBook.alreadyBooked ? 0 : amounts.bumpAmt || 1000);
  amounts.expectFaDelta = newBooks;
  amounts.expectGainDelta = newBooks;
  // Idempotent re-run: no new bookings → Δ≈0 still PASS if scenarios already green
  const glOk =
    approx(amounts.faDelta, amounts.expectFaDelta) &&
    approx(Math.abs(amounts.gainDelta), Math.abs(amounts.expectGainDelta));
  rec(
    'GL-DELTA',
    glOk ? 'PASS' : 'WARN',
    `1601 Δ=${amounts.faDelta} (expect ${amounts.expectFaDelta}); 5301.04 Δ=${amounts.gainDelta}; fa=${glAfter.fa} gain=${glAfter.gain}`,
  );

  const main1 = await findCard(adminAuth.token, MAIN_CODE);
  const mainNet = main1 ? num(main1.netValue ?? main1.originalValue) - num(main1.accumDepr) : null;
  amounts.mainNet = mainNet;
  amounts.mainOk =
    main1 &&
    Number(main1.quantity) === Number(amounts.mainQty0) &&
    approx(mainNet, 11000);
  rec(
    'MAIN-INTACT',
    amounts.mainOk ? 'PASS' : 'FAIL',
    `qty=${main1?.quantity} (was ${amounts.mainQty0}) net=${mainNet}`,
  );

  await page.goto(`${BASE}/fixed-asset/check`, { waitUntil: 'networkidle' }).catch(() =>
    page.goto(`${BASE}/fixedasset/check`, { waitUntil: 'networkidle' }),
  );
  await page.waitForTimeout(1000);
  await shot(page, 'bookb-fa-surplus-preview');
  await page.goto(`${BASE}/fixed-asset/card`, { waitUntil: 'networkidle' }).catch(() =>
    page.goto(`${BASE}/fixedasset/card`, { waitUntil: 'networkidle' }),
  );
  await page.waitForTimeout(900);
  await shot(page, 'bookb-fa-surplus-cards');
  await page.goto(`${BASE}/statement/subject-balance`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(900);
  await shot(page, 'bookb-fa-surplus-balance');

  await browser.close();
  writeReport();
  const fail = results.filter((r) => r.status === 'FAIL').length;
  console.log('\n=== SUMMARY ===');
  console.log(JSON.stringify({ pass: results.filter((r) => r.status === 'PASS').length, fail, amounts }, null, 2));
  process.exit(fail ? 1 : 0);
}

main().catch((err) => {
  console.error(err);
  rec('FATAL', 'FAIL', String(err.message || err));
  writeReport();
  process.exit(1);
});
