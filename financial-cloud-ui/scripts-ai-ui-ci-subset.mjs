/**
 * CI / local subset of post-merge AI UI regression.
 * Suites: section6 + remaining-gaps + final-gaps + guards + export-check.
 *
 * Env:
 *   AI_UI_BASE   default http://127.0.0.1:3154
 *   AI_UI_API    default http://127.0.0.1:2154
 *   AI_UI_REQUIRE_BOOKS=1  fail when AI-UI-20260930 books missing (default)
 *   AI_UI_REQUIRE_BOOKS=0  exit 0 with notice when books missing (CI fixture gate)
 */
import { spawn } from 'child_process';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(__dirname);
const REPO = path.resolve(ROOT, '..');
const REPORT = path.resolve(REPO, 'docs/testing/ai-ui-ci-subset-report.md');
const LOG_DIR = process.env.AI_UI_LOG_DIR || '/tmp/ai-ui-ci-subset';
const API = process.env.AI_UI_API || process.env.E2E_API_URL || 'http://127.0.0.1:2154';
const REQUIRE_BOOKS = process.env.AI_UI_REQUIRE_BOOKS !== '0';
const MARK = 'AI-UI-20260930';

const SUITES = [
  { id: 'section6', script: 'scripts-ai-ui-section6.mjs', title: '§六 校验/批量/权限' },
  { id: 'remaining-gaps', script: 'scripts-ai-ui-remaining-gaps.mjs', title: '剩余细项（工资/固资/辅助/待办）' },
  { id: 'final-gaps', script: 'scripts-ai-ui-final-gaps.mjs', title: '收尾细项（累计预扣/模板/账龄/互斥）' },
  { id: 'guards', script: 'scripts-ai-ui-guards.mjs', title: '反结账/闭账守卫' },
  { id: 'export-check', script: 'scripts-ai-ui-export-check.mjs', title: '导出内容级校验' },
];

async function apiLogin() {
  const g = await fetch(`${API}/api/login/get?_allow_anonymous=true`).then((r) => r.json());
  const s = await fetch(`${API}/api/login/signin?_allow_anonymous=true`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      username: 'admin',
      password: 'changeme',
      captcha: '',
      state: g.data?.state,
      authType: 'normal',
    }),
  }).then((r) => r.json());
  if (s.code !== 0) throw new Error(`login: ${s.message}`);
  return s.data.token;
}

async function hasAiUiBooks(token) {
  const res = await fetch(`${API}/api/book/fetchAll`, {
    headers: { Authorization: `Bearer ${token}` },
  }).then((r) => r.json());
  const books = res.data || [];
  const names = books.map((b) => String(b.name || ''));
  const hits = names.filter((n) => n.includes(MARK));
  return { ok: hits.length > 0, hits, total: books.length };
}

function runSuite(script) {
  return new Promise((resolve) => {
    const logPath = path.join(LOG_DIR, `${path.basename(script, '.mjs')}.log`);
    const out = fs.createWriteStream(logPath);
    const child = spawn('node', [script], {
      cwd: ROOT,
      env: process.env,
      stdio: ['ignore', 'pipe', 'pipe'],
    });
    let buf = '';
    const onChunk = (c) => {
      const s = c.toString();
      buf += s;
      out.write(s);
      process.stdout.write(s);
    };
    child.stdout.on('data', onChunk);
    child.stderr.on('data', onChunk);
    child.on('close', (code) => {
      out.end();
      const pass = (buf.match(/\[PASS\]/g) || []).length;
      const fail = (buf.match(/\[FAIL\]/g) || []).length;
      const warn = (buf.match(/\[WARN\]/g) || []).length;
      let summary = null;
      const summaryIdx = buf.lastIndexOf('=== SUMMARY ===');
      if (summaryIdx >= 0) {
        const after = buf.slice(summaryIdx + '=== SUMMARY ==='.length);
        const brace = after.indexOf('{');
        if (brace >= 0) {
          let depth = 0;
          let end = -1;
          for (let i = brace; i < after.length; i++) {
            if (after[i] === '{') depth++;
            else if (after[i] === '}') {
              depth--;
              if (depth === 0) {
                end = i;
                break;
              }
            }
          }
          if (end > brace) {
            try {
              const parsed = JSON.parse(after.slice(brace, end + 1));
              summary = parsed.summary && typeof parsed.summary === 'object' ? parsed.summary : parsed;
            } catch {
              summary = null;
            }
          }
        }
      }
      resolve({
        code: code ?? 1,
        logPath,
        summary: summary || { pass, fail, warn, observations: [] },
      });
    });
  });
}

