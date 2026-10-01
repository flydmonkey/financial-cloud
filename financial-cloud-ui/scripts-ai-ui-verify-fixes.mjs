/**
 * Verify BUG-TZ-DATE and BUG-CF-UI-SIGN after patch ec37ae8.
 * Allowed: login + in-page/API business writes (user granted script injection).
 */
import { chromium } from 'playwright';
import fs from 'fs';

const BASE = 'http://127.0.0.1:3154';
const API = 'http://127.0.0.1:2154';
const BOOK_ID = '2105377998655979522';
const MARK = 'AI-UI-20260930';
const SHOT = '/opt/cursor/artifacts/screenshots';
const REPORT = '/opt/cursor/artifacts/reports/ai-ui-verify-fixes-report.md';

const SUB = {
  '1002': { id: '2105377999364055044', name: '银行存款' },
  '5602.01': { id: '2105377999292751877', name: '开办费' },
  '1122': { id: '2105377999255003138', name: '应收账款' },
};

const CF = { sales: '2-jy-sqxj', otherOut: '9-jy-zfqt' };

const results = [];
const rec = (id, status, detail) => {
  results.push({ id, status, detail, at: new Date().toISOString() });
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
  if (s.code !== 0) throw new Error('login ' + username + ': ' + s.message);
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
}

function item(code, debit, credit, summary) {
  const s = SUB[code];
  return {
    subjectId: s.id,
    subjectName: s.name,
    summary,
    debitAmount: debit || null,
    creditAmount: credit || null,
  };
}

