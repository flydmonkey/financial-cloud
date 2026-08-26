package com.financial.cloud.constants;

/**
 * i18n message keys. Format: {@code domain.module.detail_snake_case}.
 */
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
}