function writeReport(lines) {
  fs.mkdirSync(path.dirname(REPORT), { recursive: true });
  fs.writeFileSync(REPORT, lines.join('\n'), 'utf8');
  console.log(`Wrote ${REPORT}`);
}

async function main() {
  fs.mkdirSync(LOG_DIR, { recursive: true });
  const started = new Date().toISOString();

  let books;
  try {
    const token = await apiLogin();
    books = await hasAiUiBooks(token);
  } catch (err) {
    const msg = String(err.message || err);
    writeReport([
      '# AI UI CI 子集报告',
      '',
      `- **开始**：${started}`,
      `- **API**：\`${API}\``,
      `- **结论**：FAIL（无法登录/连接后端）`,
      '',
      '```',
      msg,
      '```',
      '',
    ]);
    console.error(msg);
    process.exit(1);
  }

  if (!books.ok) {
    const lines = [
      '# AI UI CI 子集报告',
      '',
      `- **开始**：${started}`,
      `- **API**：\`${API}\``,
      `- **结论**：SKIPPED（缺少 \`${MARK}\` 账套夹具）`,
      `- **账套总数**：${books.total}`,
      `- **命中**：${books.hits.join(', ') || '（无）'}`,
      '',
      '## 阻塞说明',
      '',
      '本子集依赖已存在的 `AI-UI-20260930` 系列账套（A/B/C 等），',
      '不是从空库冷启动的 e2e。GitHub Actions 默认 `mysql` 初始化后没有这些夹具。',
      '',
      '- 本地/Cloud Agent：保留夹具后执行 `npm run test:ai-ui:ci-subset`',
      '- CI：脚本语法检查 + 本 job 的夹具探测会写出本报告；完整子集需夹具或 `workflow_dispatch` 自备环境',
      '',
      `AI_UI_REQUIRE_BOOKS=${REQUIRE_BOOKS ? '1' : '0'}`,
      '',
    ];
    writeReport(lines);
    if (REQUIRE_BOOKS) {
      console.error('AI-UI books missing and AI_UI_REQUIRE_BOOKS=1');
      process.exit(1);
    }
    console.log('::notice::AI-UI CI subset skipped: fixture books not present');
    process.exit(0);
  }

  const results = [];
  for (const suite of SUITES) {
    console.log(`\n-------- RUN ${suite.id}: ${suite.script} --------\n`);
    const t0 = Date.now();
    const r = await runSuite(suite.script);
    const s = r.summary || {};
    const fail = s.fail ?? 0;
    const pass = s.pass ?? 0;
    const warn = s.warn ?? 0;
    const ok = r.code === 0 && fail === 0;
    results.push({
      ...suite,
      ok,
      exitCode: r.code,
      pass,
      fail,
      warn,
      elapsedMs: Date.now() - t0,
      logPath: r.logPath,
    });
  }

  const ended = new Date().toISOString();
  const suitesFail = results.filter((r) => !r.ok).length;
  const overall = suitesFail === 0 ? 'PASS' : 'FAIL';
  const totalPass = results.reduce((a, r) => a + r.pass, 0);
  const totalFail = results.reduce((a, r) => a + r.fail, 0);
  const totalWarn = results.reduce((a, r) => a + r.warn, 0);

  writeReport([
    '# AI UI CI 子集报告',
    '',
    `- **开始**：${started}`,
    `- **结束**：${ended}`,
    `- **API**：\`${API}\``,
    `- **账套夹具**：${books.hits.join(', ')}`,
    `- **编排**：\`financial-cloud-ui/scripts-ai-ui-ci-subset.mjs\``,
    '',
    `## 结论：**${overall}**（套件 ${results.length - suitesFail}/${results.length}；PASS ${totalPass} / FAIL ${totalFail} / WARN ${totalWarn}）`,
    '',
    '| 套件 | 结果 | PASS | FAIL | WARN | 耗时 |',
    '|---|---|---:|---:|---:|---:|',
    ...results.map((r) => {
      const sec = Math.round(r.elapsedMs / 1000);
      return `| ${r.title} | ${r.ok ? 'PASS' : 'FAIL'} | ${r.pass} | ${r.fail} | ${r.warn} | ${sec}s |`;
    }),
    '',
    '## 支持范围',
    '',
    '仅本子集 5 个套件；不宣称全系统全部通过。',
    '',
  ]);

  process.exit(suitesFail === 0 ? 0 : 1);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
