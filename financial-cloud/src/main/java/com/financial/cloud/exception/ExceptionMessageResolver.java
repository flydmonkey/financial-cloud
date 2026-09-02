package com.financial.cloud.exception;

import java.util.HashMap;
import java.util.Map;

import com.financial.cloud.context.WebContext;
import com.financial.cloud.enums.error.AssistErrorCode;
import com.financial.cloud.enums.error.BookBusinessExceptionEnum;
import com.financial.cloud.enums.error.CommonErrorCode;
import com.financial.cloud.enums.error.ConfigErrorCode;
import com.financial.cloud.enums.error.ExcelErrorCode;
import com.financial.cloud.enums.error.FixedAssetErrorCode;
import com.financial.cloud.enums.error.HrErrorCode;
import com.financial.cloud.enums.error.JournalErrorCode;
import com.financial.cloud.enums.error.OrgsBusinessExceptionEnum;
import com.financial.cloud.enums.error.StandardErrorCode;
import com.financial.cloud.enums.error.StatementErrorCode;
import com.financial.cloud.enums.error.UsersBusinessCode;
import com.financial.cloud.enums.error.VoucherErrorCode;

public final class ExceptionMessageResolver {

    private static final Map<Integer, String> CODE_TO_MESSAGE_KEY = new HashMap<>();

    static {
        register(BookBusinessExceptionEnum.values());
        register(OrgsBusinessExceptionEnum.values());
        register(UsersBusinessCode.values());
        register(CommonErrorCode.values());
        register(StatementErrorCode.values());
        register(ConfigErrorCode.values());
        register(HrErrorCode.values());
        register(StandardErrorCode.values());
        register(VoucherErrorCode.values());
        register(JournalErrorCode.values());
        register(AssistErrorCode.values());
        register(ExcelErrorCode.values());
        register(FixedAssetErrorCode.values());
    }

    private ExceptionMessageResolver() {
    }

    private static void register(ErrorCode[] errorCodes) {
        for (ErrorCode errorCode : errorCodes) {
            String previous = CODE_TO_MESSAGE_KEY.put(errorCode.getCode(), errorCode.getMessageKey());
            if (previous != null) {
                throw new IllegalStateException(
                        "Duplicate business error code: " + errorCode.getCode());
            }
        }
    }

    public static String resolve(int code, Object[] messageArgs) {
        String messageKey = CODE_TO_MESSAGE_KEY.get(code);
        if (messageKey == null) {
            return String.valueOf(code);
        }
        if (messageArgs != null && messageArgs.length > 0) {
            return WebContext.getI18nValue(messageKey, messageArgs);
        }
        return WebContext.getI18nValue(messageKey);
    }
}
