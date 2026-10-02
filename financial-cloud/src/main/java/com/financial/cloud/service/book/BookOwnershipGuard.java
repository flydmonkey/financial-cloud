package com.financial.cloud.service.book;

import com.baomidou.mybatisplus.annotation.TableName;
import com.financial.cloud.authn.support.AuthorizationUtils;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.enums.error.UsersBusinessCode;
import com.financial.cloud.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/** Validates the complete request before controllers perform any mutation.
 * SQL identifiers come exclusively from this server-side allowlist, never from input.
 * This is also usable by services processing records from imports or generated DTOs.
 */
@Component
@RequiredArgsConstructor
public class BookOwnershipGuard {
    private final JdbcTemplate jdbc;

    private static final Set<String> TABLES = Set.of(
            "voucher", "voucher_item", "voucher_item_cash_flow", "voucher_template", "voucher_template_item",
            "fixed_asset", "asset_category", "fixed_asset_check", "fixed_asset_check_item", "fixed_asset_change",
            "fixed_asset_work", "fixed_asset_depr", "fixed_asset_accrual", "employee", "employee_salary",
            "employee_salary_temp", "employee_salary_summary", "employee_tax_deduction", "journal_account", "journal_entry", "journal_summary",
            "journal_reconciliation", "book_subject", "book_init_balance", "assist_acc", "organizations", "config",
            "config_insurance_fund", "config_cash_flow_balance", "config_salary_formula", "statement_balance_sheet_item",
            "statement_income_item", "statement_rules", "statement_cash_flow", "expense_claim", "expense_claim_attachment",
            "arap_writeoff");
    private static final Map<String, String> TYPES = Map.ofEntries(
            Map.entry("VoucherItemChangeDto", "voucher_item"),
            Map.entry("VoucherItemAuxiliaryDto", "assist_acc"),
            Map.entry("ConfigCashFlowItemDto", "config_cash_flow_balance"),
            Map.entry("CashFlowItemDto", "config_cash_flow_balance"),
            Map.entry("StatementRuleChangeDto", "statement_rules"),
            Map.entry("StatementRulesChangeDto", "statement_rules"),
            Map.entry("FixedAssetWorkItemDto", "fixed_asset_work"));

    public void requireAccess(UserInfo user, String bookId) {
        if (user == null || blank(bookId) || jdbc.queryForObject(
                "SELECT COUNT(*) FROM permission_book pb JOIN book b ON b.id=pb.book_id WHERE pb.user_id=? AND pb.book_id=? AND pb.deleted='n' AND b.deleted='n'",
                Long.class, user.getId(), bookId) == 0) denied();
    }

    public void requireAdministrator(UserInfo user, String bookId) {
        requireAccess(user, bookId);
        if (jdbc.queryForObject("SELECT COUNT(*) FROM role_member WHERE member_id=? AND book_id=? AND role_id=? AND type='USER'",
                Long.class, user.getId(), bookId, "ROLE_ADMINISTRATORS") == 0) denied();
    }

