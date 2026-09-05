package com.financial.cloud.constants.common;

public final class MessageKeys {

    private MessageKeys() {
    }

    public static final class Login {
        public static final String ERROR_ATTEMPTS = "login.error.attempts";
        public static final String ERROR_LOCKED = "login.error.locked";
        public static final String ERROR_INACTIVE = "login.error.inactive";
        public static final String ERROR_PASSWORD = "login.error.password";
        public static final String ERROR_PASSWORD_ATTEMPTS = "login.error.password.attempts";
        public static final String ERROR_PASSWORD_NULL = "login.error.password.null";
        public static final String ERROR_USERNAME = "login.error.username";
        public static final String ERROR_USERNAME_NULL = "login.error.username.null";
        public static final String ERROR_EMAIL_NULL = "login.error.email.null";
        public static final String ERROR_CAPTCHA = "login.error.captcha";
        public static final String ERROR_AUTH_TYPE = "login.error.auth_type";
        public static final String ERROR_SESSION = "login.error.session";
        public static final String ERROR_SOCIAL = "login.error.social";

        private Login() {
        }
    }

    public static final class PasswordPolicy {
        public static final String PREFIX = "password.policy.";

        public static final String CONTAINS_USERNAME = PREFIX + "contains_username";
        public static final String OLD_PASSWORD_MATCH = PREFIX + "old_password_match";
        public static final String OLD_PASSWORD_NOT_MATCH = PREFIX + "old_password_not_match";
        public static final String CONFIRM_PASSWORD_NOT_MATCH = PREFIX + "confirm_password_not_match";
        public static final String TOO_SHORT = PREFIX + "too_short";
        public static final String TOO_LONG = PREFIX + "too_long";
        public static final String INSUFFICIENT_LOWERCASE = PREFIX + "insufficient_lowercase";
        public static final String INSUFFICIENT_UPPERCASE = PREFIX + "insufficient_uppercase";
        public static final String INSUFFICIENT_DIGIT = PREFIX + "insufficient_digit";
        public static final String INSUFFICIENT_SPECIAL = PREFIX + "insufficient_special";
        public static final String INSUFFICIENT_EXPIRES_DAYS = PREFIX + "insufficient_expires_days";

        public static String passay(String errorCode) {
            return PREFIX + errorCode.toLowerCase();
        }

        private PasswordPolicy() {
        }
    }

    public static final class Book {
        public static final String ILLEGAL_MOVE = "book.error.illegal_move";
        public static final String DUPLICATE_SUBJECT_CODE_EXIST = "book.error.duplicate_subject_code_exist";
        public static final String SUB_SUBJECTS_EXISTS = "book.error.sub_subjects_exists";
        public static final String SUB_SUBJECTS_ACTIVE = "book.error.sub_subjects_active";
        public static final String PARENT_SUBJECT_FORBIDDEN = "book.error.parent_subject_forbidden";
        public static final String DUPLICATE_DEEP_LIMIT = "book.error.duplicate_deep_limit";
        public static final String DUPLICATE_SUBJECTS_EXIST = "book.error.duplicate_subjects_exist";
        public static final String DUPLICATE_SETNAME_EXIST = "book.error.duplicate_setname_exist";
        public static final String ALREADY_STANDARD_USED = "book.error.already_standard_used";
        public static final String ALREADY_SET_USED = "book.error.already_set_used";
        public static final String DISABLE_BEFORE_DELETE = "book.error.disable_before_delete";
        public static final String DELETE_HAS_VOUCHER = "book.error.delete_has_voucher";
        public static final String BOOK_SUB_SUBJECTS_ACTIVE = "book.error.book_sub_subjects_active";
        public static final String BOOK_DUPLICATE_SUBJECTS_EXIST = "book.error.book_duplicate_subjects_exist";
        public static final String BOOK_DUPLICATE_SUBJECT_CODE_EXIST = "book.error.book_duplicate_subject_code_exist";
        public static final String ASSIST_ACC_HAS_CHILDREN = "book.error.assist_acc_has_children";
        public static final String ASSIST_ACC_PARENT_HAS_ASSIST = "book.error.assist_acc_parent_has_assist";

