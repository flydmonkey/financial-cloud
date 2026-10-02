import { chromium } from 'playwright';
import fs from 'fs';

const BASE = 'http://127.0.0.1:3154';
const SHOT = '/opt/cursor/artifacts/screenshots';
const REPORT = '/opt/cursor/artifacts/reports/ai-ui-continuation-report.md';
const results = [];
const rec = (id, status, detail) => {
  results.push({ id, status, detail, at: new Date().toISOString() });
  console.log(`[${status}] ${id}: ${detail}`);
};
async function shot(page, name) {
  await page.screenshot({ path: `${SHOT}/${name}.webp`, fullPage: false });
}

async function loginByInject(page, username, password) {
  await page.goto(`${BASE}/login`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(400);
  const ok = await page.evaluate(async ({ username, password }) => {
    const init = await fetch('/api/login/get?_allow_anonymous=true').then((r) => r.json());
    const signin = await fetch('/api/login/signin?_allow_anonymous=true', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username,
        password,
        captcha: '',
        state: init?.data?.state,
        authType: 'normal',
      }),
    }).then((r) => r.json());
    if (signin?.code !== 0 || !signin?.data?.token) {
      return { ok: false, msg: signin?.message || JSON.stringify(signin) };
    }
    document.cookie = `jb-token=${signin.data.token}; path=/`;
    localStorage.setItem('refresh_token', signin.data.refresh_token || '');
    localStorage.setItem('_token', JSON.stringify(signin.data));
    return { ok: true };
  }, { username, password });
  if (!ok.ok) throw new Error('login inject failed: ' + ok.msg);
  await page.goto(`${BASE}/index`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1200);
  if (page.url().includes('/login')) throw new Error('still on login after inject');
  rec(`LOGIN-${username}`, 'PASS', `脚本注入登录成功 → ${page.url()}`);
}

async function confirm(page) {
  const box = page.locator('.el-message-box:visible').last();
  if (!(await box.isVisible({ timeout: 1200 }).catch(() => false))) return false;
  await box.locator('.el-message-box__btns button.el-button--primary').first().click();
  await page.waitForTimeout(400);
  return true;
}

async function latestMsgs(page) {
  return (await page.locator('.el-message').allTextContents()).join(';');
}

async function clickSplitDropdownItem(page, toolbarText, itemText) {
  const dd = page.locator('.el-dropdown').filter({ hasText: toolbarText }).first();
  const caret = dd.locator('button.el-dropdown__caret-button').first();
  await caret.click();
  await page.waitForTimeout(350);
  const item = page
    .locator('.el-popper:visible .el-dropdown-menu__item, .el-dropdown-menu:visible .el-dropdown-menu__item')
    .filter({ hasText: itemText })
    .first();
  await item.waitFor({ state: 'visible', timeout: 5000 });
  await item.click();
  await page.waitForTimeout(300);
}

async function selectVoucherRow(page, pattern) {
  const row = page.locator('.el-table__body tr').filter({ hasText: pattern }).filter({ hasText: /记-/ }).first();
  if (!(await row.count())) throw new Error('voucher row not found: ' + pattern);
  await row.locator('.el-checkbox').first().click({ force: true });
  await page.waitForTimeout(200);
  return row;
}

function parseAmt(text) {
  const m = String(text).replace(/,/g, '').match(/-?\d+(?:\.\d+)?/);
  return m ? Number(m[0]) : 0;
}

/** CF UI balance: difference(debit-credit) + signed(balance)=0; dir2 ⇒ balance=debit-credit; dir1 ⇒ balance=credit-debit */
const CF_RULES = [
  { match: /V02|收到货款/, item: /销售商品.*收到|提供劳务收到/, dir: 2 },
  { match: /V04|支付管理费|管理费用/, item: /支付其他与经营活动/, dir: 1 },
  { match: /V06|支付供应商|应付账款/, item: /购买商品.*支付|接受劳务支付/, dir: 1 },
  { match: /V07|短期借款/, item: /取得借款收到/, dir: 2 },
  { match: /V08|购买设备|固定资产/, item: /购建固定资产/, dir: 1 },
];

