#!/usr/bin/env node
/**
 * API smoke probe runner (cross-platform).
 * Usage: node tools/smoke-api.mjs [--base http://localhost:2154]
 */
const base = (process.argv.find((a, i) => process.argv[i - 1] === '--base') || 'http://localhost:2154').replace(/\/$/, '')

const username = process.env.E2E_USERNAME || 'admin'
const password = process.env.E2E_PASSWORD || 'maxkey'

async function login() {
    const initRes = await fetch(`${base}/api/login/get?_allow_anonymous=true`)
    const init = await initRes.json()
    const signinRes = await fetch(`${base}/api/login/signin?_allow_anonymous=true`, {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({
            username,
            password,
            captcha: '',
            state: init.data.state,
            authType: 'normal',
        }),
    })
    const signin = await signinRes.json()
    if (signin.code !== 0) {
        throw new Error(`login failed: ${JSON.stringify(signin)}`)
    }
    return {Authorization: `Bearer ${signin.data.token}`}
}

async function probe(name, path, headers) {
    const res = await fetch(`${base}${path}`, {headers})
    const body = await res.json().catch(() => ({}))
    const ok = res.ok && body.code === 0
    console.log(`${ok ? 'OK' : 'FAIL'} ${name}`)
    if (!ok) {
        console.log(`  ${path} -> ${res.status} code=${body.code}`)
    }
    return ok
}

async function main() {
    const headers = await login()
    const userRes = await fetch(`${base}/api/users/currentUser`, {headers})
    const user = (await userRes.json()).data
    const bookId = user.bookId

    const checks = [
        ['book list', `/api/book/fetch?pageNumber=1&pageSize=10`],
        ['book subject', `/api/booksubject/fetch?bookId=${bookId}&pageNumber=1&pageSize=10`],
        ['journal entry', `/api/journal/entry/fetch?pageNumber=1&pageSize=10`],
        ['employee', `/api/salary/employee/fetch?pageNumber=1&pageSize=10`],
        ['settlement', `/api/settlement/fetch?pageNumber=1&pageSize=10`],
        ['balance sheet', `/api/statement/balance-sheet?periodType=month&reportDate=2025-03`],
        ['fund balance', `/api/statistics/fund-balance?periodType=month&reportDate=2025-03`],
    ]

    let passed = 0
    for (const [name, path] of checks) {
        if (await probe(name, path, headers)) {
            passed++
        }
    }
    console.log(`\n${passed}/${checks.length} passed`)
    process.exit(passed === checks.length ? 0 : 1)
}

main().catch((err) => {
    console.error(err)
    process.exit(1)
})