    public void checkRequest(String table, Object... values) {
        if (!TABLES.contains(table)) throw new IllegalArgumentException("Unsupported ownership table: " + table);
        UserInfo user = AuthorizationUtils.getUserInfo();
        if (user == null || blank(user.getBookId())) denied();
        requireAccess(user, user.getBookId());
        org.springframework.web.context.request.RequestAttributes attributes = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        jakarta.servlet.http.HttpServletRequest request = attributes instanceof org.springframework.web.context.request.ServletRequestAttributes servlet ? servlet.getRequest() : null;
        if (request != null && Set.of("POST", "PUT", "PATCH", "DELETE").contains(request.getMethod())) {
            com.financial.cloud.constants.auth.ProductRoles.requireWriteBusiness();
        }
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Object value : values) walk(table, value, user.getBookId(), visited);
    }

    public void checkBook(String book) {
        UserInfo user = AuthorizationUtils.getUserInfo();
        if (user == null || (!blank(book) && !Objects.equals(book, user.getBookId()))) denied();
        requireAccess(user, user.getBookId());
    }

    public String templateReadScope(String relatedId) {
        UserInfo user = AuthorizationUtils.getUserInfo();
        if (user == null) denied();
        if (blank(relatedId) || Objects.equals(relatedId, user.getBookId())) {
            requireAccess(user, user.getBookId());
            return user.getBookId();
        }
        if (jdbc.queryForObject("SELECT COUNT(*) FROM standard WHERE id=?", Long.class, relatedId) == 0) denied();
        return relatedId; // explicit, read-only public standard templates
    }

    public void requireTemplateRead(String id) {
        List<String> owners = jdbc.query("SELECT related_id FROM voucher_template WHERE id=?",
                (rs, row) -> rs.getString(1), id);
        for (String owner : owners) templateReadScope(owner);
    }

    public void checkAccountWrites(List<UserInfo> accounts, UserInfo caller) {
        requireAdministrator(caller, caller.getBookId());
        for (UserInfo account : accounts) {
            if (blank(account.getId())) continue;
            List<Map<String, Object>> rows = jdbc.queryForList("SELECT book_id,created_by FROM userinfo WHERE id=? AND deleted='n'", account.getId());
            if (rows.isEmpty()) continue;
            Map<String, Object> original = rows.get(0);
            if (!Objects.equals(caller.getId(), account.getId())) {
                boolean createdHere = Objects.equals(original.get("created_by"), caller.getId())
                        && Objects.equals(original.get("book_id"), caller.getBookId());
                if (!createdHere) requireAccess(account, caller.getBookId());
                for (String book : jdbc.query("SELECT book_id FROM permission_book WHERE user_id=? AND deleted='n'", (rs, row) -> rs.getString(1), account.getId())) {
                    requireAdministrator(caller, book);
                }
            }
            account.setBookId((String) original.get("book_id"));
        }
    }

    public void checkReference(String table, String field, Object value) {
        String related = relation(table, field);
        checkRequest(related == null ? table : related, value);
    }

    private void walk(String table, Object value, String book, Set<Object> visited) {
        if (value == null) return;
        if (value instanceof CharSequence || value instanceof Number) {
            checkId(table, value.toString(), book); return;
        }
        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) walk(table, item, book, visited);
            return;
        }
        if (!value.getClass().getName().startsWith("com.financial.cloud.") || !visited.add(value)) return;
        TableName annotation = value.getClass().getAnnotation(TableName.class);
        String ownTable = annotation != null && TABLES.contains(annotation.value()) ? annotation.value()
                : TYPES.getOrDefault(value.getClass().getSimpleName(), table);
        for (Class<?> type = value.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) continue;
                try {
                    field.setAccessible(true);
                    Object item = field.get(value);
                    if (item == null) continue;
                    String name = field.getName();
                    if (value.getClass().getSimpleName().equals("BooksVoucherItemAuxiliaryValue") && name.equals("value")) {
                        checkId("assist_acc", item.toString(), book);
                    } else if (name.equals("bookId") || ((ownTable.startsWith("voucher_template")) && name.equals("relatedId"))) {
                        if (!blank(item.toString()) && !book.equals(item.toString())) denied();
                    } else if (name.equals("originId") || name.equals("parentId")) {
                        if (ownTable.equals("book_init_balance")) {
                            checkId("book_subject", item.toString(), book);
                            checkId("book_init_balance", item.toString(), book);
                        } else if (name.equals("parentId")) {
                            checkId(ownTable, item.toString(), book);
                        }
                    } else if (name.equals("id") || name.equals("configId") || name.equals("noId")) {
                        if (!value.getClass().getSimpleName().equals("VoucherItemAuxiliaryDto")) {
                            checkId(ownTable, item.toString(), book);
                        } // auxiliary-group id denotes a type; its nested value denotes the owned record
                    } else if (name.equals("listIds") || name.equals("ids")) {
                        walk(ownTable, item, book, visited);
                    } else {
                        String related = relation(ownTable, name);
                        if (related != null) {
                            walk(related, item, book, visited);
                        } else if (!(item instanceof CharSequence) && !(item instanceof Number)) {
                            walk(ownTable, item, book, visited);
                        }
                    }
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("Cannot validate ownership", e);
                }
            }
        }
        // Optional query scopes and new rows must also receive the server's book.
        for (Class<?> type = value.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field scope = type.getDeclaredField("bookId");
                scope.setAccessible(true);
                scope.set(value, book);
                break;
            } catch (NoSuchFieldException ignored) {
                // continue through DTO/entity base classes
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Cannot bind book scope", e);
            }
        }
    }

    private String relation(String table, String name) {
        if (name.endsWith("SubjectId") || name.equals("subjectId") || name.equals("subjectIds") || name.equals("belongSubjectId")) return "book_subject";
        if (name.equals("originId") && table.equals("book_init_balance")) return "book_subject";
        if (name.equals("parentId")) return table;
        if (name.equals("accId") || name.equals("accountId")) return "journal_account";
        if (name.equals("assetId") || name.equals("assetIds")) return "fixed_asset";
        if (name.equals("categoryId")) return "asset_category";
        if (name.equals("deptId") || name.equals("departmentId")) return "organizations";
        if (name.equals("employeeId") || name.equals("employeeIds")) return "employee";
        if (name.equals("salaryId") || name.equals("salaryIds")) return "employee_salary";
        if (name.equals("templateId")) return "voucher_template";
        if (name.equals("voucherId") || name.endsWith("VoucherId")) return "voucher";
        if (name.equals("voucherItemId")) return "voucher_item";
        if (name.equals("assistId") || name.equals("auxiliaryId") || name.equals("counterpartId")) return "assist_acc";
        if (name.equals("itemId") && table.equals("fixed_asset_check")) return "fixed_asset_check_item";
        if (name.equals("checkId")) return "fixed_asset_check";
        if (name.equals("claimId")) return "expense_claim";
        if (name.equals("cashFlowItemDtos")) return "config_cash_flow_balance";
        if (name.equals("ruleList") || name.equals("rules")) return "statement_rules";
        return null;
    }

    public void checkId(String table, String id, String book) {
        checkId(table, id, book, new HashSet<>());
    }

    private void checkId(String table, String id, String book, Set<String> visited) {
        if (blank(id) || "0".equals(id)) return; // root sentinels and new records
        if (!TABLES.contains(table)) throw new IllegalArgumentException("Unsupported ownership table: " + table);
        if (!visited.add(table + ":" + id)) return;
        String column = table.equals("config") ? "config_id" : "id";
        String owner = table.startsWith("voucher_template") ? "related_id" : "book_id";
        List<String> books = jdbc.query("SELECT " + owner + " FROM " + table + " WHERE " + column + "=?",
                (rs, row) -> rs.getString(1), id);
        // A new client-generated ID may not exist yet. Existing records can never change owner.
        for (String actual : books) if (!book.equals(actual)) denied();
        if (books.isEmpty()) return;
        // Stored references also need validation before a detail read or balance update.
        for (Map<String, Object> row : jdbc.queryForList("SELECT * FROM " + table + " WHERE " + column + "=?", id)) {
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                StringBuilder name = new StringBuilder();
                boolean upper = false;
                for (char letter : entry.getKey().toCharArray()) {
                    if (letter == '_') upper = true;
                    else { name.append(upper ? Character.toUpperCase(letter) : letter); upper = false; }
                }
                String related = relation(table, name.toString());
                if (related != null && entry.getValue() != null) {
                    checkId(related, entry.getValue().toString(), book, visited);
                }
            }
        }
        if (table.equals("voucher")) {
            for (Map<String, Object> row : jdbc.queryForList("SELECT id FROM voucher_item WHERE voucher_id=?", id)) {
                checkId("voucher_item", row.get("id").toString(), book, visited);
            }
        }
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static void denied() { throw new BusinessException(UsersBusinessCode.PERMISSION_DENIED); }
}
