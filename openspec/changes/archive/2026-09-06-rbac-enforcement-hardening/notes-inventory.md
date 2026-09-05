# Legacy SUPERVISOR / 1000 inventory (task 2.1)

## Backend
| Location | Before | After (this change) |
|----------|--------|---------------------|
| `ProductRoles.java` WRITE_VOUCHER / APPROVE_OR_CLOSE | included `ROLE_SUPERVISOR`, `1000` | removed; four product roles only |
| `ProductRoles.isAdministrator()` | admin + SUPERVISOR + 1000 | `ROLE_ADMINISTRATORS` only |
| `AuthzService.grantAuthority` | still **emits** `ROLE_SUPERVISOR` when role category is supervisor | kept for session plumbing; **not** used as product bypass |
| Admin `roles.role_code` often `1000` | still may appear as authority string | not in ProductRoles allow-lists |

## Frontend
| Location | Before | After |
|----------|--------|-------|
| `directive/permission/hasRole.ts` | bypass: admin, ROLE_ADMINISTRATORS, ROLE_SUPERVISOR, 1000 | only `ROLE_ADMINISTRATORS` |
| `plugins/auth.ts` authRole | same | only `ROLE_ADMINISTRATORS` |
| `views/voucher/voucher-index.vue` v-hasRole | included ROLE_SUPERVISOR | product roles only |

## Module API guard checklist (task 4.1)

| Module | Write guard | Who may write |
|--------|-------------|---------------|
| Voucher | `requireWriteVoucher` / `requireApproveVoucher` | admin/bookkeeper/reviewer; approve: admin/reviewer |
| Settlement close | `requireClosePeriod` | admin/reviewer |
| Journal | `requireWriteBusiness` | admin/bookkeeper/reviewer |
| Fixed asset | `requireWriteBusiness` | admin/bookkeeper/reviewer |
| Payroll salary | `requireWriteBusiness` | admin/bookkeeper/reviewer |
| ARAP writeoff | `requireWriteBusiness` | admin/bookkeeper/reviewer |
| Assist acc | `requireWriteBusiness` | admin/bookkeeper/reviewer |
| Standards (准则) | `requireAdministrator` | admin |
| Roles (角色管理) | `requireAdministrator` | admin |
| Book update/delete/members | `BookService.requireBookAdministrator` | book admin |

Viewer: menus for voucher/ledger/report/ARAP read; mutations denied.