async function main() {
  fs.mkdirSync(SHOT, { recursive: true });
  fs.mkdirSync('/opt/cursor/artifacts/reports', { recursive: true });

  const admin = await apiLogin('admin', 'changeme');
  const term = '2026-03';
  rec('TERM', 'INFO', `probe open term=${term}`);

  const wordNumRes = await api(
    admin.token,
    'GET',
    `/api/voucher/able-word-num?head=${encodeURIComponent('记')}&year=2026&month=3`,
  );
  let wordNum = Number(wordNumRes.data || 1);

  const mkPayload = (dateStr, summary) => ({
    bookId: BOOK_ID,
    wordHead: '记',
    wordNum: wordNum++,
    companyName: 'AI-UI-20260930-测试公司A',
    receiptNum: 0,
    voucherDate: dateStr,
    voucherYear: 2026,
    voucherMonth: 3,
    items: [
      item('5602.01', 1, null, summary),
      item('1002', null, 1, summary),
    ],
  });

  // BUG-TZ-DATE: first day of open term
  const firstDay = `${term}-01`;
  const midDay = `${term}-15`;
  const first = await api(admin.token, 'POST', '/api/voucher/draft', mkPayload(firstDay, `${MARK}-TZ-FIRST-DAY`));
  if (first.code === 0) {
    rec('BUG-TZ-DATE-FIRST', 'PASS', `draft ${firstDay} ok id=${first.data}`);
  } else {
    rec('BUG-TZ-DATE-FIRST', 'FAIL', `draft ${firstDay}: code=${first.code} ${first.message || JSON.stringify(first)}`);
  }

  const mid = await api(admin.token, 'POST', '/api/voucher/draft', mkPayload(midDay, `${MARK}-TZ-MID-DAY`));
  if (mid.code === 0) {
    rec('BUG-TZ-DATE-MID', 'PASS', `draft ${midDay} ok id=${mid.data}`);
  } else {
    rec('BUG-TZ-DATE-MID', 'FAIL', `draft ${midDay}: code=${mid.code} ${mid.message || JSON.stringify(mid)}`);
  }

  // cleanup
  for (const r of [first, mid]) {
    const id = r?.data;
    if (r.code === 0 && id) {
      const del = await api(admin.token, 'DELETE', `/api/voucher/delete/${id}`);
      rec('TZ-DEL', del.code === 0 ? 'PASS' : 'WARN', `delete ${id}: ${del.message || del.code}`);
    }
  }

  // Re-specify month-1 CF with positive amounts via API (fix storage convention)
  const pending = await api(
    admin.token,
    'GET',
    `/api/statement/cash-flow/get?pageNumber=1&pageSize=100&year=2026&month=1&cashFlowItemType=0`,
  );
  const lines = pending.data || [];
  rec('CF-PENDING', 'INFO', `month1 rows=${Array.isArray(lines) ? lines.length : JSON.stringify(pending).slice(0, 120)}`);

  // Find V02 AR credit line and set positive sales CF
  if (Array.isArray(lines) && lines.length) {
    const v02 = lines.find(
      (x) =>
        /V02|收到货款/.test(x.summary || '') &&
        !/^(1001|1002|1003)/.test(x.subjectCode || ''),
    );
    if (v02) {
      const bal = Math.abs(Number(v02.debitAmount) || Number(v02.creditAmount) || 50000);
      const spec = await api(admin.token, 'POST', '/api/statement/cash-flow/specify', {
        bookId: BOOK_ID,
        voucherDate: '2026-01',
        cashFlowItemType: 0,
        isEdit: true,
        voucherItemCashFlowDtos: [
          {
            ...v02,
            cashFlowItemCode: CF.sales,
            cashFlowBalance: bal,
            cashFlowItemType: 0,
          },
        ],
      });
      rec(
        'CF-RESPECIFY-V02',
        spec.code === 0 ? 'PASS' : 'FAIL',
        `bal=+${bal} code=${spec.code} ${spec.message || ''}`,
      );
    } else {
      rec('CF-RESPECIFY-V02', 'WARN', 'V02 non-cash line not found');
    }
  }

  // UI: open CF assign for Jan, select sales, confirm positive autofill + save
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({ viewport: { width: 1600, height: 1000 } });
  await injectSession(page, admin);

  await page.goto(`${BASE}/statement/cash-flow-statement?year=2026&month=1`, {
    waitUntil: 'networkidle',
  });
  await page.waitForTimeout(1000);

  // try select January in UI picker if present
  const monthBtn = page.getByRole('button', { name: /选择月度|2026/ }).first();
  if (await monthBtn.count()) {
    await monthBtn.click().catch(() => {});
    await page.waitForTimeout(300);
    const jan = page.locator('.el-picker-panel:visible, .el-popper:visible').getByText('1月', { exact: false }).first();
    if (await jan.count()) {
      await jan.click().catch(() => {});
      await page.waitForTimeout(800);
    } else {
      await page.keyboard.press('Escape').catch(() => {});
    }
  }

  const assignBtn = page.getByRole('button', { name: /指定现金流量/ });
  if (await assignBtn.count()) {
    await assignBtn.first().click();
    await page.waitForTimeout(1500);
    const outer = page.locator('.el-drawer.open').first();
    // filter / search V02 if input exists
    const search = outer.locator('input[placeholder*="摘要"], input[placeholder*="搜索"]').first();
    if (await search.count()) {
      await search.fill('V02');
      await page.waitForTimeout(500);
    }
    const rows = outer.locator('.el-table__body tr');
    const n = await rows.count();
    rec('CF-UI-ROWS', 'INFO', `drawer rows=${n}`);
    let target = -1;
    for (let i = 0; i < n; i++) {
      const t = await rows.nth(i).innerText();
      if (/V02|收到货款/.test(t) && /应收|1122/.test(t)) {
        target = i;
        break;
      }
    }
    if (target < 0) {
      for (let i = 0; i < n; i++) {
        const t = await rows.nth(i).innerText();
        if (/V02|收到货款/.test(t)) {
          target = i;
          break;
        }
      }
    }
    if (target >= 0) {
      const row = rows.nth(target);
      await row.getByRole('button', { name: /编辑/ }).click();
      await page.waitForTimeout(1000);
      const inner = page.locator('.el-drawer.open').last();
      await inner.locator('.el-select').first().click();
      await page.waitForTimeout(300);
      await page
        .locator('.el-select-dropdown:visible .el-select-dropdown__item')
        .filter({ hasText: /销售商品/ })
        .first()
        .click();
      await page.waitForTimeout(500);
      const amountTd = inner.locator('.el-table__body tr').first().locator('td').nth(7);
      let cell = (await amountTd.innerText()).trim();
      rec('CF-UI-AUTOFILL', /^-/.test(cell.replace(/,/g, '')) ? 'FAIL' : 'PASS', `autofill=${cell}`);

      // ensure positive
      await amountTd.locator('.editable-cell').click({ force: true }).catch(() => {});
      await page.waitForTimeout(200);
      const inp = amountTd.locator('input');
      if (await inp.count()) {
        await inp.fill('50000');
        await inp.blur();
        await page.waitForTimeout(300);
      }
      cell = (await amountTd.innerText()).trim();
      await inner.locator('.el-drawer__footer button.el-button--primary').click();
      await page.waitForTimeout(1500);
      const msgs = await page.locator('.el-message').allTextContents();
      const box = await page.locator('.el-message-box:visible').innerText().catch(() => '');
      const ok = msgs.some((m) => /成功/.test(m)) && !/不平衡/.test(msgs.join(' ') + box);
      rec('CF-UI-SAVE-POS', ok ? 'PASS' : 'FAIL', `cell=${cell} msgs=${msgs.join('|')} box=${box.slice(0, 100)}`);
      await page.screenshot({ path: `${SHOT}/verify-cf-ui-save.webp` });
    } else {
      rec('CF-UI-TARGET', 'WARN', 'no V02 row in drawer');
      await page.screenshot({ path: `${SHOT}/verify-cf-drawer-empty.webp` });
    }
    await page.keyboard.press('Escape').catch(() => {});
  } else {
    rec('CF-UI-BTN', 'WARN', 'no assign button');
  }

  // Report check: year cumulative sales should not be negative after respecify
  await page.goto(`${BASE}/statement/cash-flow-statement`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1200);
  const bodyText = await page.locator('body').innerText();
  const snip = bodyText.replace(/\s+/g, ' ');
  const m = snip.match(/销售商品、提供劳务收到的现金\s*2\s*([-\d,\.]+)\s*([-\d,\.]+)/);
  const ytd = m ? m[2] : '?';
  const ytdNum = Number(String(ytd).replace(/,/g, ''));
  rec(
    'CF-REPORT-YTD-SALES',
    Number.isFinite(ytdNum) && ytdNum >= 0 ? 'PASS' : 'FAIL',
    `month=${m?.[1]} ytd=${ytd} snip=${snip.slice(snip.indexOf('销售商品'), snip.indexOf('销售商品') + 80)}`,
  );
  await page.screenshot({ path: `${SHOT}/verify-cf-report-after.webp` });

  await browser.close();

  const md = [
    '# AI UI Fix Verification',
    '',
    `- time: ${new Date().toISOString()}`,
    `- commit: ec37ae8`,
    `- book: ${BOOK_ID}`,
    '',
    '| ID | Status | Detail |',
    '|---|---|---|',
    ...results.map((r) => `| ${r.id} | ${r.status} | ${String(r.detail).replace(/\|/g, '\\|')} |`),
    '',
  ].join('\n');
  fs.writeFileSync(REPORT, md);
  console.log('wrote', REPORT);
  process.exit(results.some((r) => r.status === 'FAIL') ? 1 : 0);
}

main().catch((e) => {
  console.error(e);
  process.exit(2);
});