        private Book() {
        }
    }

    public static final class Org {
        public static final String SUB_USERS_EXISTS = "org.error.sub_users_exists";
        public static final String SYNC_USERS_EXISTS = "org.error.sync_users_exists";
        public static final String SUB_ORGS_EXISTS = "org.error.sub_orgs_exists";
        public static final String ILLEGAL_MOVE = "org.error.illegal_move";
        public static final String SUB_USERS_ACTIVE = "org.error.sub_users_active";
        public static final String SYNC_USERS_ACTIVE = "org.error.sync_users_active";
        public static final String SUB_ORGS_ACTIVE = "org.error.sub_orgs_active";
        public static final String CURRENT_ORGS_ACTIVE = "org.error.current_orgs_active";
        public static final String CURRENT_USERS_ACTIVE = "org.error.current_users_active";
        public static final String PARENT_ORGS_FORBIDDEN = "org.error.parent_orgs_forbidden";
        public static final String DUPLICATE_ORGS_EXIST = "org.error.duplicate_orgs_exist";
        public static final String DUPLICATE_ORGSCODE_EXIST = "org.error.duplicate_orgscode_exist";
        public static final String GROUP_ALREADY_USED = "org.error.group_already_used";
        public static final String GROUPS_ACTIVE = "org.error.groups_active";

        private Org() {
        }
    }

    public static final class User {
        public static final String FORBIDDEN = "user.error.forbidden";
        public static final String USERNAME_USED = "user.error.username_used";
        public static final String MOBILE_USED = "user.error.mobile_used";
        public static final String EMAIL_USED = "user.error.email_used";
        public static final String VERIFY_MOBILE_ABSENT = "user.error.verify_mobile_absent";
        public static final String ROLE_REQUIRED = "user.error.role_required";
        public static final String BOOK_REQUIRED = "user.error.book_required";
        public static final String PERMISSION_DENIED = "user.error.permission_denied";

        private User() {
        }
    }

    public static final class Common {
        public static final String OPERATION_FAILED = "common.error.operation_failed";
        public static final String SORT_PARAM_INVALID = "common.error.sort_param_invalid";
        public static final String FILE_NOT_FOUND = "common.error.file_not_found";
        public static final String PARAM_INVALID_FOR_QUERY = "common.error.param_invalid_for_query";
        public static final String SQL_INJECTION_RISK = "common.error.sql_injection_risk";
        public static final String EXCEL_SUFFIX_ERROR = "common.error.excel_suffix_error";

        private Common() {
        }
    }

    public static final class Statement {
        public static final String PERIOD_TYPE_EMPTY = "statement.error.period_type_empty";
        public static final String REPORT_DATE_EMPTY = "statement.error.report_date_empty";
        public static final String BOOK_ID_EMPTY = "statement.error.book_id_empty";
        public static final String INVALID_PERIOD_TYPE = "statement.error.invalid_period_type";
        public static final String INVALID_QUARTER = "statement.error.invalid_quarter";
        public static final String INVALID_HALF_YEAR = "statement.error.invalid_half_year";
        public static final String DATE_RANGE_SIZE = "statement.error.date_range_size";
        public static final String START_DATE_AFTER_END = "statement.error.start_date_after_end";
        public static final String CASH_FLOW_MODIFY_FORBIDDEN = "statement.error.cash_flow_modify_forbidden";
        public static final String CASH_FLOW_INIT_REQUIRED = "statement.error.cash_flow_init_required";
        public static final String CASH_FLOW_SQL_REQUIRED = "statement.error.cash_flow_sql_required";
        public static final String UNKNOWN_CASH_FLOW_CODE = "statement.error.unknown_cash_flow_code";
        public static final String BALANCE_SHEET_TRIAL_BALANCE_FAILED = "statement.error.balance_sheet_trial_balance_failed";
        public static final String INCOME_STATEMENT_FORMULA_FAILED = "statement.error.income_statement_formula_failed";
        public static final String CASH_FLOW_RECONCILIATION_FAILED = "statement.error.cash_flow_reconciliation_failed";
        public static final String EXPENSE_DETAIL_PERIOD_TOO_LONG = "statement.error.expense_detail_period_too_long";