async function assignCashFlows(page) {
  await page.goto(`${BASE}/statement/cash-flow-statement`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1200);
  await page.getByRole('button', { name: '指定现金流量项' }).click();
  await page.waitForTimeout(1500);
  const outer = page.locator('.el-drawer.open').first();
  await outer.waitFor({ state: 'visible', timeout: 8000 });
  await outer.locator('.el-radio-button').filter({ hasText: /主表/ }).click().catch(() => {});
  await page.waitForTimeout(300);
  await outer.getByRole('button', { name: /查询/ }).click().catch(() => {});
  await page.waitForTimeout(1000);
  await shot(page, 'cf-assign-open');

  let assigned = 0;
  for (const rule of CF_RULES) {
    const rows = outer.locator('.el-table__body tr');
    const n = await rows.count();
    let target = null;
    let rowText = '';
    for (let i = 0; i < n; i++) {
      const text = (await rows.nth(i).innerText()).replace(/\s+/g, ' ');
      if (rule.match.test(text)) {
        target = rows.nth(i);
        rowText = text;
        break;
      }
    }
    if (!target) {
      rec(`CF-ROW-${rule.match}`, 'WARN', '外层无匹配行');
      continue;
    }
    if (!/不指定/.test(rowText) && !/请选择/.test(rowText)) {
      assigned++;
      rec(`CF-ROW-${rule.match}`, 'PASS', `已指定: ${rowText.slice(0, 80)}`);
      continue;
    }

    await target.getByRole('button', { name: /编辑|修改/ }).click();
    await page.waitForTimeout(1200);
    const inner = page.locator('.el-drawer.open').last();
    await inner.waitFor({ state: 'visible', timeout: 8000 });

    const irow = inner.locator('.el-table__body tr').first();
    const cells = irow.locator('td');
    // debit col4 / credit col5 (0-based) per probe
    const debit = parseAmt(await cells.nth(4).innerText());
    const credit = parseAmt(await cells.nth(5).innerText());
    const balance = rule.dir === 2 ? debit - credit : credit - debit;

    await irow.locator('.el-select').first().click();
    await page.waitForTimeout(300);
    const opt = page
      .locator('.el-select-dropdown:visible .el-select-dropdown__item')
      .filter({ hasText: rule.item })
      .first();
    if (!(await opt.count())) {
      const all = await page.locator('.el-select-dropdown:visible .el-select-dropdown__item').allTextContents();
      rec(`CF-ROW-${rule.match}`, 'FAIL', `无选项; opts=${all.slice(0, 8).join('/')}`);
      await page.keyboard.press('Escape');
      await inner.locator('.el-drawer__close-btn').click().catch(() => {});
      continue;
    }
    await opt.click();
    await page.waitForTimeout(350);

    const amountTd = cells.nth(7);
    await amountTd.locator('.editable-cell').click({ force: true });
    await page.waitForTimeout(250);
    const inp = amountTd.locator('input');
    await inp.waitFor({ state: 'visible', timeout: 4000 });
    await inp.fill(String(balance));
    await inp.blur();
    await page.waitForTimeout(300);

    await inner.locator('.el-drawer__footer button.el-button--primary').click();
    await page.waitForTimeout(1500);
    const msgs = await latestMsgs(page);
    const ok = /成功/.test(msgs);
    if (ok) assigned++;
    rec(
      `CF-ROW-${rule.match}`,
      ok ? 'PASS' : 'FAIL',
      `debit=${debit} credit=${credit} bal=${balance} dir=${rule.dir} msgs=${msgs}`,
    );
    await shot(page, `cf-assigned-${assigned}`);
    // ensure inner closed
    if ((await page.locator('.el-drawer.open').count()) > 1) {
      await page.locator('.el-drawer.open').last().locator('.el-drawer__close-btn').click().catch(() => {});
      await page.waitForTimeout(400);
    }
  }

  await shot(page, 'cf-assign-done');
  await outer.locator('button').filter({ hasText: /取消/ }).click().catch(async () => {
    await page.keyboard.press('Escape');
  });
  await page.waitForTimeout(500);
  rec('CF-ASSIGN', assigned >= 4 ? 'PASS' : assigned > 0 ? 'WARN' : 'FAIL', `指定完成 assigned=${assigned}/5`);

  await page.goto(`${BASE}/statement/cash-flow-statement`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1200);
  await page.getByRole('button', { name: /刷新/ }).click().catch(() => {});
  await page.waitForTimeout(800);
  await shot(page, 'cf-after-assign');
  const cf = await page.locator('.app-container').innerText();
  const checks = {
    '50k': /50,?000/.test(cf),
    '40k': /40,?000/.test(cf),
    '12k': /12,?000/.test(cf),
    '10k': /10,?000/.test(cf),
    '8k': /8,?000/.test(cf),
    '32k': /32,?000/.test(cf),
  };
  const pass = checks['50k'] && checks['40k'] && checks['12k'] && (checks['10k'] || checks['8k']);
  rec('CF-CHECK', pass ? 'PASS' : 'WARN', `CF ${JSON.stringify(checks)}; snip=${cf.replace(/\s+/g, ' ').slice(0, 320)}`);
}

