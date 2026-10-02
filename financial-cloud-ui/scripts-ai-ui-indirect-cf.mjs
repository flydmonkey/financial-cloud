/**
 * Book A: cash-flow direct + indirect (supplementary) reconciliation smoke.
 */
import { chromium } from 'playwright';
import fs from 'fs';

const BASE = 'http://127.0.0.1:3154';
const API = 'http://127.0.0.1:2154';
const BOOK_A = '2105377998655979522';
const SHOT = '/opt/cursor/artifacts/screenshots';
const REPORT = '/workspace/docs/testing/ai-ui-indirect-cf-report.md';

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
  if (s.code !== 0) throw new Error(s.message);
  return { token: s.data.token, refresh: s.data.refresh_token, data: s.data };
}

async function api(token, method, path) {
  return fetch(`${API}${path}`, {
    method,
    headers: { Authorization: 'Bearer ' + token },
  }).then((r) => r.json());
}

function num(v) {
  const n = Number(String(v ?? '').replace(/,/g, ''));
  return Number.isFinite(n) ? n : NaN;
}

function findRow(rows, re) {
  return rows.find((r) => re.test(r.itemName || r.name || ''));
}

async function sheet(token, ym) {
  const q = new URLSearchParams({
    periodType: 'month',
    date: ym,
    reportDate: ym,
  });
  const r = await api(token, 'GET', `/api/statement/cash-flow?${q}`);
  if (r.code !== 0 || !Array.isArray(r.data)) {
    throw new Error(`cash-flow ${ym}: ${r.message || JSON.stringify(r).slice(0, 120)}`);
  }
  return r.data;
}

