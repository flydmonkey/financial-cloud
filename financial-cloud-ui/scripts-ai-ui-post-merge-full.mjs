/**
 * Post-merge full regression on main (AI-UI-20260930 books already present).
 * Runs verification suites sequentially and writes an aggregate report.
 */
import { spawn } from 'child_process';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(__dirname);
const REPORT = path.resolve(ROOT, '../docs/testing/ai-ui-post-merge-full-retest-report.md');
const LOG_DIR = '/tmp/ai-ui-post-merge';

/** Suites that verify existing test books (prefer idempotent / restore-friendly). */
const SUITES = [
  { id: 'section6', script: 'scripts-ai-ui-section6.mjs', title: '§六 校验/批量/权限' },
  { id: 'remaining-gaps', script: 'scripts-ai-ui-remaining-gaps.mjs', title: '剩余细项（工资/固资/辅助/待办）' },
  { id: 'final-gaps', script: 'scripts-ai-ui-final-gaps.mjs', title: '收尾细项（累计预扣/模板/账龄/互斥）' },
  { id: 'optional', script: 'scripts-ai-ui-optional-workbench-tax.mjs', title: '可选：工作台/封存/税费' },
  { id: 'guards', script: 'scripts-ai-ui-guards.mjs', title: '反结账/闭账守卫' },
  { id: 'export-check', script: 'scripts-ai-ui-export-check.mjs', title: '导出内容级校验' },
  { id: 'verify-fixes', script: 'scripts-ai-ui-verify-fixes.mjs', title: '历史缺陷修复抽检' },
  { id: 'indirect-cf', script: 'scripts-ai-ui-indirect-cf.mjs', title: '间接法现金流量抽检' },
  { id: 'book-b-journal-ext', script: 'scripts-ai-ui-book-b-journal-ext.mjs', title: '专项 B 日记账扩展' },
];

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
      // Fallback: count [PASS]/[FAIL]/[WARN]
      const pass = (buf.match(/\[PASS\]/g) || []).length;
      const fail = (buf.match(/\[FAIL\]/g) || []).length;
      const warn = (buf.match(/\[WARN\]/g) || []).length;
      resolve({
        code: code ?? 1,
        logPath,
        summary: summary || { pass, fail, warn, observations: [] },
        tail: buf.slice(-800),
      });
    });
  });
}

async function main() {
  fs.mkdirSync(LOG_DIR, { recursive: true });
  fs.mkdirSync(path.dirname(REPORT), { recursive: true });

  const started = new Date().toISOString();
  const results = [];

  console.log(`\n=== POST-MERGE FULL RETEST @ ${started} ===\n`);

  for (const suite of SUITES) {
    console.log(`\n-------- RUN ${suite.id}: ${suite.script} --------\n`);
    const t0 = Date.now();
    const r = await runSuite(suite.script);
    const elapsedMs = Date.now() - t0;
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
      elapsedMs,
      observations: s.observations || [],
      logPath: r.logPath,
    });
    console.log(
      `\n<< ${suite.id} done exit=${r.code} PASS=${pass} FAIL=${fail} WARN=${warn} ${elapsedMs}ms >>\n`,
    );
  }

  const ended = new Date().toISOString();
  const totalPass = results.reduce((a, r) => a + r.pass, 0);
  const totalFail = results.reduce((a, r) => a + r.fail, 0);
  const totalWarn = results.reduce((a, r) => a + r.warn, 0);
  const suitesOk = results.filter((r) => r.ok).length;
  const suitesFail = results.filter((r) => !r.ok).length;
  const overall = suitesFail === 0 ? 'PASS' : 'FAIL';

  const lines = [
    '# AI UI 合入 main 后全量复测报告',
    '',
    `- **基线**：\`origin/main\` @ \`52ebaed\`（Merge PR #17）`,
    `- **环境**：前端 \`http://127.0.0.1:3154\` / 后端 \`http://127.0.0.1:2154\` / MySQL \`:3307\``,
    `- **账套**：保留 \`AI-UI-20260930\` 系列（A/B/C/D/封存探针）`,
    `- **开始**：${started}`,
    `- **结束**：${ended}`,
    `- **编排脚本**：\`financial-cloud-ui/scripts-ai-ui-post-merge-full.mjs\``,
    '',
    `## 结论：**${overall}**（套件 ${suitesOk}/${results.length} 通过；步骤合计 PASS ${totalPass} / FAIL ${totalFail} / WARN ${totalWarn}）`,
    '',
    '## 套件结果',
    '',
    '| 套件 | 结果 | PASS | FAIL | WARN | 耗时 |',
    '|---|---|---:|---:|---:|---:|',
    ...results.map((r) => {
      const status = r.ok ? 'PASS' : 'FAIL';
      const sec = Math.round(r.elapsedMs / 1000);
      return `| ${r.title} | ${status} | ${r.pass} | ${r.fail} | ${r.warn} | ${sec}s |`;
    }),
    '',
    '## 观察',
    '',
  ];

  const obs = results.flatMap((r) =>
    (r.observations || []).map((o) => `- **${r.id}**：${o}`),
  );
  if (obs.length === 0) {
    lines.push('_无额外观察（或各子报告已记录）_');
  } else {
    lines.push(...obs);
  }

  lines.push(
    '',
    '## 日志',
    '',
    ...results.map((r) => `- \`${r.logPath}\``),
    '',
    '## 明细子报告',
    '',
    '- `docs/testing/ai-ui-section6-report.md`',
    '- `docs/testing/ai-ui-remaining-gaps-report.md`',
    '- `docs/testing/ai-ui-final-gaps-report.md`',
    '- `docs/testing/ai-ui-optional-workbench-tax-report.md`',
    '- `docs/testing/ai-ui-guards-report.md`',
    '- `docs/testing/ai-ui-export-check-report.md`',
    '- （及 verify-fixes / indirect-cf / book-b-journal-ext 既有报告，若脚本有写出）',
    '',
  );

  fs.writeFileSync(REPORT, lines.join('\n'), 'utf8');
  console.log(`\nWrote ${REPORT}`);
  console.log(`OVERALL ${overall} suitesOk=${suitesOk}/${results.length} PASS=${totalPass} FAIL=${totalFail} WARN=${totalWarn}`);
  process.exit(suitesFail === 0 ? 0 : 1);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
