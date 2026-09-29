package com.financial.cloud.service.book.backup;

import com.financial.cloud.service.book.backup.BackupTableSpec.FkEdge;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 账套备份表注册表：备份/恢复的唯一权威清单。
 * 顺序即恢复时的灌数顺序（被引用表必须先于引用表）。
 */
public final class BackupTableRegistry {

    private BackupTableRegistry() {
    }

    /** 备份包含的表（恢复顺序）。对应 openspec/changes/book-backup-restore/design.md §2。 */
    public static final List<BackupTableSpec> SPECS = List.of(
            // 科目与期初
            BackupTableSpec.of("book_subject")
                    .fks(FkEdge.of("parent_id", "book_subject")),
            BackupTableSpec.of("book_init_balance")
                    .fks(FkEdge.of("parent_id", "book_init_balance")),
            // 辅助核算与凭证字号（均无 deleted 列）
            BackupTableSpec.of("assist_acc"),
            BackupTableSpec.of("voucher_word").noDeleted(),
            // 凭证链（人员 ID 置空，姓名列保留用于展示）
            BackupTableSpec.of("voucher")
                    .nulls("company_id", "audit_member_id", "sender_id", "manager_id"),
            BackupTableSpec.of("voucher_item")
                    .fks(FkEdge.of("voucher_id", "voucher"),
                            FkEdge.of("subject_id", "book_subject")),
            BackupTableSpec.of("voucher_auxiliary").noDeleted()
                    .fks(FkEdge.of("voucher_id", "voucher"),
                            FkEdge.of("voucher_item_id", "voucher_item"),
                            FkEdge.soft("item_id", "assist_acc")),
            BackupTableSpec.of("voucher_item_cash_flow").noDeleted()
                    .fks(FkEdge.of("voucher_item_id", "voucher_item")),
            // 附件关联表：file_id 指向 file_storage（全局表，二进制不进备份包；同实例恢复仍可用）
            BackupTableSpec.of("voucher_attachment")
                    .fks(FkEdge.of("voucher_id", "voucher")),
            BackupTableSpec.of("approval_record")
                    .viaVoucher()
                    .fks(FkEdge.of("voucher_id", "voucher"))
                    .nulls("approver_id"),
            // 出纳日记账
            BackupTableSpec.of("journal_account")
                    .fks(FkEdge.soft("subject_id", "book_subject")),
            BackupTableSpec.of("journal_entry")
                    .fks(FkEdge.of("acc_id", "journal_account"),
                            FkEdge.soft("subject_id", "book_subject"),
                            FkEdge.soft("voucher_id", "voucher")),
            BackupTableSpec.of("journal_summary"),
            BackupTableSpec.of("journal_reconciliation")
                    .fks(FkEdge.of("acc_id", "journal_account")),
            // 固定资产
            BackupTableSpec.of("asset_category")
                    .fks(FkEdge.soft("fixed_asset_subject_id", "book_subject"),
                            FkEdge.soft("accum_depr_subject_id", "book_subject")),
            BackupTableSpec.of("fixed_asset")
                    .fks(FkEdge.of("category_id", "asset_category"),
                            FkEdge.soft("fixed_asset_subject_id", "book_subject"),
                            FkEdge.soft("purchase_counterpart_subject_id", "book_subject"),
                            FkEdge.soft("tax_subject_id", "book_subject"),
                            FkEdge.soft("accum_depr_subject_id", "book_subject"),
                            FkEdge.soft("expense_subject_id", "book_subject"),
                            FkEdge.soft("disposal_subject_id", "book_subject"),
                            FkEdge.soft("impairment_subject_id", "book_subject"),
                            FkEdge.soft("impairment_counterpart_subject_id", "book_subject"),
                            FkEdge.soft("purchase_voucher_id", "voucher"),
                            FkEdge.soft("dispose_voucher_id", "voucher"))
                    .nulls("dept_id", "user_id"),
            BackupTableSpec.of("fixed_asset_work")
                    .fks(FkEdge.of("asset_id", "fixed_asset")),
            BackupTableSpec.of("fixed_asset_accrual")
                    .fks(FkEdge.soft("voucher_id", "voucher")),
            BackupTableSpec.of("fixed_asset_depr")
                    .fks(FkEdge.of("asset_id", "fixed_asset"),
                            FkEdge.of("accrual_id", "fixed_asset_accrual"),
                            FkEdge.soft("expense_subject_id", "book_subject"),
                            FkEdge.soft("accum_depr_subject_id", "book_subject"))
                    .nulls("dept_id"),
            BackupTableSpec.of("fixed_asset_change")
                    .fks(FkEdge.of("asset_id", "fixed_asset")),
            BackupTableSpec.of("fixed_asset_change_item")
                    .fks(FkEdge.of("change_id", "fixed_asset_change"),
                            FkEdge.of("asset_id", "fixed_asset")),
            // 资产盘点
            BackupTableSpec.of("fixed_asset_check"),
            BackupTableSpec.of("fixed_asset_check_item")
                    .fks(FkEdge.of("check_id", "fixed_asset_check"),
                            FkEdge.of("asset_id", "fixed_asset")),
            // 费用报销
            BackupTableSpec.of("expense_claim")
                    .fks(FkEdge.soft("voucher_id", "voucher")),
            BackupTableSpec.of("expense_claim_item")
                    .fks(FkEdge.of("claim_id", "expense_claim")),
            BackupTableSpec.of("expense_claim_attachment")
                    .fks(FkEdge.of("claim_id", "expense_claim")),
            // 往来核销（无 deleted 列；counterpart_id 指向 assist_acc 软引用）
            BackupTableSpec.of("arap_writeoff").noDeleted()
                    .fks(FkEdge.soft("counterpart_id", "assist_acc")),
            BackupTableSpec.of("arap_writeoff_line").noDeleted()
                    .fks(FkEdge.of("writeoff_id", "arap_writeoff"),
                            FkEdge.of("voucher_item_id", "voucher_item"),
                            FkEdge.soft("voucher_id", "voucher")),
            // 组织/部门（账套级：EmployeeMapper.pageList 按 book_id INNER JOIN，
            // 不备份会导致恢复后员工列表为空；parent_id 自引用软映射，人员字段置空）
            BackupTableSpec.of("organizations")
                    .fks(FkEdge.soft("parent_id", "organizations"))
                    .nulls("created_by", "modified_by"),
            // 薪资（employee.manager_id 指向实例级用户，置空）
            BackupTableSpec.of("employee")
                    .fks(FkEdge.soft("department_id", "organizations"))
                    .nulls("manager_id"),
            BackupTableSpec.of("employee_salary")
                    .fks(FkEdge.of("employee_id", "employee"),
                            FkEdge.soft("accrual_voucher_id", "voucher"),
                            FkEdge.soft("salary_voucher_id", "voucher")),
            BackupTableSpec.of("employee_salary_summary")
                    .fks(FkEdge.soft("accrual_voucher_id", "voucher"),
                            FkEdge.soft("salary_voucher_id", "voucher")),
            BackupTableSpec.of("employee_salary_temp")
                    .fks(FkEdge.of("employee_id", "employee")),
            BackupTableSpec.of("employee_tax_deduction"),
            BackupTableSpec.of("config_salary_formula"),
            BackupTableSpec.of("config_insurance_fund").noDeleted(),
            // 结账
            BackupTableSpec.of("settlement"),
            BackupTableSpec.of("settlement_carryforward")
                    .fks(FkEdge.soft("voucher_id", "voucher"))
                    .nulls("voucher_template_id"),
            // 报表与配置
            BackupTableSpec.of("statement_rules").noDeleted(),
            BackupTableSpec.of("statement_subject_balance")
                    .fks(FkEdge.soft("parent_id", "book_subject"),
                            FkEdge.soft("source_id", "book_subject")),
            BackupTableSpec.of("statement_balance_sheet"),
            BackupTableSpec.of("statement_balance_sheet_item")
                    // 表头永不落库，模板行 balance_sheet_id='template' 无对应记录：保留原值
                    .fks(FkEdge.softKeep("balance_sheet_id", "statement_balance_sheet")),
            BackupTableSpec.of("statement_income"),
            BackupTableSpec.of("statement_income_item")
                    // 模板行 income_id='template' 无对应表头：保留原值；真实表头行正常重映射
                    .fks(FkEdge.softKeep("income_id", "statement_income")),
            BackupTableSpec.of("statement_cash_flow"),
            BackupTableSpec.of("config_cash_flow_balance").noDeleted(),
            // 科目与现金流项目关系（code 引用，无 ID 外键；模板行 is_template=1 目标实例自带，仅备份非模板行）
            BackupTableSpec.of("standard_subject_cash_flow").noDeleted().where("is_template = 0"),
            BackupTableSpec.of("config").withPk("config_id").noDeleted()
    );

