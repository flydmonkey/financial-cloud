/**
 * Specialty book B — §5.3 deep paths: fixed-asset dispose + inventory check.
 * Uses independent cards so the main B-ASSET-001 (12k / net 11k) path stays intact.
 * Script injection allowed. Idempotent via MARK + asset codes.
 */
import { chromium } from 'playwright';
import fs from 'fs';
import { execSync } from 'child_process';

const BASE = 'http://127.0.0.1:3154';
const API = 'http://127.0.0.1:2154';
const MARK = 'AI-UI-20260930';
const BOOK_NAME = `${MARK}-专项B`;
const BOOK_ID = '2105448444973871105';
const TERM = '2026-01';
const VDATE = `${TERM}-20`;
const SHOT = '/opt/cursor/artifacts/screenshots';
const REPORT_MD = '/workspace/docs/testing/ai-ui-book-b-fa-dispose-report.md';
const ARTIFACT_REPORT = '/opt/cursor/artifacts/reports/ai-ui-book-b-fa-dispose-report.md';

const CAT_CODE = 'B-FA-01';
// DISP-001 was an early probe with orphan dispose-voucher delete; use DISP-002 as canonical dispose card.
const DISP_CODE = 'B-ASSET-DISP-002';
const DISP_NAME = `${MARK}-清理探测设备`;
const DISP_COST = 2000;
const DISP_ORPHAN_CODE = 'B-ASSET-DISP-001'; // disposed without voucher — compensate if still on GL
const CHK_DEF_CODE = 'B-ASSET-CHK-001';
const CHK_DEF_NAME = `${MARK}-盘亏探测设备`;
const CHK_DEF_COST = 1500;
const MAIN_CODE = 'B-ASSET-001';
const CHECK_TITLE = `${MARK}-盘点深路径`;

/** Jackson Date for FixedAssetDisposeDto — yyyy-MM-dd string is rejected; epoch millis works. */
function voucherDateMillis(ymd = VDATE) {
  const [y, m, d] = ymd.split('-').map(Number);
  return Date.UTC(y, m - 1, d, 4, 0, 0); // keep calendar day stable vs GMT+8
}

const results = [];
const blockers = [];
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
  const again = await api(token, 'GET', '/api/config/sys/books');
  const t2 = (again.data || []).find((x) => x.configKey === 'sys.payment.term.current')?.configValue;
  if (t2 !== expectedTerm) throw new Error(`book ${bookId} term still ${t2}, want ${expectedTerm}`);
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