        private Statement() {
        }
    }

    public static final class Config {
        public static final String BOOK_NOT_INIT_CURRENT_PERIOD = "config.error.book_not_init_current_period";
        public static final String BOOK_NOT_INIT_INITIAL_PERIOD = "config.error.book_not_init_initial_period";
        public static final String BUILTIN_PARAM_CANNOT_DELETE = "config.error.builtin_param_cannot_delete";
        public static final String BOOK_MISSING_PARAM = "config.error.book_missing_param";
        public static final String PERSONAL_TAX_RANGE_INVALID = "config.error.personal_tax_range_invalid";
        public static final String PERSONAL_TAX_LEVEL_DUPLICATE = "config.error.personal_tax_level_duplicate";
        public static final String SALARY_FORMULA_NAME_DUPLICATE = "config.error.salary_formula_name_duplicate";
        public static final String PASSWORD_POLICY_NOT_CONFIGURED = "config.error.password_policy_not_configured";

        private Config() {
        }
    }

    public static final class Hr {
        public static final String INSURANCE_FUND_CONFIG_REQUIRED = "hr.error.insurance_fund_config_required";
        public static final String EMPLOYEE_NOT_FOUND = "hr.error.employee_not_found";
        public static final String RECORD_NOT_FOUND = "hr.error.record_not_found";
        public static final String NO_DATA = "hr.error.no_data";
        public static final String CUSTOM_PAY_BASE_REQUIRED = "hr.error.custom_pay_base_required";
        public static final String PAYMENT_EXPORT_NO_DATA = "hr.error.payment_export_no_data";
        public static final String PAYMENT_EXPORT_MISSING_BANK = "hr.error.payment_export_missing_bank";

        private Hr() {
        }
    }

    public static final class Voucher {
        public static final String ITEM_OR_TIME_INVALID = "voucher.error.item_or_time_invalid";

        private Voucher() {
        }
    }

    public static final class Journal {
        public static final String INSUFFICIENT_BALANCE = "journal.error.insufficient_balance";

        private Journal() {
        }
    }

    public static final class Assist {
        public static final String CODE_DUPLICATE = "assist.error.code_duplicate";

        private Assist() {
        }
    }

    public static final class FixedAsset {
        public static final String CATEGORY_CODE_DUPLICATE = "fixed_asset.error.category_code_duplicate";
        public static final String CATEGORY_IN_USE = "fixed_asset.error.category_in_use";
        public static final String ASSET_CODE_DUPLICATE = "fixed_asset.error.asset_code_duplicate";
        public static final String ASSET_HAS_DEPR = "fixed_asset.error.asset_has_depr";
        public static final String ASSET_NOT_FOUND = "fixed_asset.error.asset_not_found";
        public static final String CALC_FIELDS_LOCKED = "fixed_asset.error.calc_fields_locked";
        public static final String WORK_REQUIRED = "fixed_asset.error.work_required";
        public static final String NOTHING_TO_ACCRUE = "fixed_asset.error.nothing_to_accrue";
        public static final String REACCRUE_FORBIDDEN = "fixed_asset.error.reaccrue_forbidden";
        public static final String SUBJECT_REQUIRED = "fixed_asset.error.subject_required";
        public static final String CATEGORY_NOT_FOUND = "fixed_asset.error.category_not_found";
        public static final String CHANGE_ITEMS_EMPTY = "fixed_asset.error.change_items_empty";
        public static final String CHANGE_NO_DIFF = "fixed_asset.error.change_no_diff";
        public static final String ALREADY_DISPOSED = "fixed_asset.error.already_disposed";
        public static final String DISPOSE_SUBJECT_REQUIRED = "fixed_asset.error.dispose_subject_required";
        public static final String ACCELERATED_LIFE_INVALID = "fixed_asset.error.accelerated_life_invalid";
        public static final String PURCHASE_SUBJECT_REQUIRED = "fixed_asset.error.purchase_subject_required";
        public static final String ALREADY_SUSPENDED = "fixed_asset.error.already_suspended";
        public static final String NOT_SUSPENDED = "fixed_asset.error.not_suspended";
        public static final String CANNOT_SUSPEND_DISPOSED = "fixed_asset.error.cannot_suspend_disposed";