    /** 账套主表：单独处理（恢复时新建，元信息存于 manifest.book）。 */
    public static final String BOOK_TABLE = "book";

    /**
     * 显式排除的账套级（含 book_id）表：实例身份/权限/日志/锁/第三方配置。
     * 每条排除必须与 design.md §2「明确排除」一致。
     */
    public static final Set<String> EXCLUDED_BOOK_SCOPED_TABLES = Set.of(
            "userinfo",               // 用户账号（实例级身份）
            "permission",             // 权限
            "permission_book",        // 账套授权（恢复环境用户不同）
            "role_member",            // 角色成员
            "history_event",          // 日志
            "history_login",          // 登录日志
            "history_synchronizer",   // 同步日志
            "history_system_logs",    // 系统日志
            "session_list",           // 会话
            "scheduled_lock",         // 调度锁
            "socials_associate",      // 第三方登录关联
            "socials_provider",       // 第三方登录配置
            "config_email_senders",   // 邮件配置（实例级）
            "config_sms_provider",    // 短信配置（实例级）
            "config_login_policy"     // 登录策略（实例级）
    );

    private static final Map<String, BackupTableSpec> BY_TABLE =
            SPECS.stream().collect(Collectors.toUnmodifiableMap(BackupTableSpec::table, Function.identity()));

    public static BackupTableSpec specOf(String table) {
        return BY_TABLE.get(table);
    }
}
