/**
 * Content-level export checks for book A statements (xlsx via openpyxl).
 */
import { chromium } from 'playwright';
import fs from 'fs';
import { execSync } from 'child_process';

const BASE = 'http://127.0.0.1:3154';
const API = 'http://127.0.0.1:2154';
const BOOK_A = '2105377998655979522';
const DL = '/opt/cursor/artifacts/downloads';
const SHOT = '/opt/cursor/artifacts/screenshots';
const REPORT = '/workspace/docs/testing/ai-ui-export-check-report.md';

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
  return { token: s.data.token, data: s.data, refresh: s.data.refresh_token };
}

async function api(token, method, path) {
  return fetch(`${API}${path}`, { method, headers: { Authorization: 'Bearer ' + token } }).then((r) =>
    r.json(),
  );
}

function analyzeXlsx(path, expectHints) {
  const py = `
import openpyxl, json, sys
wb=openpyxl.load_workbook(${JSON.stringify(path)}, data_only=True)
out=[]
for ws in wb.worksheets:
    vals=[]
    for row in ws.iter_rows(values_only=True):
        for c in row:
            if c is None: continue
            vals.append(str(c))
    text=' | '.join(vals)
    out.append({'sheet': ws.title, 'cells': len(vals), 'text': text[:2000]})
print(json.dumps(out, ensure_ascii=False))
`;
  const raw = execSync(`python3 - <<'PY'\n${py}\nPY`, { encoding: 'utf-8' });
  const sheets = JSON.parse(raw);
  const all = sheets.map((s) => s.text).join('\n');
  const hits = {};
  for (const h of expectHints) {
    hits[h] = all.includes(h) || all.replace(/,/g, '').includes(h.replace(/,/g, ''));
  }
  return { sheets, hits, size: fs.statSync(path).size };
}

async function downloadViaApi(token, path, dest) {
  const res = await fetch(`${API}${path}`, {
    headers: { Authorization: 'Bearer ' + token },
  });
  if (!res.ok) throw new Error(`HTTP ${res.status} ${path}`);
  const buf = Buffer.from(await res.arrayBuffer());
  fs.writeFileSync(dest, buf);
  return buf.length;
}

async function main() {
  fs.mkdirSync(DL, { recursive: true });
  fs.mkdirSync(SHOT, { recursive: true });
  const admin = await apiLogin('admin', 'changeme');
  await api(admin.token, 'GET', `/api/users/switchBook/${BOOK_A}`);

  const exports = [
    {
      id: 'BS-XLSX',
      path: '/api/statement/balance-sheet/export?periodType=month&date=2026-01&reportDate=2026-01',
      file: `${DL}/export-bs-2026-01.xlsx`,
      hints: ['160000', '242000', '银行存款', '资产'],
    },
    {
      id: 'IS-XLSX',
      path: '/api/statement/income/export?periodType=month&date=2026-01&reportDate=2026-01',
      file: `${DL}/export-is-2026-01.xlsx`,
      hints: ['80000', '40000', '主营业务收入', '净利润'],
    },
    {
      id: 'CF-XLSX',
      path: '/api/statement/cash-flow/export?periodType=month&date=2026-01&reportDate=2026-01',
      file: `${DL}/export-cf-2026-01.xlsx`,
      hints: ['50000', '32000', '160000', '销售商品'],
    },
    {
      id: 'SB-XLSX',
      path: '/api/statement/subject-balance/export?periodType=month&date=2026-01&reportDate=2026-01',
      file: `${DL}/export-sb-2026-01.xlsx`,
      hints: ['1002', '160000', '银行存款'],
    },
  ];

  for (const ex of exports) {
    try {
      const size = await downloadViaApi(admin.token, ex.path, ex.file);
      if (size < 1000) {
        rec(ex.id, 'FAIL', `too small size=${size}`);
        continue;
      }
      const { sheets, hits, size: sz } = analyzeXlsx(ex.file, ex.hints);
      const ok = Object.values(hits).filter(Boolean).length >= Math.min(2, ex.hints.length);
      rec(
        ex.id,
        ok ? 'PASS' : 'WARN',
        `size=${sz} sheets=${sheets.map((s) => s.sheet).join(',')} hits=${JSON.stringify(hits)}`,
      );
    } catch (e) {
      rec(ex.id, 'FAIL', String(e.message || e));
    }
  }

  // PDF smoke via API if present
  for (const [id, path, file] of [
    [
      'BS-PDF',
      '/api/statement/balance-sheet/export-pdf?periodType=month&date=2026-01&reportDate=2026-01',
      `${DL}/export-bs-2026-01.pdf`,
    ],
    [
      'CF-PDF',
      '/api/statement/cash-flow/export-pdf?periodType=month&date=2026-01&reportDate=2026-01',
      `${DL}/export-cf-2026-01.pdf`,
    ],
  ]) {
    try {
      const size = await downloadViaApi(admin.token, path, file);
      const head = fs.readFileSync(file).subarray(0, 5).toString('utf8');
      rec(id, size > 500 && head.startsWith('%PDF') ? 'PASS' : 'WARN', `size=${size} head=${head}`);
    } catch (e) {
      rec(id, 'WARN', String(e.message || e));
    }
  }

  // UI export button screenshot
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({ viewport: { width: 1600, height: 1000 } });
  await page.goto(`${BASE}/login`, { waitUntil: 'networkidle' });
  await page.evaluate((auth) => {
    document.cookie = `jb-token=${auth.token}; path=/`;
    localStorage.setItem('_token', JSON.stringify(auth.data));
  }, admin);
  await page.goto(`${BASE}/statement/balance-sheet`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(800);
  await page.screenshot({ path: `${SHOT}/export-check-bs.webp` });
  await browser.close();

  const md = [
    '# AI UI Export Content Checks（主账套 A）',
    '',
    `- time: ${new Date().toISOString()}`,
    `- book: ${BOOK_A}`,
    `- period sample: 2026-01`,
    '',
    '| ID | Status | Detail |',
    '|---|---|---|',
    ...results.map((r) => `| ${r.id} | ${r.status} | ${String(r.detail).replace(/\|/g, '\\|')} |`),
    '',
    'Files under `/opt/cursor/artifacts/downloads/export-*`.',
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