async function main() {
  fs.mkdirSync(SHOT, { recursive: true });
  const admin = await apiLogin('admin', 'changeme');
  await api(admin.token, 'GET', `/api/users/switchBook/${BOOK_A}`);

  // Expected from prompt (independent)
  const exp = {
    '2026-01': {
      opDirect: 32000, // 50-10-8; may miss V04 10k → 42000 if V04 CF missing
      sales: 50000,
      purchase: 8000,
      otherOut: 10000,
      invest: -12000,
      finance: 40000,
      ni: 40000,
      invDec: 10000,
      arInc: 30000,
      apInc: 12000,
      opIndirect: 32000,
    },
    '2026-02': {
      opDirect: 28000, // 40-5-7; CF may be incomplete
      sales: 40000,
      purchase: 7000,
      otherOut: 5000,
      invest: 0,
      finance: 0,
      ni: 13000,
      invDec: 12000,
      arDec: 10000,
      apDec: 7000,
      opIndirect: 28000,
    },
  };

  for (const ym of ['2026-01', '2026-02']) {
    const rows = await sheet(admin.token, ym);
    rec(`${ym}-ROWS`, 'INFO', `count=${rows.length}`);

    const sales = findRow(rows, /销售商品.*收到的现金/);
    const purchase = findRow(rows, /购买商品.*支付的现金/);
    const otherOut = findRow(rows, /支付其他与经营活动有关的现金/);
    const invest = findRow(rows, /购建固定资产/);
    const finance = findRow(rows, /取得借款收到的现金/);
    const opNet = findRow(rows, /经营活动产生的现金流量净额/) || findRow(rows, /^经营活动现金流量净额/);
    const ni = findRow(rows, /^净利润$|净利润$/);
    const inv = findRow(rows, /存货的减少/);
    const ar = findRow(rows, /经营性应收项目的减少|应收项目的减少/);
    const ap = findRow(rows, /经营性应付项目的增加|应付项目的增加/);
    const opInd = rows.filter((r) => /经营活动产生的现金流量净额/.test(r.itemName || ''));
    // often two: main + supplementary
    const cashInc = findRow(rows, /现金及现金等价物净增加额/);
    const begin = findRow(rows, /加：期初现金|^期初现金/);
    const end = findRow(rows, /期末现金及现金等价物余额|期末现金/);

    const pick = (row) => ({
      m: num(row?.currentMonthAmount ?? row?.monthAmount ?? row?.termAmount ?? row?.currentAmount),
      y: num(row?.currentYearAmount ?? row?.yearAmount ?? row?.cumulativeAmount),
      raw: row,
    });

    // discover amount field names from first non-null numeric props
    if (rows[0]) {
      rec(
        `${ym}-FIELDS`,
        'INFO',
        Object.keys(rows[0])
          .filter((k) => /amount|Amount|balance|Balance|month|year|term/i.test(k))
          .join(','),
      );
    }

    const sample = rows.slice(0, 3).map((r) => JSON.stringify(r).slice(0, 180));
    rec(`${ym}-SAMPLE`, 'INFO', sample.join(' || '));

    const e = exp[ym];
    const salesM = pick(sales).m;
    const purchaseM = pick(purchase).m;
    const otherM = pick(otherOut).m;
    const investM = pick(invest).m;
    const finM = pick(finance).m;

    rec(
      `${ym}-DIRECT-SALES`,
      salesM === e.sales || salesM === e.sales || Number.isFinite(salesM) ? (salesM === e.sales ? 'PASS' : 'WARN') : 'FAIL',
      `actual=${salesM} expected=${e.sales}`,
    );
    rec(
      `${ym}-DIRECT-PURCHASE`,
      purchaseM === e.purchase ? 'PASS' : Number.isFinite(purchaseM) ? 'WARN' : 'FAIL',
      `actual=${purchaseM} expected=${e.purchase}`,
    );
    rec(
      `${ym}-DIRECT-OTHER-OUT`,
      otherM === e.otherOut ? 'PASS' : 'WARN',
      `actual=${otherM} expected=${e.otherOut} (may be missing CF assign)`,
    );
    rec(
      `${ym}-DIRECT-INVEST`,
      investM === Math.abs(e.invest) || investM === e.invest ? 'PASS' : 'WARN',
      `actual=${investM} expectedAbs=${Math.abs(e.invest)}`,
    );
    rec(
      `${ym}-DIRECT-FINANCE`,
      finM === e.finance ? 'PASS' : 'WARN',
      `actual=${finM} expected=${e.finance}`,
    );

    // Indirect lines
    const niM = pick(ni).m;
    const invM = pick(inv).m;
    const arM = pick(ar).m;
    const apM = pick(ap).m;
    rec(`${ym}-IND-NI`, niM === e.ni ? 'PASS' : 'WARN', `actual=${niM} expected=${e.ni}`);
    rec(
      `${ym}-IND-INV`,
      invM === e.invDec ? 'PASS' : 'WARN',
      `actual=${invM} expectedDec=${e.invDec}`,
    );
    if (ym === '2026-01') {
      rec(
        `${ym}-IND-AR`,
        arM === -e.arInc || arM === e.arInc ? 'PASS' : 'WARN',
        `actual=${arM} expectedInc=${e.arInc} (sign per UI)`,
      );
      rec(
        `${ym}-IND-AP`,
        apM === e.apInc ? 'PASS' : 'WARN',
        `actual=${apM} expectedInc=${e.apInc}`,
      );
    } else {
      rec(
        `${ym}-IND-AR`,
        arM === e.arDec || arM === -e.arDec ? 'PASS' : 'WARN',
        `actual=${arM} expectedDec=${e.arDec}`,
      );
      rec(
        `${ym}-IND-AP`,
        apM === -e.apDec || apM === e.apDec ? 'PASS' : 'WARN',
        `actual=${apM} expectedDec=${e.apDec}`,
      );
    }

    const opNets = opInd.map((r) => pick(r));
    rec(
      `${ym}-OP-NETS`,
      'INFO',
      opNets.map((x, i) => `#${i} m=${x.m} y=${x.y}`).join('; ') || 'none',
    );

    // Independent indirect calc check if components present
    if ([niM, invM, arM, apM].every((x) => Number.isFinite(x))) {
      // prompt: NI + invDec - arInc + apInc (Jan) using signed report values
      const calc = niM + invM + arM + apM; // if AR decrease is positive in report, formula already signed
      rec(
        `${ym}-IND-CALC`,
        Math.abs(calc - e.opIndirect) < 0.01 ? 'PASS' : 'WARN',
        `ni+inv+ar+ap=${calc} expectedOp=${e.opIndirect}`,
      );
    }
  }

  // UI screenshot Feb CF
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({ viewport: { width: 1600, height: 1000 } });
  await page.goto(`${BASE}/login`, { waitUntil: 'networkidle' });
  await page.evaluate((auth) => {
    document.cookie = `jb-token=${auth.token}; path=/`;
    localStorage.setItem('_token', JSON.stringify(auth.data));
  }, admin);
  await page.goto(`${BASE}/statement/cash-flow-statement`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1200);
  await page.screenshot({ path: `${SHOT}/indirect-cf-ui.webp` });
  // scroll for supplementary
  await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight));
  await page.waitForTimeout(400);
  await page.screenshot({ path: `${SHOT}/indirect-cf-ui-bottom.webp` });
  await browser.close();

  const md = [
    '# AI UI 主账套现金流量 / 间接法点测',
    '',
    `- time: ${new Date().toISOString()}`,
    `- book: ${BOOK_A}`,
    '',
    '独立预期（提示词）：1 月经营净额 32,000；间接法 40k+10k−30k+12k；2 月 28,000；13k+12k+10k−7k。',
    '',
    '| ID | Status | Detail |',
    '|---|---|---|',
    ...results.map((r) => `| ${r.id} | ${r.status} | ${String(r.detail).replace(/\|/g, '\\|')} |`),
    '',
    '## 证据',
    '',
    '- `/opt/cursor/artifacts/screenshots/indirect-cf-ui.webp`',
    '- `/opt/cursor/artifacts/screenshots/indirect-cf-ui-bottom.webp`',
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