        private FixedAsset() {
        }
    }

    public static final class Standard {
        public static final String USED_BY_BOOK = "standard.error.used_by_book";
        public static final String SUBJECT_NOT_FOUND = "standard.error.subject_not_found";
        public static final String CASH_FLOW_LEAF_SUBJECT_REQUIRED = "standard.error.cash_flow_leaf_subject_required";

        private Standard() {
        }
    }

    public static final class Excel {
        public static final String TEMPLATE_ROW_NOT_FOUND = "excel.error.template_row_not_found";
        public static final String FIELD_NOT_FOUND = "excel.error.field_not_found";

        private Excel() {
        }
    }

    public static final class Validation {
        public static final String ASSIST_CODE_REQUIRED = "{validation.assist.code.required}";
        public static final String ASSIST_NAME_REQUIRED = "{validation.assist.name.required}";
        public static final String ASSIST_TARGET_REQUIRED = "{validation.assist.target.required}";
        public static final String ASSIST_TYPE_REQUIRED = "{validation.assist.type.required}";
        public static final String BOOK_ACCOUNT_SUBJECT_REQUIRED = "{validation.book.account_subject.required}";
        public static final String BOOK_BALANCE_DIRECTION_REQUIRED = "{validation.book.balance_direction.required}";
        public static final String BOOK_BOOK_ID_REQUIRED = "{validation.book.book_id.required}";
        public static final String BOOK_CASH_SUBJECT_FLAG_REQUIRED = "{validation.book.cash_subject_flag.required}";
        public static final String BOOK_INIT_PERIOD_REQUIRED = "{validation.book.init_period.required}";
        public static final String BOOK_NAME_MAX_LENGTH = "{validation.book.name.max_length}";
        public static final String BOOK_NAME_REQUIRED = "{validation.book.name.required}";
        public static final String BOOK_OWNER_BOOK_ID_REQUIRED = "{validation.book.owner_book_id.required}";
        public static final String BOOK_SUBJECT_CODE_PATTERN = "{validation.book.subject_code.pattern}";
        public static final String BOOK_SUBJECT_CODE_REQUIRED = "{validation.book.subject_code.required}";
        public static final String BOOK_SUBJECT_ENCODING_REQUIRED = "{validation.book.subject_encoding.required}";
        public static final String BOOK_SUBJECT_NAME_MAX_LENGTH = "{validation.book.subject_name.max_length}";
        public static final String BOOK_SUBJECT_NAME_REQUIRED = "{validation.book.subject_name.required}";
        public static final String BOOK_SUBJECT_NUMBER_REQUIRED = "{validation.book.subject_number.required}";
        public static final String BOOK_SUBJECT_TYPE_REQUIRED = "{validation.book.subject_type.required}";
        public static final String BOOK_TAX_NATURE_REQUIRED = "{validation.book.tax_nature.required}";
        public static final String COMMON_DATE_REQUIRED = "{validation.common.date.required}";
        public static final String COMMON_EDIT_TARGET_REQUIRED = "{validation.common.edit_target.required}";
        public static final String COMMON_ID_REQUIRED = "{validation.common.id.required}";
        public static final String COMMON_ID_LOWERCASE_REQUIRED = "{validation.common.id_lowercase.required}";
        public static final String COMMON_INPUT_PARAM_REQUIRED = "{validation.common.input_param.required}";
        public static final String COMMON_INPUT_PARAM_LIST_REQUIRED = "{validation.common.input_param_list.required}";
        public static final String COMMON_LEVEL_REQUIRED = "{validation.common.level.required}";
        public static final String COMMON_MONTH_REQUIRED = "{validation.common.month.required}";
        public static final String COMMON_NAME_REQUIRED = "{validation.common.name.required}";
        public static final String COMMON_NUMBER_REQUIRED = "{validation.common.number.required}";
        public static final String COMMON_OWNER_MONTH_REQUIRED = "{validation.common.owner_month.required}";
        public static final String COMMON_RELATED_OBJECT_CODE_REQUIRED = "{validation.common.related_object_code.required}";
        public static final String COMMON_RULE_NAME_REQUIRED = "{validation.common.rule_name.required}";
        public static final String COMMON_SELECTED_ID_REQUIRED = "{validation.common.selected_id.required}";
        public static final String COMMON_SORT_ORDER_REQUIRED = "{validation.common.sort_order.required}";
        public static final String COMMON_STATUS_NOT_NULL = "{validation.common.status.not_null}";
        public static final String COMMON_STATUS_REQUIRED = "{validation.common.status.required}";
        public static final String COMMON_TEMPLATE_NAME_REQUIRED = "{validation.common.template_name.required}";
        public static final String COMMON_TYPE_REQUIRED = "{validation.common.type.required}";
        public static final String COMMON_YEAR_REQUIRED = "{validation.common.year.required}";
        public static final String CONFIG_PARAM_KEY_MAX_LENGTH = "{validation.config.param_key.max_length}";
        public static final String CONFIG_PARAM_KEY_REQUIRED = "{validation.config.param_key.required}";
        public static final String CONFIG_PARAM_NAME_MAX_LENGTH = "{validation.config.param_name.max_length}";
        public static final String CONFIG_PARAM_NAME_REQUIRED = "{validation.config.param_name.required}";
        public static final String CONFIG_PARAM_VALUE_MAX_LENGTH = "{validation.config.param_value.max_length}";
        public static final String CONFIG_PARAM_VALUE_REQUIRED = "{validation.config.param_value.required}";
        public static final String CONFIG_TAX_RATE_REQUIRED = "{validation.config.tax_rate.required}";
        public static final String HR_BIRTH_DATE_REQUIRED = "{validation.hr.birth_date.required}";
        public static final String HR_DEPARTMENT_REQUIRED = "{validation.hr.department.required}";
        public static final String HR_EDUCATION_REQUIRED = "{validation.hr.education.required}";
        public static final String HR_EMPLOYEE_STATUS_REQUIRED = "{validation.hr.employee_status.required}";
        public static final String HR_EMPLOYEE_TYPE_REQUIRED = "{validation.hr.employee_type.required}";
        public static final String HR_GENDER_REQUIRED = "{validation.hr.gender.required}";
        public static final String HR_ID_NUMBER_REQUIRED = "{validation.hr.id_number.required}";
        public static final String HR_ID_TYPE_REQUIRED = "{validation.hr.id_type.required}";
        public static final String HR_NAME_MAX_LENGTH = "{validation.hr.name.max_length}";
        public static final String HR_NAME_REQUIRED = "{validation.hr.name.required}";
        public static final String HR_SELECTED_SALARY_FORMULA_REQUIRED = "{validation.hr.selected_salary_formula.required}";
        public static final String ORG_CODE_LENGTH_RANGE = "{validation.org.code.length_range}";
        public static final String ORG_CODE_MAX_LENGTH = "{validation.org.code.max_length}";
        public static final String ORG_CODE_REQUIRED = "{validation.org.code.required}";
        public static final String ORG_COMPANY_NAME_REQUIRED = "{validation.org.company_name.required}";
        public static final String ORG_FULL_NAME_MAX_LENGTH = "{validation.org.full_name.max_length}";
        public static final String ORG_FULL_NAME_REQUIRED = "{validation.org.full_name.required}";
        public static final String ORG_GROUP_CODE_REQUIRED = "{validation.org.group_code.required}";
        public static final String ORG_GROUP_NAME_REQUIRED = "{validation.org.group_name.required}";
        public static final String ORG_NAME_MAX_LENGTH = "{validation.org.name.max_length}";
        public static final String ORG_NAME_REQUIRED = "{validation.org.name.required}";
        public static final String ORG_SELECTED_MEMBER_REQUIRED = "{validation.org.selected_member.required}";
        public static final String ORG_UNIT_NAME_MAX_LENGTH = "{validation.org.unit_name.max_length}";
        public static final String ORG_UNIT_NAME_REQUIRED = "{validation.org.unit_name.required}";
        public static final String STANDARD_ACCOUNTING_STANDARD_MAX_LENGTH = "{validation.standard.accounting_standard.max_length}";
        public static final String STANDARD_ACCOUNTING_STANDARD_REQUIRED = "{validation.standard.accounting_standard.required}";
        public static final String STANDARD_ACCOUNTING_SYSTEM_REQUIRED = "{validation.standard.accounting_system.required}";
        public static final String STANDARD_STANDARD_REQUIRED = "{validation.standard.standard.required}";
        public static final String STANDARD_STANDARD_CODE_REQUIRED = "{validation.standard.standard_code.required}";
        public static final String STATEMENT_CALCULATION_METHOD_REQUIRED = "{validation.statement.calculation_method.required}";
        public static final String STATEMENT_CALCULATION_METHOD_SIMPLE_REQUIRED = "{validation.statement.calculation_method_simple.required}";
        public static final String STATEMENT_DATA_RULE_REQUIRED = "{validation.statement.data_rule.required}";
        public static final String STATEMENT_FINANCIAL_ITEM_REQUIRED = "{validation.statement.financial_item.required}";
        public static final String STATEMENT_FINANCIAL_ITEM_CODE_REQUIRED = "{validation.statement.financial_item_code.required}";
        public static final String STATEMENT_ITEM_TYPE_REQUIRED = "{validation.statement.item_type.required}";
        public static final String STATEMENT_MAIN_REPORT_ID_REQUIRED = "{validation.statement.main_report_id.required}";
        public static final String STATEMENT_PERIOD_REQUIRED = "{validation.statement.period.required}";
        public static final String STATEMENT_REPORT_CATEGORY_REQUIRED = "{validation.statement.report_category.required}";
        public static final String STATEMENT_REPORT_PERIOD_TYPE_REQUIRED = "{validation.statement.report_period_type.required}";
        public static final String STATEMENT_REPORT_TYPE_REQUIRED = "{validation.statement.report_type.required}";
        public static final String USER_CONFIRM_PASSWORD_REQUIRED = "{validation.user.confirm_password.required}";
        public static final String USER_DISPLAY_NAME_REQUIRED = "{validation.user.display_name.required}";
        public static final String USER_EMAIL_REQUIRED = "{validation.user.email.required}";
        public static final String USER_NEW_PASSWORD_REQUIRED = "{validation.user.new_password.required}";
        public static final String USER_NICKNAME_MAX_LENGTH = "{validation.user.nickname.max_length}";
        public static final String USER_PASSWORD_REQUIRED = "{validation.user.password.required}";
        public static final String USER_PHONE_REQUIRED = "{validation.user.phone.required}";
        public static final String USER_ROLE_REQUIRED = "{validation.user.role.required}";
        public static final String USER_SELECTED_ROLES_REQUIRED = "{validation.user.selected_roles.required}";
        public static final String USER_STATUS_REQUIRED = "{validation.user.status.required}";
        public static final String USER_TYPE_REQUIRED = "{validation.user.type.required}";
        public static final String USER_USERNAME_MAX_LENGTH = "{validation.user.username.max_length}";
        public static final String USER_USERNAME_REQUIRED = "{validation.user.username.required}";
        public static final String VOUCHER_ATTACHMENT_COUNT_REQUIRED = "{validation.voucher.attachment_count.required}";
        public static final String VOUCHER_AUDIT_REQUIRED_REQUIRED = "{validation.voucher.audit_required.required}";
        public static final String VOUCHER_EDIT_DEBIT_CREDIT_REQUIRED = "{validation.voucher.edit_debit_credit.required}";
        public static final String VOUCHER_EDIT_SUBJECT_REQUIRED = "{validation.voucher.edit_subject.required}";
        public static final String VOUCHER_EDIT_SUMMARY_REQUIRED = "{validation.voucher.edit_summary.required}";
        public static final String VOUCHER_NEW_VOUCHER_WORD_REQUIRED = "{validation.voucher.new_voucher_word.required}";
        public static final String VOUCHER_ORIGINAL_VOUCHER_WORD_REQUIRED = "{validation.voucher.original_voucher_word.required}";
        public static final String VOUCHER_PREFIX_REQUIRED = "{validation.voucher.prefix.required}";
        public static final String VOUCHER_SUMMARY_REQUIRED = "{validation.voucher.summary.required}";
        public static final String VOUCHER_VOUCHER_ITEMS_REQUIRED = "{validation.voucher.voucher_items.required}";
        public static final String VOUCHER_VOUCHER_PREFIX_REQUIRED = "{validation.voucher.voucher_prefix.required}";
        public static final String VOUCHER_VOUCHER_RULE_REQUIRED = "{validation.voucher.voucher_rule.required}";
        public static final String VOUCHER_VOUCHER_TARGET_REQUIRED = "{validation.voucher.voucher_target.required}";
        public static final String VOUCHER_VOUCHER_WORD_REQUIRED = "{validation.voucher.voucher_word.required}";
        public static final String VOUCHER_VOUCHER_WORD_NUMBER_REQUIRED = "{validation.voucher.voucher_word_number.required}";
        public static final String FIXED_ASSET_CATEGORY_CODE_REQUIRED = "{validation.fixed_asset.category_code.required}";
        public static final String FIXED_ASSET_CATEGORY_NAME_REQUIRED = "{validation.fixed_asset.category_name.required}";
        public static final String FIXED_ASSET_METHOD_REQUIRED = "{validation.fixed_asset.method.required}";
        public static final String FIXED_ASSET_LIFE_MONTHS_REQUIRED = "{validation.fixed_asset.life_months.required}";
        public static final String FIXED_ASSET_RESIDUAL_RATE_REQUIRED = "{validation.fixed_asset.residual_rate.required}";
        public static final String FIXED_ASSET_SUBJECT_REQUIRED = "{validation.fixed_asset.subject.required}";
        public static final String FIXED_ASSET_ACCUM_SUBJECT_REQUIRED = "{validation.fixed_asset.accum_subject.required}";
        public static final String FIXED_ASSET_CODE_REQUIRED = "{validation.fixed_asset.code.required}";
        public static final String FIXED_ASSET_NAME_REQUIRED = "{validation.fixed_asset.name.required}";
        public static final String FIXED_ASSET_CATEGORY_REQUIRED = "{validation.fixed_asset.category.required}";
        public static final String FIXED_ASSET_START_DATE_REQUIRED = "{validation.fixed_asset.start_date.required}";
        public static final String FIXED_ASSET_ORIGINAL_VALUE_REQUIRED = "{validation.fixed_asset.original_value.required}";
        public static final String FIXED_ASSET_EXPENSE_SUBJECT_REQUIRED = "{validation.fixed_asset.expense_subject.required}";

        private Validation() {
        }
    }
}
