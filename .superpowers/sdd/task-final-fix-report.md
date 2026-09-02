# Phase 3 Final Review Fix Report

## Changes

- Disabled Spring Security logout handling so `/logout` remains under `PermissionInterceptor`.
- Raised `PageQuery.MAX_PAGE_SIZE` to `100000` for intentional reference-data full fetches and updated its clamp regression test.
- Replaced per-voucher lookups with `selectVoBatchIds`, then restored requested ID order before building the `LinkedHashMap`.
- Injected the Spring-managed `JsonMapper` into the 401/403 writers, removed the unused management flag, and refreshed their Javadocs.
- Added Apache license headers to the API v1 path rewrite filter and auto-configuration.

## Commits

- `0c6faad` — `fix: address final review auth and query findings`

## Verification

- RED: `PageQueryTest` failed as expected (`expected: <100000> but was: <100>`).
- GREEN: `PageQueryTest` passed (3 tests, 0 failures).
- Focused suite (direct Maven Wrapper launcher equivalent to the requested `mvnw.cmd` command):
  - `PageQueryTest,MessageAuthCodesTest,ApiV1PathRewriteFilterTest,BusinessExceptionCodeRangesTest`
  - Result: `BUILD SUCCESS`; 9 tests, 0 failures, 0 errors, 0 skipped.
- Compile with tests skipped (direct Maven Wrapper launcher equivalent):
  - Result: `BUILD SUCCESS`.
- IDE diagnostics: no linter errors in changed files.

## Remaining Concerns

- The first `mvnw.cmd` attempt under the configured JDK 21 printed Java usage and exited 1. Prior-agent-compatible direct wrapper invocation with `C:\Program Files\Java\jdk-17\bin\java.exe` completed both required verifications successfully.
- Maven reports pre-existing duplicate `sonatype_releases` and `sonatype_snapshots` server IDs in the user settings.
- Optional MySQL smoke test was not run.

---

## Ledger Books Menu — Final Review Fixes (2026-08-28)

### I-1: Icon column → `res_style`

Frontend reads `menu.resStyle` (`financial-cloud-ui/src/api/menu.ts`), not `icon`.

**Files changed:**

1. `sql/seed/ledger_books_menu.sql` — moved `'menus-kemuyuebiao'` from `icon` to `res_style`; `icon` is `NULL`.
2. `sql/seed/general_ledger_menu.sql` — same pattern.
3. `tools/apply_ledger_books_menu.py` — `verify()` now asserts ledger parent `icon IS NULL` and `res_style = 'menus-kemuyuebiao'`.

### I-2: README order

`sql/seed/README.md` — removed conflicting guidance to run `apply_general_ledger_menu.py` first when 总账 is missing. Single recommended order: `ledger_books_menu` first, then `general_ledger_menu` as needed.

### Apply + verify commands

```powershell
cd C:\Users\Administrator\Projects\jinbooks
python tools/apply_ledger_books_menu.py
python tools/apply_general_ledger_menu.py
python tools/apply_ledger_books_menu.py
```

**Exit codes:** all `0`.

**Output:**

```
OK applied+verified ledger_books_menu.sql as jinbooks (twice)
OK applied general_ledger_menu.sql as jinbooks
resources: (('2026082816300000001', '总账', '/statement/general-ledger', '2026082817000000001', 2, 'y'),)
permission: (('2026082816300000002', 'ROLE_ADMINISTRATORS', '2026082816300000001'),)
OK applied+verified ledger_books_menu.sql as jinbooks (twice)
```

**DB SELECT** (`resources` for 账簿 + 总账):

```sql
SELECT id, res_name, icon, res_style
FROM resources
WHERE id IN ('2026082817000000001','2026082816300000001')
ORDER BY id;
```

| id | res_name | icon | res_style |
|---|---|---|---|
| 2026082816300000001 | 总账 | NULL | menus-kemuyuebiao |
| 2026082817000000001 | 账簿 | NULL | menus-kemuyuebiao |

Both menus have `res_style` set and `icon` NULL as required.

**Note:** No git commit per instruction.
