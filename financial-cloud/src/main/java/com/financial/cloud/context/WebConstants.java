package com.financial.cloud.context;

public final class WebConstants {

    private WebConstants() {
    }

    public static final String CURRENT_MESSAGE = "current_message";

    public static final String CURRENT_INST = "current_inst";

    public static final String AUTHENTICATION = "current_authentication";

    public static final String LOGIN_ERROR_SESSION_MESSAGE = "login_error_session_message_key";

    public static final class LOGIN_RESULT {
        public static final String SUCCESS = "success";
        public static final String FAIL = "fail";
        public static final String PASSWORD_ERROE = "password error";
        public static final String USER_NOT_EXIST = "user not exist";
        public static final String USER_LOCKED = "locked";
        public static final String USER_INACTIVE = "inactive";

        private LOGIN_RESULT() {
        }
    }
}