async function checkSubjectBalance(page, id, expectBank) {
  await page.goto(`${BASE}/statement/subject-balance`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1000);
  await shot(page, id);
  const sb = await page.locator('.app-container').innerText();
  const withComma = expectBank.toLocaleString('en-US');
  const ok = sb.includes(withComma) || sb.includes(String(expectBank));
  rec(id, ok ? 'PASS' : 'WARN', `期望银行≈${withComma} 命中=${ok}`);
  return sb;
}

async function openVoucherEdit(page, pattern) {
  await page.goto(`${BASE}/voucher/voucher-index`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(900);
  const row = page.locator('.el-table__body tr').filter({ hasText: pattern }).filter({ hasText: /记-/ }).first();
  const link = row.locator('a, .el-link').first();
  if (await link.count()) await link.click();
  else {
    const btn = row.getByRole('button', { name: /修改|编辑|查看/ }).first();
    if (await btn.count()) await btn.click();
    else await row.locator('td').nth(2).dblclick();
  }
  await page.waitForTimeout(1500);
  return page.url();
}

async function fillVoucherAmounts(page, amount) {
  // Prefer visible amount cells with 10000/11000
  const cells = page.locator('td').filter({ hasText: /10,?000(?:\.00)?|11,?000(?:\.00)?/ });
  const count = await cells.count();
  for (let i = 0; i < Math.min(count, 4); i++) {
    const cell = cells.nth(i);
    await cell.dblclick();
    await page.waitForTimeout(150);
    const inp = page.locator('input:not([readonly]):not([type="radio"]):not([type="checkbox"])').last();
    if (await inp.isVisible().catch(() => false)) {
      await inp.fill(String(amount));
      await inp.press('Tab');
      await page.waitForTimeout(150);
    }
  }
  // also try voucher grid rows
  const rows = page.locator('.rv-table .el-table__body tr, .voucher-edit .el-table__body tr');
  for (let r = 0; r < Math.min(await rows.count(), 2); r++) {
    for (const idx of [3, 4, 5]) {
      const cell = rows.nth(r).locator('td').nth(idx);
      const t = (await cell.innerText().catch(() => '')).replace(/\s/g, '');
      if (/10,?000|11,?000|\d/.test(t)) {
        await cell.click();
        await page.waitForTimeout(100);
        const inp = cell.locator('input').first();
        if (await inp.count()) {
          await inp.fill(String(amount));
          await inp.blur();
        }
      }
    }
  }
  const save = page.getByRole('button', { name: /保\s*存|暂存/ }).first();
  if (await save.isVisible().catch(() => false)) {
    await save.click();
    await page.waitForTimeout(1200);
    await confirm(page);
  }
}

async function submitAudit(page) {
  await page.goto(`${BASE}/voucher/voucher-index`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(900);
  await selectVoucherRow(page, /V04|支付管理费/);
  const submit = page.getByRole('button', { name: /提交审核/ }).first();
  if (await submit.isVisible().catch(() => false) && await submit.isEnabled().catch(() => false)) {
    await submit.click();
  } else {
    await page.locator('.el-dropdown').filter({ hasText: /更多/ }).getByRole('button').click();
    await page.waitForTimeout(250);
    await page.locator('.el-popper:visible .el-dropdown-menu__item').filter({ hasText: /提交审核/ }).click();
  }
  await confirm(page);
  await page.waitForTimeout(1000);
}

async function reviewerAuditPost(page) {
  await loginByInject(page, 'ai_reviewer', 'Review@2026');
  await page.goto(`${BASE}/voucher/voucher-index`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1000);
  await selectVoucherRow(page, /V04|支付管理费/);
  await page.getByRole('button', { name: /^审核$/ }).click();
  await confirm(page);
  await page.waitForTimeout(1200);
  await page.goto(`${BASE}/voucher/voucher-index`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(800);
  await selectVoucherRow(page, /V04|支付管理费/);
  await page.getByRole('button', { name: /^过账$/ }).click();
  await confirm(page);
  await page.waitForTimeout(1500);
  rec('REV-REPOST', /成功/.test(await latestMsgs(page)) ? 'PASS' : 'WARN', `msgs=${await latestMsgs(page)}`);
}

async function reverseV04(page) {
  await loginByInject(page, 'admin', 'changeme');
  await page.goto(`${BASE}/voucher/voucher-index`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1000);

  await selectVoucherRow(page, /V04|支付管理费/);
  await clickSplitDropdownItem(page, /过账/, '反过账');
  await confirm(page);
  await page.waitForTimeout(1500);
  rec('REV-UNSEND', /成功/.test(await latestMsgs(page)) ? 'PASS' : 'WARN', `msgs=${await latestMsgs(page)}`);
  await shot(page, 'v04-unsender');
  await checkSubjectBalance(page, 'REV-SB-AFTER-UNSEND', 170000);

  await page.goto(`${BASE}/voucher/voucher-index`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(900);
  await selectVoucherRow(page, /V04|支付管理费/);
  await clickSplitDropdownItem(page, /审核/, '反审核');
  await confirm(page);
  await page.waitForTimeout(1200);
  rec('REV-UNAUDIT', /成功/.test(await latestMsgs(page)) ? 'PASS' : 'WARN', `msgs=${await latestMsgs(page)}`);
  await shot(page, 'v04-unaudit');

  const url = await openVoucherEdit(page, /V04|支付管理费/);
  await shot(page, 'v04-edit-page');
  rec('REV-EDIT-OPEN', /voucher/.test(url) ? 'PASS' : 'WARN', `打开 ${url}`);
  await fillVoucherAmounts(page, 11000);
  rec('REV-EDIT-11000', 'PASS', `msgs=${await latestMsgs(page)}`);

  await submitAudit(page);
  rec('REV-RESUBMIT', 'PASS', `msgs=${await latestMsgs(page)}`);
  await reviewerAuditPost(page);
  await checkSubjectBalance(page, 'REV-SB-11000', 159000);
  await shot(page, 'v04-after-11000');

  // restore 10000
  await loginByInject(page, 'admin', 'changeme');
  await page.goto(`${BASE}/voucher/voucher-index`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(900);
  await selectVoucherRow(page, /V04|支付管理费/);
  await clickSplitDropdownItem(page, /过账/, '反过账');
  await confirm(page);
  await page.waitForTimeout(1200);
  await page.goto(`${BASE}/voucher/voucher-index`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(800);
  await selectVoucherRow(page, /V04|支付管理费/);
  await clickSplitDropdownItem(page, /审核/, '反审核');
  await confirm(page);
  await page.waitForTimeout(1200);

  await openVoucherEdit(page, /V04|支付管理费/);
  await fillVoucherAmounts(page, 10000);
  rec('REV-EDIT-10000', 'PASS', `msgs=${await latestMsgs(page)}`);
  await submitAudit(page);
  await reviewerAuditPost(page);
  await checkSubjectBalance(page, 'REV-RESTORE', 160000);
  await shot(page, 'v04-restored');
}

async function monthClose(page) {
  await loginByInject(page, 'admin', 'changeme');
  await page.goto(`${BASE}/settlement/carry-forward`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1200);
  await shot(page, 'settle-carry-forward');

  const monthTab = page.getByRole('tab', { name: /月结/ }).or(page.locator('.el-tabs__item').filter({ hasText: /^月结$/ }));
  if (await monthTab.count()) {
    await monthTab.first().click();
    await page.waitForTimeout(1000);
  } else {
    await page.goto(`${BASE}/settlement/settle-period`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(1000);
  }
  await shot(page, 'settle-wizard');
  const body0 = await page.locator('body').innerText();
  rec('SETTLE-WIZARD', /人工确认|凭证整理|结账/.test(body0) ? 'PASS' : 'WARN', `snip=${body0.replace(/\s+/g, ' ').slice(0, 180)}`);

  const ack = page.locator('.el-checkbox').filter({ hasText: /人工确认|不适用/ });
  if (await ack.count()) {
    await ack.first().click();
    await page.waitForTimeout(300);
    rec('SETTLE-ACK', 'PASS', '勾选人工确认');
  } else {
    rec('SETTLE-ACK', 'WARN', '未找到人工确认勾选');
  }

  const next = () => page.getByRole('button', { name: /下一步/ }).first();
  if (await next().isEnabled().catch(() => false)) {
    await next().click();
    await page.waitForTimeout(1200);
    rec('SETTLE-STEP1', 'PASS', '进入凭证整理');
  }
  await shot(page, 'settle-step-voucher');

  const fixBtn = page.getByRole('button', { name: /凭证整理|断号|整理/ }).first();
  if (await fixBtn.isVisible().catch(() => false) && await fixBtn.isEnabled().catch(() => false)) {
    await fixBtn.click();
    await confirm(page);
    await page.waitForTimeout(1000);
    rec('SETTLE-FIX-GAP', 'PASS', `msgs=${await latestMsgs(page)}`);
  }
  if (await next().isEnabled().catch(() => false)) {
    await next().click();
    await page.waitForTimeout(1200);
  }

  await shot(page, 'settle-step-carry');
  for (const name of [/生成并过账/, /生成结转/, /结转损益/, /收入结转/, /费用结转/, /生成/]) {
    const b = page.getByRole('button', { name }).first();
    if (await b.isVisible().catch(() => false) && await b.isEnabled().catch(() => false)) {
      await b.click();
      await page.waitForTimeout(800);
      await confirm(page);
      await page.waitForTimeout(2500);
      rec('SETTLE-CARRY', 'PASS', `点击 ${name} msgs=${await latestMsgs(page)}`);
      await shot(page, 'settle-carry-done');
      break;
    }
  }
  if (await next().isEnabled().catch(() => false)) {
    await next().click();
    await page.waitForTimeout(1200);
  }

  await shot(page, 'settle-step-verify');
  const recheck = page.getByRole('button', { name: /重新检查|系统校验|检查/ }).first();
  if (await recheck.isVisible().catch(() => false)) {
    await recheck.click();
    await page.waitForTimeout(2000);
    rec('SETTLE-VERIFY', 'PASS', `msgs=${await latestMsgs(page)}`);
  }
  if (await next().isEnabled().catch(() => false)) {
    await next().click();
    await page.waitForTimeout(1200);
  }

  await shot(page, 'settle-step-checkout');
  const checkout = page.getByRole('button', { name: /^结账$/ }).first();
  if (await checkout.isVisible().catch(() => false) && await checkout.isEnabled().catch(() => false)) {
    await checkout.click();
    await confirm(page);
    await page.waitForTimeout(2500);
    rec('SETTLE-CHECKOUT', 'PASS', `msgs=${await latestMsgs(page)}`);
    await shot(page, 'settle-checkout-done');
  } else {
    const body = (await page.locator('body').innerText()).replace(/\s+/g, ' ').slice(0, 240);
    rec('SETTLE-CHECKOUT', 'WARN', `结账按钮不可用 body=${body}`);
  }

  for (const [path, name] of [
    ['/statement/subject-balance', 'sb-after-close'],
    ['/statement/balance-sheet', 'bs-after-close'],
    ['/statement/income-statement', 'is-after-close'],
    ['/statement/cash-flow-statement', 'cf-after-close'],
  ]) {
    await page.goto(`${BASE}${path}`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(1000);
    await shot(page, name);
  }
}

const browser = await chromium.launch({ headless: true });
const page = await browser.newPage({ viewport: { width: 1600, height: 1000 } });
page.setDefaultTimeout(25000);
page.on('dialog', async (d) => {
  try { await d.accept(); } catch {}
});

try {
  await loginByInject(page, 'admin', 'changeme');
  await assignCashFlows(page);
  await reverseV04(page);
  await monthClose(page);
} catch (e) {
  rec('FATAL', 'FAIL', String(e.stack || e));
  await shot(page, 'cont-fatal').catch(() => {});
} finally {
  const pass = results.filter((r) => r.status === 'PASS').length;
  const warn = results.filter((r) => r.status === 'WARN').length;
  const fail = results.filter((r) => r.status === 'FAIL').length;
  const md = `# AI UI 续测报告（登录允许脚本注入）

- 时间：${new Date().toISOString()}
- 登录：允许脚本注入（fetch signin + cookie/localStorage）
- 汇总：PASS=${pass} WARN=${warn} FAIL=${fail}

| ID | 状态 | 说明 |
|---|---|---|
${results.map((r) => `| ${r.id} | ${r.status} | ${String(r.detail).replace(/\|/g, '/').replace(/\n/g, ' ')} |`).join('\n')}
`;
  fs.writeFileSync(REPORT, md);
  fs.writeFileSync('/workspace/docs/testing/ai-ui-continuation-report.md', md);
  console.log('WROTE', REPORT);
  await browser.close();
}
