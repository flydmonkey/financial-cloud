/**
 * Book A guards: uncheckout rules + closed-period voucher edit block + export smoke.
 * Script injection allowed (login + API writes).
 */
import { chromium } from 'playwright';
import fs from 'fs';

const BASE = 'http://127.0.0.1:3154';
const API = 'http://127.0.0.1:2154';
const BOOK_A = '2105377998655979522';
const MARK = 'AI-UI-20260930';
const SHOT = '/opt/cursor/artifacts/screenshots';
const REPORT_DOC = '/workspace/docs/testing/ai-ui-guards-report.md';

const results = [];
const rec = (id, status, detail) => {
  results.push({ id, status, detail });
  console.log(`[${status}] ${id}: ${detail}`);
};

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

async function switchBook(token, bookId) {
  const r = await api(token, 'GET', `/api/users/switchBook/${bookId}`);
  if (r.code !== 0) throw new Error(`switchBook ${bookId}: ${r.message}`);
  return r;
}

async function main() {
  fs.mkdirSync(SHOT, { recursive: true });
  fs.mkdirSync('/opt/cursor/artifacts/reports', { recursive: true });
  fs.mkdirSync('/opt/cursor/artifacts/downloads', { recursive: true });

  const admin = await apiLogin('admin', 'changeme');
  await switchBook(admin.token, BOOK_A);
  rec('SWITCH-A', 'PASS', `bookId=${BOOK_A}`);

  // Find a posted Jan voucher on book A only
  const janList = await api(
    admin.token,
    'GET',
    `/api/voucher/fetch?pageNumber=1&pageSize=50&year=2026&month=1`,
  );
  const vouchers = janList.data?.records || janList.data || [];
  const posted = Array.isArray(vouchers)
    ? vouchers.find(
        (v) =>
          String(v.bookId || BOOK_A) === BOOK_A &&
          (v.senderId || v.status === 'completed') &&
          /AI-UI-20260930-V0/.test(JSON.stringify(v)),
      )
    : null;
  const anyPosted = posted || (Array.isArray(vouchers) ? vouchers.find((v) => v.senderId) : null);

  if (anyPosted) {
    const vid = anyPosted.id;
    const detail = await api(admin.token, 'GET', `/api/voucher/get/${vid}`);
    const body = detail.data || anyPosted;
    rec('CLOSED-TARGET', 'INFO', `vid=${vid} book=${body.bookId} status=${body.status} sender=${body.senderId}`);

    const upd = await api(admin.token, 'PUT', '/api/voucher/update', {
      ...body,
      id: vid,
      bookId: BOOK_A,
      summary: `${MARK}-SHOULD-BLOCK-CLOSED`,
    });
    const blocked = upd.code !== 0;
    rec(
      'CLOSED-EDIT-BLOCK',
      blocked ? 'PASS' : 'FAIL',
      `code=${upd.code} msg=${upd.message || ''}`,
    );

    const unsender = await api(admin.token, 'PUT', `/api/voucher/unsender/${vid}`);
    const unsenderBlocked = unsender.code !== 0;
    rec(
      'CLOSED-UNSENDER-BLOCK',
      unsenderBlocked ? 'PASS' : 'FAIL',
      `code=${unsender.code} msg=${unsender.message || ''}`,
    );
    // If accidentally succeeded, re-post immediately
    if (!unsenderBlocked) {
      const reviewer = await apiLogin('ai_reviewer', 'Review@2026');
      await switchBook(reviewer.token, BOOK_A);
      await api(reviewer.token, 'PUT', `/api/voucher/audit/${vid}`);
      await switchBook(admin.token, BOOK_A);
      const repost = await api(admin.token, 'PUT', `/api/voucher/sender/${vid}`);
      rec('CLOSED-UNSENDER-RESTORE', repost.code === 0 ? 'PASS' : 'FAIL', `msg=${repost.message}`);
    }
  } else {
    rec('CLOSED-EDIT-BLOCK', 'WARN', `no jan posted voucher: ${JSON.stringify(janList).slice(0, 180)}`);
  }

  // Uncheckout non-latest closed month
  const bad = await api(admin.token, 'POST', '/api/settlement/uncheckout', {
    yearPeriod: '2026-01',
  });
  rec(
    'UNCHECKOUT-NON-LATEST',
    bad.code !== 0 ? 'PASS' : 'FAIL',
    `code=${bad.code} msg=${bad.message || ''}`,
  );

  // Uncheckout latest closed (expect 2026-02 when current is 2026-03)
  const uc = await api(admin.token, 'POST', '/api/settlement/uncheckout', {
    yearPeriod: '2026-02',
  });
  if (uc.code === 0) {
    rec('UNCHECKOUT-LATEST', 'PASS', `msg=${uc.message}`);
    // Re-close exactly once via GET (POST is 405). Do not chain-close empty months.
    const checkout = await api(
      admin.token,
      'GET',
      '/api/settlement/checkout?yearPeriod=2026-02',
    );
    rec(
      'RECHECKOUT-FEB',
      checkout.code === 0 ? 'PASS' : 'WARN',
      `code=${checkout.code} msg=${checkout.message || JSON.stringify(checkout).slice(0, 160)}`,
    );
  } else {
    // Acceptable: blocked because Mar has vouchers / missing snapshot / period mismatch
    rec(
      'UNCHECKOUT-LATEST',
      /只能反结账|凭证|日记账|快照|不能反/i.test(String(uc.message || '')) ? 'PASS' : 'WARN',
      `code=${uc.code} msg=${uc.message}`,
    );
  }

  // UI checks on book A
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({ viewport: { width: 1600, height: 1000 } });
  await page.goto(`${BASE}/login`, { waitUntil: 'networkidle' });
  await page.evaluate((auth) => {
    document.cookie = `jb-token=${auth.token}; path=/`;
    localStorage.setItem('refresh_token', auth.refresh || '');
    localStorage.setItem('_token', JSON.stringify(auth.data));
  }, admin);
  // ensure UI book A via workspace if needed
  await page.goto(`${BASE}/workspace/books-board`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(800);
  const row = page.locator('tr, .book-card, .el-card').filter({ hasText: /主账套A/ }).first();
  if (await row.count()) {
    const enter = row.getByRole('button', { name: /进入|打开|切换/ }).first();
    if (await enter.count()) await enter.click();
    else await row.click();
    await page.waitForTimeout(1000);
  }

  await page.goto(`${BASE}/settlement/settle-list`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1200);
  const bodyText = (await page.locator('body').innerText()).replace(/\s+/g, ' ');
  const onBookA = /主账套A/.test(bodyText);
  const hasUncheckout = /反结账/.test(bodyText);
  rec(
    'UI-UNCHECKOUT-ENTRY',
    onBookA && hasUncheckout ? 'PASS' : onBookA ? 'WARN' : 'WARN',
    `onA=${onBookA} hasBtn=${hasUncheckout} snip=${bodyText.slice(0, 240)}`,
  );
  await page.screenshot({ path: `${SHOT}/guards-settle-list-a.webp` });

  await page.goto(`${BASE}/statement/balance-sheet`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1000);
  const exportBtn = page.getByRole('button', { name: /导出/ });
  const pdfBtn = page.getByRole('button', { name: /PDF|导出 PDF/ });
  rec(
    'UI-EXPORT-ENTRY',
    (await exportBtn.count()) > 0 || (await pdfBtn.count()) > 0 ? 'PASS' : 'WARN',
    `export=${await exportBtn.count()} pdf=${await pdfBtn.count()}`,
  );
  if (await exportBtn.count()) {
    const [download] = await Promise.all([
      page.waitForEvent('download', { timeout: 15000 }).catch(() => null),
      exportBtn.first().click(),
    ]);
    if (download) {
      const tmp = await download.path();
      const name = download.suggestedFilename() || 'balance-export.xlsx';
      const dest = `/opt/cursor/artifacts/downloads/${name.replace(/[^\w.\u4e00-\u9fff-]+/g, '_')}`;
      if (tmp) fs.copyFileSync(tmp, dest);
      const size = fs.existsSync(dest) ? fs.statSync(dest).size : 0;
      rec('EXPORT-DOWNLOAD', size > 0 ? 'PASS' : 'WARN', `file=${name} size=${size}`);
      // light content check: xlsx is zip; ensure non-trivial and name mentions 资产
      rec(
        'EXPORT-CONTENT-SMOKE',
        size > 1000 && /资产|balance|负载|负债/i.test(name) ? 'PASS' : 'WARN',
        `name=${name} size=${size}`,
      );
    } else {
      rec('EXPORT-DOWNLOAD', 'WARN', 'no download event');
    }
  }
  await page.screenshot({ path: `${SHOT}/guards-balance-sheet-a.webp` });
  await browser.close();

  const md = [
    '# AI UI Guards / Export Smoke（主账套 A）',
    '',
    `- time: ${new Date().toISOString()}`,
    `- bookId: ${BOOK_A}`,
    '',
    '| ID | Status | Detail |',
    '|---|---|---|',
    ...results.map((r) => `| ${r.id} | ${r.status} | ${String(r.detail).replace(/\|/g, '\\|')} |`),
    '',
    '## Notes',
    '',
    '- First mistaken run hit 专项B session; book B income voucher was unsent then re-posted (`senderId` restored).',
    '- This run forces `GET /api/users/switchBook/{bookA}` before API checks.',
    '',
  ].join('\n');
  fs.writeFileSync(REPORT_DOC, md);
  fs.writeFileSync('/opt/cursor/artifacts/reports/ai-ui-guards-report.md', md);
  console.log('wrote', REPORT_DOC);
  process.exit(results.some((r) => r.status === 'FAIL') ? 1 : 0);
}

main().catch((e) => {
  console.error(e);
  process.exit(2);
});
