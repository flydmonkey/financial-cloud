# 代账工作台 · 多账套月末看板 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship a cross-book「代账工作台」so bookkeepers see authorized books' period/todos/close status, jump into blockers, and batch-export monthly books packs.

**Architecture:** New `BooksBoardService` aggregates per authorized book using existing todo counts + `currentTerm` vs focus-period heuristic. Extend `MonthlyBooksPack` with optional `bookId` (authz) and `export-batch` total ZIP. Vue page + menu seed; no change to single-book home `TodoPanel` or checkout APIs.

**Tech Stack:** Spring Boot + MyBatis-Plus, JUnit 5 + Mockito, Vue 3 + Element Plus, Playwright (minimal), SQL menu patch.

**Spec:** [docs/superpowers/specs/2026-09-29-daizhang-books-board-design.md](../specs/2026-09-29-daizhang-books-board-design.md)

## Global Constraints

- Non-goals: cross-book batch checkout/post, full verify orchestration, SaaS multi-tenant, mobile/AI/tax bureau.
- Hard cap **200** authorized books; set `truncated=true` when more exist.
- Close status = `currentTerm` vs focus period only (`C>F` closed, `==` open, `<` behind).
- Viewer: list OK; export + enter-processing write jumps forbidden (backend reject export).
- Seal books: list + export if closed; no enter-processing.
- Do not weaken period/seal guards; one slice per commit narrative.
- Every functional knife: related unit tests; CI red ⇒ not done.

---

## File map

| File | Responsibility |
|------|----------------|
| `.../util/BooksBoardCloseStatus.java` | Pure heuristic enum + compare |
| `.../dto/workspace/BooksBoardRowVo.java` | One board row |
| `.../dto/workspace/BooksBoardVo.java` | List + truncated + totalGranted |
| `.../dto/workspace/BooksPackBatchRequest.java` | batch body |
| `.../service/workspace/BooksBoardService.java` | Aggregate authorized books |
| `.../controller/workspace/BooksBoardController.java` | `GET /api/workspace/books-board` |
| `.../service/report/DashboardTodoService.java` | Expose reusable count helpers if needed |
| `.../service/statement/MonthlyBooksPackService.java` | `buildBatch` + auth helper hook |
| `.../controller/statement/MonthlyBooksPackController.java` | optional bookId + POST export-batch |
| `sql/patches/2026-09-29-books-board-menu.sql` | Menu + role perms |
| `financial-cloud-ui/src/api/workspace/booksBoard.ts` | API client |
| `financial-cloud-ui/src/views/workspace/books-board.vue` | UI |
| `financial-cloud-ui/e2e/books-board.spec.ts` | Minimal E2E |
| `docs/product/00-overview.md` / `21-roadmap.md` | Point near-term to board |

---

### Task 1: Close-status heuristic (pure)

**Files:**
- Create: `financial-cloud/src/main/java/com/financial/cloud/util/BooksBoardCloseStatus.java`
- Create: `financial-cloud/src/test/java/com/financial/cloud/util/BooksBoardCloseStatusTest.java`

**Interfaces:**
- Produces: `BooksBoardCloseStatus.resolve(String currentTerm, String focusPeriod)` → `CLOSED` | `OPEN` | `BEHIND` | `UNKNOWN`

- [ ] **Step 1: Write failing tests**

```java
@Test
void closedWhenCurrentAfterFocus() {
    assertThat(BooksBoardCloseStatus.resolve("2026-10", "2026-09"))
            .isEqualTo(BooksBoardCloseStatus.CLOSED);
}
@Test
void openWhenEqual() {
    assertThat(BooksBoardCloseStatus.resolve("2026-09", "2026-09"))
            .isEqualTo(BooksBoardCloseStatus.OPEN);
}
@Test
void behindWhenCurrentBeforeFocus() {
    assertThat(BooksBoardCloseStatus.resolve("2026-08", "2026-09"))
            .isEqualTo(BooksBoardCloseStatus.BEHIND);
}
@Test
void unknownOnBlank() {
    assertThat(BooksBoardCloseStatus.resolve(null, "2026-09"))
            .isEqualTo(BooksBoardCloseStatus.UNKNOWN);
}
```

- [ ] **Step 2: Implement enum + YearMonth compare; run tests; commit**

```bash
cd financial-cloud && ./mvnw -q -Dtest=BooksBoardCloseStatusTest test
git add ... && git commit -m "feat(workspace): books-board close-status heuristic"
```

---

### Task 2: BooksBoardService + API

**Files:**
- Create DTOs under `dto/workspace/`
- Create `BooksBoardService` + `BooksBoardController`
- Create `BooksBoardServiceTest`
- Modify `DashboardTodoService` only if extracting shared counters (prefer calling `todo(bookId)` per book for clarity; OK within 200 cap)

**Interfaces:**
- Consumes: `BookService.listBooks(userId)` or `PermissionBook` + `BookMapper`; `DashboardTodoService.todo`; `ConfigSysService.getCurrentTerm`; `BooksBoardCloseStatus`
- Produces: `BooksBoardVo list(String userId, String focusPeriod, boolean onlyTodo, String keyword)`

**Row fields:** `bookId`, `bookName`, `companyName`, `currentTerm`, `voucherReviewed`, `pendingAuditCount`, `pendingPostCount`, `depreciationPending`, `closeStatus` (`CLOSED`/`OPEN`/`BEHIND`/`UNKNOWN`), `bookStatus` (1/0/2), `blocker` (enum/code: `AUDIT`/`POST`/`DEPRECIATION`/`READY_CLOSE`/`READY_PACK`/`BEHIND`/`NONE`), `sealed` boolean