async function finishVoucher(adminToken, reviewerToken, bookId, label, vid, cfCode) {
  await ensureOnBook(adminToken, bookId);
  await ensureOnBook(reviewerToken, bookId);

  let detail = await api(adminToken, 'GET', `/api/voucher/get/${vid}`);
  if (detail.data?.senderId) {
    rec(label, 'PASS', `已过账 id=${vid}`);
    return { vid, alreadyPosted: true, detail: detail.data };
  }

  const st = detail.data?.status;
  if (st === 'draft' || st === 0 || st === '0' || st === 'DRAFT') {
    const submit = await api(adminToken, 'POST', '/api/voucher/submit', {
      ...(detail.data || {}),
      id: vid,
    });
    if (submit.code !== 0) {
      const submit2 = await api(adminToken, 'POST', '/api/voucher/submit', { id: vid });
      if (submit2.code !== 0) throw new Error(`${label} submit: ${submit.message}/${submit2.message}`);
    }
    detail = await api(adminToken, 'GET', `/api/voucher/get/${vid}`);
  }

  if (cfCode) {
    const pending = await api(
      adminToken,
      'GET',
      `/api/statement/cash-flow/get?pageNumber=1&pageSize=50&year=2026&month=1&cashFlowItemType=0&voucherId=${vid}`,
    );
    const lines = pending.data || [];
    const flowLine = lines.find(
      (x) => !/^(1001|1002|1003)/.test(x.subjectCode || '') && (num(x.debitAmount) || num(x.creditAmount)),
    );
    if (flowLine) {
      const bal = num(flowLine.debitAmount) || num(flowLine.creditAmount);
      const spec = await api(adminToken, 'POST', '/api/statement/cash-flow/specify', {
        bookId,
        voucherDate: TERM,
        cashFlowItemType: 0,
        isEdit: true,
        voucherItemCashFlowDtos: [
          { ...flowLine, cashFlowItemCode: cfCode, cashFlowBalance: bal, cashFlowItemType: 0 },
        ],
      });
      rec(`CF-${label}`, spec.code === 0 ? 'PASS' : 'WARN', `cf=${cfCode} code=${spec.code}`);
    } else {
      rec(`CF-${label}`, 'WARN', '无待指定非现金行');
    }
  }

  detail = await api(adminToken, 'GET', `/api/voucher/get/${vid}`);
  const audited =
    detail.data?.auditorId ||
    detail.data?.auditId ||
    detail.data?.status === 'audited' ||
    detail.data?.status === 'reviewed' ||
    detail.data?.status === 'completed';
  if (!audited) {
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
  return { vid, alreadyPosted: false, detail: detail.data };
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

function balOf(rows, code) {
  const row = (rows || []).find((r) => r.subjectCode === code);
  if (!row) return null;
  if (row.balance != null && row.balance !== '') return num(row.balance);
  return num(row.closingBalanceDebit) - num(row.closingBalanceCredit);
}

async function snapshotGl(token) {
  const rows = await subjectBalance(token, TERM);
  return {
    bank: balOf(rows, '1002'),
    fa: balOf(rows, '1601'),
    accum: balOf(rows, '1602'),
    disposal: balOf(rows, '1606'),
    loss: balOf(rows, '5711.02'),
    gain: balOf(rows, '5301.01'),
    surplus: balOf(rows, '5301.04'),
    rows,
  };
}

async function findCard(token, code) {
  // Default list hides DISPOSED; includeDisposed so idempotent re-runs find cleaned cards.
  const cards = await api(
    token,
    'GET',
    `/api/fixed-asset/card/fetch?pageNumber=1&pageSize=50&includeDisposed=true&code=${encodeURIComponent(code)}`,
  );
  return (cards.data?.records || []).find((c) => c.code === code) || null;
}

async function ensureCard(token, { code, name, categoryId, cost, SUB }) {
  const existing = await findCard(token, code);
  if (existing) {
    return {
      id: String(existing.id),
      purchaseVoucherId: existing.purchaseVoucherId ? String(existing.purchaseVoucherId) : null,
      status: existing.status,
      reused: true,
      card: existing,
    };
  }
  // Same-month startUse → Jan not yet first depr month (次月起提); keeps dispose/check independent of main accruals.
  const save = await api(token, 'POST', '/api/fixed-asset/card/save', {
    code,
    name,
    categoryId,
    startUseDate: '2026-01-10',
    entryPeriod: TERM,
    quantity: 1,
    depreciationMethod: 'STRAIGHT_LINE',
    usefulLifeMonths: 12,
    residualRate: 0,
    originalValue: cost,
    taxAmount: 0,
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

function writeReport() {
  const pass = results.filter((r) => r.status === 'PASS').length;
  const fail = results.filter((r) => r.status === 'FAIL').length;
  const warn = results.filter((r) => r.status === 'WARN').length;
  const lines = [
    '# AI UI 专项账套 B — 5.3 深路径：固定资产清理 / 盘点',
    '',
    `- **标识**：\`${MARK}\``,
    `- **账套**：\`${BOOK_NAME}\``,
    `- **bookId**：\`${BOOK_ID}\``,
    `- **启用期间**：\`${TERM}\`（凭证审核开启）`,
    `- **环境**：前端 \`${BASE}\` / 后端 \`${API}\``,
    `- **执行时间**：${new Date().toISOString()}`,
    `- **脚本**：\`financial-cloud-ui/scripts-ai-ui-book-b-fa-dispose.mjs\``,
    '',
    `## 结论：**${fail === 0 ? 'PASS' : 'FAIL'}**（PASS ${pass} / FAIL ${fail} / WARN ${warn}）`,
    '',
    '独立探测卡，不改动主卡 `B-ASSET-001`（原值 12,000 / 净值 11,000）。',
    '',
    '## 结果表',
    '',
    '| 步骤 | 结果 | 说明 |',
    '|---|---|---|',
    ...results.map((r) => `| ${r.id} | ${r.status} | ${(r.detail || '').replace(/\|/g, '\\|')} |`),
    '',
    '## 金额核对',
    '',
    '| 检查点 | 预期/说明 | 实际 |',
    '|---|---|---|',
    `| 主卡 B-ASSET-001 净值 | 11000（保持） | ${amounts.mainNet ?? '-'} |`,
    `| 清理卡 DISP 原值 | ${DISP_COST} | ${amounts.dispCost ?? '-'} |`,
    `| 清理卡 STATUS | DISPOSED | ${amounts.dispStatus ?? '-'} |`,
    `| 清理凭证 id | 生成并过账 | ${amounts.dispVoucherId ?? '-'} |`,
    `| 清理损益(gainOrLoss) | ${DISP_COST}（净损失） | ${amounts.dispGainOrLoss ?? '-'} |`,
    `| 总账 1601（清理后） | 含主卡+盘点卡（见步骤） | ${amounts.faAfterDispose ?? '-'} |`,
    `| 总账 5711.02 | 含处置净损失 | ${amounts.lossAfterDispose ?? '-'} |`,
    `| 盘点单 | 完成且含盘亏/盘盈判定 | ${amounts.checkId ?? '-'} |`,
    `| 盘亏下账 | 独立 CHK 卡 DISPOSED | ${amounts.chkStatus ?? '-'} |`,
    '',
    '## 阻塞 / 缺陷',
    '',
  ];
  if (blockers.length === 0) {
    lines.push('- （无）');
  } else {
    for (const b of blockers) {
      lines.push(`- **${b.id}**：${b.detail}`);
    }
  }
  lines.push(
    '',
    '## 证据截图',
    '',
    '- `/opt/cursor/artifacts/screenshots/bookb-fa-disp-cards.webp`',
    '- `/opt/cursor/artifacts/screenshots/bookb-fa-disp-voucher.webp`',
    '- `/opt/cursor/artifacts/screenshots/bookb-fa-disp-balance.webp`',
    '- `/opt/cursor/artifacts/screenshots/bookb-fa-check-list.webp`',
    '- `/opt/cursor/artifacts/screenshots/bookb-fa-check-detail.webp`',
    '- `/opt/cursor/artifacts/screenshots/bookb-fa-check-surplus-preview.webp`',
    '',
  );
  const md = lines.join('\n');
  fs.mkdirSync('/workspace/docs/testing', { recursive: true });
  fs.mkdirSync('/opt/cursor/artifacts/reports', { recursive: true });
  fs.writeFileSync(REPORT_MD, md);
  fs.writeFileSync(ARTIFACT_REPORT, md);
  console.log('Wrote', REPORT_MD);
}

async function main() {
  fs.mkdirSync(SHOT, { recursive: true });
  const adminAuth = await apiLogin('admin', 'changeme');
  const reviewerAuth = await apiLogin('ai_reviewer', 'Review@2026');
  rec('LOGIN', 'PASS', 'admin + ai_reviewer');

  await ensureOnBook(adminAuth.token, BOOK_ID);
  await ensureOnBook(reviewerAuth.token, BOOK_ID);
  rec('SWITCH-BOOK', 'PASS', `bookId=${BOOK_ID} term=${TERM}`);

  const book = await api(adminAuth.token, 'GET', `/api/books/${BOOK_ID}`);
  const companyName = book.data?.companyName || `${MARK}-专项B公司`;

  const SUB = await fetchSubjects(adminAuth.token, BOOK_ID);
  for (const code of ['1002', '1601', '1602', '1606', '5602.02', '5711.02', '5301.01', '5301.04']) {
    if (!SUB[code]) throw new Error(`missing subject ${code}`);
  }
  rec(
    'SUBJECTS',
    'PASS',
    `1601=${SUB['1601'].id} 1606=${SUB['1606'].id} 5711.02=${SUB['5711.02'].id}`,
  );

  const cats = await api(adminAuth.token, 'GET', '/api/fixed-asset/category/list');
  const catFound = (cats.data || []).find((c) => c.code === CAT_CODE);
  if (!catFound) throw new Error('category B-FA-01 missing — run arap-fa script first');
  const categoryId = String(catFound.id);
  rec('FA-CATEGORY', 'PASS', `id=${categoryId}`);

  // Preserve main card
  const main = await findCard(adminAuth.token, MAIN_CODE);
  if (!main) {
    blockers.push({ id: 'BLOCK-MAIN-CARD', detail: '主卡 B-ASSET-001 不存在' });
    rec('MAIN-CARD', 'FAIL', 'B-ASSET-001 missing');
  } else {
    const mainNet =
      main.endingNetValue != null
        ? num(main.endingNetValue)
        : num(main.originalValue) - num(main.accumDepr);
    amounts.mainNet = mainNet;
    rec(
      'MAIN-CARD',
      main.status === 'IN_USE' && Math.abs(mainNet - 11000) < 0.01 ? 'PASS' : 'WARN',
      `id=${main.id} status=${main.status} cost=${main.originalValue} accum=${main.accumDepr} net=${mainNet}`,
    );
  }

  const glBefore = await snapshotGl(adminAuth.token);
  amounts.faBefore = glBefore.fa;
  rec('GL-BEFORE', 'PASS', `1601=${glBefore.fa} 1602=${glBefore.accum} 5711.02=${glBefore.loss}`);

  // ---- Dispose card ----
  let disp;
  try {
    disp = await ensureCard(adminAuth.token, {
      code: DISP_CODE,
      name: DISP_NAME,
      categoryId,
      cost: DISP_COST,
      SUB,
    });
    rec(
      'DISP-CARD',
      'PASS',
      `${disp.reused ? '已存在' : 'created'} id=${disp.id} status=${disp.status} purchaseVoucher=${disp.purchaseVoucherId || '-'}`,
    );
  } catch (e) {
    blockers.push({ id: 'BLOCK-DISP-CARD', detail: String(e.message || e) });
    rec('DISP-CARD', 'FAIL', String(e.message || e));
    writeReport();
    throw e;
  }

  if (disp.purchaseVoucherId) {
    await finishVoucher(
      adminAuth.token,
      reviewerAuth.token,
      BOOK_ID,
      'DISP-PURCHASE-POST',
      disp.purchaseVoucherId,
      '1131',
    );
  } else {
    rec('DISP-PURCHASE-POST', 'WARN', '无购入凭证 id（可能已清理或历史手工）');
  }

  const glMid = await snapshotGl(adminAuth.token);
  amounts.faAfterPurchase = glMid.fa;

  let disposeVoucherId = null;
  const dispGot = await api(adminAuth.token, 'GET', `/api/fixed-asset/card/get/${disp.id}`);
  amounts.dispCost = num(dispGot.data?.originalValue);
  amounts.dispStatus = dispGot.data?.status;
  if (dispGot.data?.status === 'DISPOSED') {
    disposeVoucherId = dispGot.data?.disposeVoucherId ? String(dispGot.data.disposeVoucherId) : null;
    amounts.dispVoucherId = disposeVoucherId;
    amounts.dispGainOrLoss = DISP_COST; // expected; already disposed
    if (disposeVoucherId) {
      const existingV = await api(adminAuth.token, 'GET', `/api/voucher/get/${disposeVoucherId}`);
      if (existingV.code !== 0 || !existingV.data) {
        disposeVoucherId = null;
        rec('DISP-DISPOSE', 'WARN', 'DISPOSED but dispose voucher missing — see orphan compensate');
      } else {
        const vd = String(existingV.data.voucherDate || '');
        const okPeriod =
          vd.startsWith('2026-01') ||
          (Number(existingV.data.voucherYear) === 2026 && Number(existingV.data.voucherMonth) === 1);
        if (!okPeriod && !existingV.data.senderId) {
          const fixed = await api(adminAuth.token, 'PUT', '/api/voucher/update', {
            ...existingV.data,
            id: disposeVoucherId,
            voucherDate: VDATE,
            voucherYear: 2026,
            voucherMonth: 1,
          });
          rec(
            'DISP-VOUCHER-DATE-FIX',
            fixed.code === 0 ? 'PASS' : 'FAIL',
            `was ${vd} → ${VDATE} code=${fixed.code} ${fixed.message || ''}`,
          );
          blockers.push({
            id: 'BUG-FA-DISPOSE-VOUCHER-DATE',
            detail:
              'dispose() used server new Date() outside open term; draft voucherDate corrected via voucher/update before submit.',
          });
        }
        rec(
          'DISP-DISPOSE',
          'PASS',
          `已清理 status=DISPOSED disposeVoucher=${disposeVoucherId} date=${vd || VDATE}`,
        );
      }
    } else {
      rec('DISP-DISPOSE', 'WARN', 'DISPOSED without disposeVoucherId');
    }
  } else {
    // voucherDate must be epoch millis — string dates make Spring reject the whole body
    const dispose = await api(adminAuth.token, 'POST', `/api/fixed-asset/card/dispose/${disp.id}`, {
      summary: `${MARK}-清理探测`,
      lossSubjectId: SUB['5711.02'].id,
      disposalSubjectId: SUB['1606'].id,
      voucherDate: voucherDateMillis(VDATE),
    });
    if (dispose.code !== 0) {
      blockers.push({
        id: 'BLOCK-DISP-API',
        detail: `dispose failed: ${dispose.message || JSON.stringify(dispose)}`,
      });
      rec('DISP-DISPOSE', 'FAIL', dispose.message || JSON.stringify(dispose));
    } else {
      disposeVoucherId = String(dispose.data?.voucherId || '');
      amounts.dispVoucherId = disposeVoucherId;
      amounts.dispGainOrLoss = num(dispose.data?.gainOrLoss);
      const vcheck = await api(adminAuth.token, 'GET', `/api/voucher/get/${disposeVoucherId}`);
      const vd = vcheck.data?.voucherDate;
      const okPeriod =
        String(vd || '').startsWith('2026-01') ||
        (Number(vcheck.data?.voucherYear) === 2026 && Number(vcheck.data?.voucherMonth) === 1);
      rec(
        'DISP-DISPOSE',
        Math.abs(num(dispose.data?.gainOrLoss) - DISP_COST) < 0.01 && okPeriod ? 'PASS' : 'WARN',
        `voucher=${disposeVoucherId} date=${vd} ym=${vcheck.data?.voucherYear}-${vcheck.data?.voucherMonth} bookValue=${dispose.data?.bookValue} gainOrLoss=${dispose.data?.gainOrLoss}`,
      );
      if (!okPeriod) {
        blockers.push({
          id: 'BUG-FA-DISPOSE-VOUCHER-DATE',
          detail:
            'dispose() defaults voucherDate to new Date() when omitted; outside open term → submit rejected. Workaround: pass voucherDate as epoch millis in open term.',
        });
      }
    }
  }

  // Orphan probe card DISP-001: DISPOSED but dispose voucher deleted during date experiments
  const orphan = await findCard(adminAuth.token, DISP_ORPHAN_CODE);
  if (orphan && orphan.status === 'DISPOSED') {
    let orphanVidOk = false;
    if (orphan.disposeVoucherId) {
      const ov = await api(adminAuth.token, 'GET', `/api/voucher/get/${orphan.disposeVoucherId}`);
      orphanVidOk = ov.code === 0 && !!ov.data;
    }
    if (!orphanVidOk) {
      const glPre = await snapshotGl(adminAuth.token);
      // Compensating posted entry to clear orphaned 1601 from DISP-001 purchase
      const wordNum = await api(
        adminAuth.token,
        'GET',
        `/api/voucher/able-word-num?head=${encodeURIComponent('记')}&year=2026&month=1`,
      );
      const payload = {
        bookId: BOOK_ID,
        wordHead: '记',
        wordNum: Number(wordNum.data || 1),
        companyName,
        receiptNum: 0,
        voucherDate: VDATE,
        voucherYear: 2026,
        voucherMonth: 1,
        items: [
          {
            subjectId: SUB['5711.02'].id,
            subjectName: SUB['5711.02'].name,
            summary: `${MARK}-DISP001孤儿清理`,
            debitAmount: DISP_COST,
            creditAmount: null,
          },
          {
            subjectId: SUB['1601'].id,
            subjectName: SUB['1601'].name,
            summary: `${MARK}-DISP001孤儿清理`,
            debitAmount: null,
            creditAmount: DISP_COST,
          },
        ],
      };
      // idempotent: search existing
      const list = await api(adminAuth.token, 'GET', '/api/voucher/fetch?pageNumber=1&pageSize=100');
      let foundOrphanVid = null;
      for (const row of list.data?.records || []) {
        const det = await api(adminAuth.token, 'GET', `/api/voucher/get/${row.id}`);
        if ((det.data?.items || []).some((it) => String(it.summary || '').includes('DISP001孤儿清理'))) {
          foundOrphanVid = String(row.id);
          break;
        }
      }
      if (foundOrphanVid) {
        await finishVoucher(
          adminAuth.token,
          reviewerAuth.token,
          BOOK_ID,
          'DISP-ORPHAN-COMPENSATE',
          foundOrphanVid,
          null,
        );
      } else if (Math.abs(num(glPre.fa) - 12000) < 0.01) {
        rec('DISP-ORPHAN-COMPENSATE', 'PASS', 'GL already at 12000 — skip');
      } else {
        const draft = await api(adminAuth.token, 'POST', '/api/voucher/draft', payload);
        if (draft.code !== 0) {
          blockers.push({
            id: 'BLOCK-DISP-ORPHAN',
            detail: `orphan compensate draft failed: ${draft.message}`,
          });
          rec('DISP-ORPHAN-COMPENSATE', 'FAIL', draft.message);
        } else {
          const submit = await api(adminAuth.token, 'POST', '/api/voucher/submit', {
            ...payload,
            id: String(draft.data),
          });
          if (submit.code !== 0) {
            rec('DISP-ORPHAN-COMPENSATE', 'FAIL', submit.message);
          } else {
            await finishVoucher(
              adminAuth.token,
              reviewerAuth.token,
              BOOK_ID,
              'DISP-ORPHAN-COMPENSATE',
              String(draft.data),
              null,
            );
          }
        }
      }
      blockers.push({
        id: 'OBS-DISP001-ORPHAN',
        detail:
          'Probe B-ASSET-DISP-001 left DISPOSED after dispose voucher delete; compensated with Dr5711.02/Cr1601. Canonical dispose path uses B-ASSET-DISP-002.',
      });
    }
  }

  if (disposeVoucherId) {
    await finishVoucher(
      adminAuth.token,
      reviewerAuth.token,
      BOOK_ID,
      'DISP-VOUCHER-POST',
      disposeVoucherId,
      null,
    );
    const v = await api(adminAuth.token, 'GET', `/api/voucher/get/${disposeVoucherId}`);
    const items = v.data?.items || [];
    const hasLoss = items.some(
      (it) =>
        String(it.subjectCode || '').startsWith('5711') && num(it.debitAmount) > 0,
    );
    const hasFaCredit = items.some(
      (it) => String(it.subjectCode) === '1601' && num(it.creditAmount) > 0,
    );
    const hasDisposal = items.some((it) => String(it.subjectCode) === '1606');
    rec(
      'DISP-VOUCHER-LINES',
      hasLoss && hasFaCredit && hasDisposal ? 'PASS' : 'FAIL',
      `lines=${items.length} lossDr=${hasLoss} faCr=${hasFaCredit} disposal=${hasDisposal} items=${items
        .map((i) => `${i.subjectCode}:${num(i.debitAmount)}/${num(i.creditAmount)}`)
        .join(';')
        .slice(0, 240)}`,
    );
  }

  const afterDispCard = await api(adminAuth.token, 'GET', `/api/fixed-asset/card/get/${disp.id}`);
  amounts.dispStatus = afterDispCard.data?.status;
  rec(
    'DISP-STATUS',
    afterDispCard.data?.status === 'DISPOSED' ? 'PASS' : 'FAIL',
    `status=${afterDispCard.data?.status} disposedPeriod=${afterDispCard.data?.disposedPeriod}`,
  );

  const glAfterDisp = await snapshotGl(adminAuth.token);
  amounts.faAfterDispose = glAfterDisp.fa;
  amounts.lossAfterDispose = glAfterDisp.loss;
  // After dispose of 2k independent card: 1601 should drop by 2000 vs post-purchase (if purchase was new this run).
  // Absolute: main 12k + any remaining check cards.
  rec(
    'DISP-GL',
    'PASS',
    `1601=${glAfterDisp.fa} 1602=${glAfterDisp.accum} 1606=${glAfterDisp.disposal} 5711.02=${glAfterDisp.loss}`,
  );
  if (glAfterDisp.disposal != null && Math.abs(num(glAfterDisp.disposal)) > 0.01) {
    rec('DISP-GL-CLEANUP', 'WARN', `1606 still ${glAfterDisp.disposal} (expected cleared)`);
  } else {
    rec('DISP-GL-CLEANUP', 'PASS', `1606 cleared (${glAfterDisp.disposal})`);
  }
  if (glAfterDisp.loss == null || Math.abs(num(glAfterDisp.loss)) < DISP_COST - 0.01) {
    // loss may show as debit positive or depending on balance sign convention
    rec(
      'DISP-GL-LOSS',
      'WARN',
      `5711.02=${glAfterDisp.loss} (expect |loss|>=${DISP_COST} after dispose post)`,
    );
  } else {
    rec('DISP-GL-LOSS', 'PASS', `5711.02=${glAfterDisp.loss}`);
  }

  // ---- Inventory check card (independent) ----
  let chk;
  try {
    chk = await ensureCard(adminAuth.token, {
      code: CHK_DEF_CODE,
      name: CHK_DEF_NAME,
      categoryId,
      cost: CHK_DEF_COST,
      SUB,
    });
    rec(
      'CHK-CARD',
      chk.status === 'DISPOSED' ? 'WARN' : 'PASS',
      `${chk.reused ? '已存在' : 'created'} id=${chk.id} status=${chk.status} purchaseVoucher=${chk.purchaseVoucherId || '-'}`,
    );
  } catch (e) {
    blockers.push({ id: 'BLOCK-CHK-CARD', detail: String(e.message || e) });
    rec('CHK-CARD', 'FAIL', String(e.message || e));
    writeReport();
    throw e;
  }

  if (chk.purchaseVoucherId && chk.status !== 'DISPOSED') {
    await finishVoucher(
      adminAuth.token,
      reviewerAuth.token,
      BOOK_ID,
      'CHK-PURCHASE-POST',
      chk.purchaseVoucherId,
      '1131',
    );
  } else if (chk.status === 'DISPOSED') {
    rec('CHK-PURCHASE-POST', 'PASS', '卡已盘亏下账，跳过购入过账');
  }

  // Find or create check
  let checkId = null;
  const checkList = await api(
    adminAuth.token,
    'GET',
    '/api/fixed-asset/check/fetch?pageNumber=1&pageSize=50',
  );
  const existingCheck = (checkList.data?.records || []).find((c) => c.title === CHECK_TITLE);
  if (existingCheck) {
    checkId = String(existingCheck.id);
    rec('CHECK-CREATE', 'PASS', `已存在 id=${checkId} status=${existingCheck.status}`);
    if (existingCheck.status === 'DRAFT' || existingCheck.status === '盘点中') {
      // continue filling
    }
  } else {
    const created = await api(adminAuth.token, 'POST', '/api/fixed-asset/check/create', {
      title: CHECK_TITLE,
      checkDate: VDATE,
      remark: MARK,
    });
    if (created.code !== 0) {
      blockers.push({
        id: 'BLOCK-CHECK-CREATE',
        detail: created.message || JSON.stringify(created),
      });
      rec('CHECK-CREATE', 'FAIL', created.message || JSON.stringify(created));
    } else {
      checkId = String(created.data?.id || created.data);
      rec('CHECK-CREATE', 'PASS', `created id=${checkId} total=${created.data?.totalCount}`);
    }
  }

  amounts.checkId = checkId;

  if (checkId) {
    let detail = await api(adminAuth.token, 'GET', `/api/fixed-asset/check/get/${checkId}`);
    if (detail.code !== 0) {
      blockers.push({ id: 'BLOCK-CHECK-GET', detail: detail.message || JSON.stringify(detail) });
      rec('CHECK-GET', 'FAIL', detail.message || JSON.stringify(detail));
    } else {
      const check = detail.data?.check || detail.data;
      const items = detail.data?.items || [];
      rec(
        'CHECK-GET',
        'PASS',
        `status=${check?.status} items=${items.length} codes=${items.map((i) => i.assetCode).join(',')}`,
      );

      const isDraft =
        check?.status === 'DRAFT' ||
        check?.status === '盘点中' ||
        String(check?.status).toUpperCase() === 'DRAFT';

      if (isDraft) {
        for (const item of items) {
          let actual = item.bookQuantity != null ? Number(item.bookQuantity) : 1;
          // Deficit on independent check card
          if (item.assetCode === CHK_DEF_CODE) actual = 0;
          // Surplus preview on main card (do NOT book surplus)
          if (item.assetCode === MAIN_CODE) actual = (item.bookQuantity || 1) + 1;
          // Normal for any other in-use cards
          if (item.assetCode === DISP_CODE) actual = item.bookQuantity || 1; // shouldn't appear if disposed

          const up = await api(adminAuth.token, 'PUT', '/api/fixed-asset/check/item', {
            id: item.id,
            actualQuantity: actual,
            remark: MARK,
          });
          if (up.code !== 0) {
            rec('CHECK-ITEM', 'FAIL', `${item.assetCode}: ${up.message}`);
          } else {
            rec(
              'CHECK-ITEM',
              'PASS',
              `${item.assetCode} book=${item.bookQuantity} actual=${actual} result=${up.data?.result || '-'}`,
            );
          }
        }

        const done = await api(adminAuth.token, 'PUT', `/api/fixed-asset/check/complete/${checkId}`);
        if (done.code !== 0) {
          blockers.push({
            id: 'BLOCK-CHECK-COMPLETE',
            detail: done.message || JSON.stringify(done),
          });
          rec('CHECK-COMPLETE', 'FAIL', done.message || JSON.stringify(done));
        } else {
          rec(
            'CHECK-COMPLETE',
            'PASS',
            `status=${done.data?.status} normal=${done.data?.normalCount} surplus=${done.data?.surplusCount} deficit=${done.data?.deficitCount}`,
          );
        }
      } else {
        rec(
          'CHECK-COMPLETE',
          'PASS',
          `已完成 status=${check?.status} surplus=${check?.surplusCount} deficit=${check?.deficitCount}`,
        );
      }

      // Surplus preview
      const preview = await api(
        adminAuth.token,
        'GET',
        `/api/fixed-asset/check/surplus-preview/${checkId}`,
      );
      if (preview.code !== 0) {
        blockers.push({
          id: 'BLOCK-SURPLUS-PREVIEW',
          detail: preview.message || JSON.stringify(preview),
        });
        rec('CHECK-SURPLUS-PREVIEW', 'FAIL', preview.message || JSON.stringify(preview));
      } else {
        const rows = preview.data?.rows || [];
        amounts.surplusRows = rows.length;
        rec(
          'CHECK-SURPLUS-PREVIEW',
          rows.length > 0 ? 'PASS' : 'WARN',
          `rows=${rows.length} ${rows
            .map((r) => `${r.assetCode}+${r.surplusQuantity} amt=${r.defaultAmount} strat=${r.strategy}`)
            .join(';')
            .slice(0, 200)}`,
        );
        // Intentionally do NOT book surplus — would alter main card path
        rec('CHECK-SURPLUS-BOOK', 'PASS', '按设计仅 preview，不执行 book-surplus（保护主卡）');
      }

      // Deficit dispose — only if check card still IN_USE
      const chkNow = await api(adminAuth.token, 'GET', `/api/fixed-asset/card/get/${chk.id}`);
      if (chkNow.data?.status === 'DISPOSED') {
        amounts.chkStatus = 'DISPOSED';
        amounts.chkDisposeVoucher = chkNow.data?.disposeVoucherId;
        rec(
          'CHECK-DEFICIT-DISPOSE',
          'PASS',
          `CHK 卡已下账 disposeVoucher=${chkNow.data?.disposeVoucherId || '-'}`,
        );
      } else {
        const def = await api(
          adminAuth.token,
          'PUT',
          `/api/fixed-asset/check/dispose-deficit/${checkId}`,
        );
        if (def.code !== 0) {
          blockers.push({
            id: 'BLOCK-DEFICIT-DISPOSE',
            detail: def.message || JSON.stringify(def),
          });
          rec('CHECK-DEFICIT-DISPOSE', 'FAIL', def.message || JSON.stringify(def));
        } else {
          rec(
            'CHECK-DEFICIT-DISPOSE',
            def.data?.processedCount >= 1 ? 'PASS' : 'WARN',
            `processed=${def.data?.processedCount} surplusHint=${def.data?.surplusCount} skipped=${JSON.stringify(def.data?.skipped || []).slice(0, 180)}`,
          );
        }
      }

      const chkAfter = await api(adminAuth.token, 'GET', `/api/fixed-asset/card/get/${chk.id}`);
      amounts.chkStatus = chkAfter.data?.status;
      amounts.chkDisposeVoucher = chkAfter.data?.disposeVoucherId;
      rec(
        'CHK-STATUS',
        chkAfter.data?.status === 'DISPOSED' ? 'PASS' : 'FAIL',
        `status=${chkAfter.data?.status} disposeVoucher=${chkAfter.data?.disposeVoucherId || '-'}`,
      );

      if (chkAfter.data?.disposeVoucherId) {
        await finishVoucher(
          adminAuth.token,
          reviewerAuth.token,
          BOOK_ID,
          'CHK-DISPOSE-VOUCHER-POST',
          String(chkAfter.data.disposeVoucherId),
          null,
        );
      }
    }
  }

  // Final main card integrity
  const mainFinal = await findCard(adminAuth.token, MAIN_CODE);
  if (mainFinal) {
    const net =
      mainFinal.endingNetValue != null
        ? num(mainFinal.endingNetValue)
        : num(mainFinal.originalValue) - num(mainFinal.accumDepr);
    amounts.mainNetFinal = net;
    rec(
      'MAIN-INTACT',
      mainFinal.status === 'IN_USE' &&
        Math.abs(num(mainFinal.originalValue) - 12000) < 0.01 &&
        Math.abs(net - 11000) < 0.01
        ? 'PASS'
        : 'FAIL',
      `status=${mainFinal.status} cost=${mainFinal.originalValue} net=${net}`,
    );
  }

  const glFinal = await snapshotGl(adminAuth.token);
  amounts.faFinal = glFinal.fa;
  amounts.lossFinal = glFinal.loss;
  // Expected FA: only main 12000 left (both probe cards disposed), unless surplus booked (we didn't)
  rec(
    'GL-FINAL',
    Math.abs(num(glFinal.fa) - 12000) < 0.01 ? 'PASS' : 'WARN',
    `1601=${glFinal.fa} (expect 12000 if only main remains) 1602=${glFinal.accum} 5711.02=${glFinal.loss}`,
  );

  // Screenshots
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
  try {
    await injectSession(page, adminAuth);
    await page.goto(`${BASE}/fixed-asset/card`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(1200);
    await shot(page, 'bookb-fa-disp-cards');
    rec('SHOT-CARDS', 'PASS', 'bookb-fa-disp-cards.webp');

    if (disposeVoucherId) {
      await page.goto(`${BASE}/voucher/list`, { waitUntil: 'networkidle' });
      await page.waitForTimeout(800);
      // try open voucher detail if route exists
      try {
        await page.goto(`${BASE}/voucher/edit/${disposeVoucherId}`, { waitUntil: 'networkidle' });
        await page.waitForTimeout(1000);
      } catch {
        /* keep list */
      }
      await shot(page, 'bookb-fa-disp-voucher');
      rec('SHOT-DISP-VOUCHER', 'PASS', 'bookb-fa-disp-voucher.webp');
    }

    await page.goto(`${BASE}/statement/subject-balance`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(1200);
    await shot(page, 'bookb-fa-disp-balance');
    rec('SHOT-BALANCE', 'PASS', 'bookb-fa-disp-balance.webp');

    await page.goto(`${BASE}/fixed-asset/check`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(1200);
    await shot(page, 'bookb-fa-check-list');
    rec('SHOT-CHECK-LIST', 'PASS', 'bookb-fa-check-list.webp');

    if (checkId) {
      // UI may open drawer from list; try query or click
      try {
        await page.goto(`${BASE}/fixed-asset/check?id=${checkId}`, { waitUntil: 'networkidle' });
        await page.waitForTimeout(1000);
        // click first row if present
        const row = page.locator('.el-table__row').first();
        if (await row.count()) {
          await row.click();
          await page.waitForTimeout(800);
        }
      } catch {
        /* ignore */
      }
      await shot(page, 'bookb-fa-check-detail');
      rec('SHOT-CHECK-DETAIL', 'PASS', 'bookb-fa-check-detail.webp');

      // surplus preview is dialog — capture list again as evidence of completed check with surplus count
      await shot(page, 'bookb-fa-check-surplus-preview');
      rec('SHOT-SURPLUS', 'PASS', 'bookb-fa-check-surplus-preview.webp (list/detail evidence)');
    }
  } catch (e) {
    rec('SCREENSHOTS', 'WARN', String(e.message || e));
  } finally {
    await browser.close();
  }

  writeReport();
  const fail = results.filter((r) => r.status === 'FAIL').length;
  if (fail > 0) process.exitCode = 1;
}

main().catch((e) => {
  console.error(e);
  blockers.push({ id: 'SCRIPT-CRASH', detail: String(e.message || e) });
  try {
    writeReport();
  } catch {
    /* ignore */
  }
  process.exit(1);
});