**Default focusPeriod:** if blank → `YearMonth.now().minusMonths(1)` formatted `yyyy-MM`

**onlyTodo:** row has pending audit/post OR depreciation OR closeStatus OPEN/BEHIND

**keyword:** case-insensitive contains on name/companyName

**Truncation:** take first 200 of sorted list (by name); `truncated` if `listBooks.size() > 200`

- [ ] **Step 1: Failing service tests** — empty grants; two books mixed status; onlyTodo filter; keyword; truncate flag with >200 mocked list size (mock list of 201)
- [ ] **Step 2: Implement service + controller `GET /api/workspace/books-board`**
- [ ] **Step 3: Run `BooksBoardServiceTest`; commit**

```bash
./mvnw -q -Dtest=BooksBoardCloseStatusTest,BooksBoardServiceTest test
git commit -m "feat(workspace): books-board aggregate API"
```

---

### Task 3: Books-pack bookId + batch ZIP

**Files:**
- Modify: `MonthlyBooksPackController.java`, `MonthlyBooksPackService.java`
- Create: `BooksPackBatchRequest.java`
- Modify/extend: `MonthlyBooksPackServiceTest.java`
- Auth: resolve target bookId; require user has grant via `permission_book` (count query); reject viewer for export via `ProductRoles.canWriteBusiness()` (or explicit deny VIEWER-only); 403 `BusinessException`

**Interfaces:**
- `GET .../export?yearPeriod&includeVoucherList&bookId?` — if bookId blank use session; else must be granted
- `POST .../export-batch` body `{ bookIds, yearPeriod, includeVoucherList }` → ZIP whose entries are `{safeBookName}/本月账本包_{period}.zip` plus optional `errors.txt`

**Batch rules:**
- Skip/deny ungranted ids into errors.txt
- On build failure for one book: catch, append to errors, continue
- Empty success with only errors: still return ZIP containing errors.txt (HTTP 200) so UI can show message

- [ ] **Step 1: Tests** — authorized bookId builds; unauthorized throws; batch 2 success; batch 1 fail writes errors.txt
- [ ] **Step 2: Implement; run tests; commit**

```bash
./mvnw -q -Dtest=MonthlyBooksPackServiceTest test
git commit -m "feat(statement): books-pack bookId + batch export ZIP"
```

---

### Task 4: Frontend board + menu seed

**Files:**
- Create: `financial-cloud-ui/src/api/workspace/booksBoard.ts`
- Create: `financial-cloud-ui/src/views/workspace/books-board.vue`
- Create: `sql/patches/2026-09-29-books-board-menu.sql` (and wire into `tools/build_init_sql.py` DATA_PATCH or MENU list if required)
- Regenerate or append menu into init via patch (idempotent DELETE+INSERT like tax-estimate)

**Menu:** parent = root `1` or near 首页; path `/workspace/books-board`; roles ADMIN + BOOKKEEPER + REVIEWER + VIEWER (viewer menu OK, UI hides export)

**UI:**
- Header: focus period `el-date-picker` month; onlyTodo switch; keyword input; refresh
- Table columns per spec; selection only when `closeStatus===CLOSED'` for batch
- Actions: 进入处理 (`switchBook` then `location.assign(path)`); 导出 (download); batch bar
- Blocker path map per spec §5.3
- `v-hasRole` hide export for VIEWER; sealed disable 进入处理

- [ ] **Step 1: API + vue page**
- [ ] **Step 2: Menu SQL patch + ensure build_init includes it**
- [ ] **Step 3: Commit**

```bash
git commit -m "feat(ui): 代账工作台 books-board page and menu"
```

---

### Task 5: E2E + product docs

**Files:**
- Create: `financial-cloud-ui/e2e/books-board.spec.ts` (skip-friendly if env lacks 2 books: create second book via API in test helpers if pattern exists)
- Modify: `docs/product/00-overview.md`, `docs/product/21-roadmap.md`
- Update spec status → implementing/completed when done

**E2E minimum:** login → open `/workspace/books-board` → table visible (at least 1 row for default admin). Prefer 2 books if helpers allow.

- [ ] **Step 1: Docs sync + E2E scaffold**
- [ ] **Step 2: Run backend unit tests suite for workspace + books pack; frontend lint/typecheck if feasible**
- [ ] **Step 3: Commit + push + update PR**

```bash
./mvnw -q -Dtest=BooksBoardCloseStatusTest,BooksBoardServiceTest,MonthlyBooksPackServiceTest test
git commit -m "docs: books-board product narrative + e2e scaffold"
git push -u origin cursor/daizhang-books-board-be74
```

---

## Spec coverage checklist

| Spec § | Task |
|--------|------|
| §4.1 board + columns + filters | T2, T4 |
| §4.1 enter processing | T4 |
| §4.1 batch pack | T3, T4 |
| §5.2 heuristic | T1 |
| §6 APIs | T2, T3 |
| §7 viewer/seal | T3, T4 |
| §9 tests | T1–T3, T5 |
| Non-goals | Global Constraints |

## Execution note

User authorized full autonomous execution after spec approval → prefer **inline executing-plans** on branch `cursor/daizhang-books-board-be74` without waiting for approach choice.
