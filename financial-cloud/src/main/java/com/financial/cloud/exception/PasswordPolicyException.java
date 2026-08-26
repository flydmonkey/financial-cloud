package com.financial.cloud.exception;

import com.financial.cloud.constants.system.ConstsServiceMessage;
import com.financial.cloud.constants.common.MessageKeys;
import com.financial.cloud.context.WebContext;

public class PasswordPolicyException extends Exception {

	private static final long serialVersionUID = -253274228039876768L;
	private String errorCode;
	private Object filedValue;

	public PasswordPolicyException(String errorCode, Object filedValue) {
		super();
		this.errorCode = errorCode;
		this.filedValue = filedValue;
	}

	public PasswordPolicyException(String errorCode) {
		super();
		this.errorCode = errorCode;
	}

	public Object getFiledValue() {
		return filedValue;
	}

	public String getKey() {
		return switch (errorCode) {
			case ConstsServiceMessage.PASSWORDPOLICY.XW00000001 -> MessageKeys.PasswordPolicy.CONTAINS_USERNAME;
			case ConstsServiceMessage.PASSWORDPOLICY.XW00000002 -> MessageKeys.PasswordPolicy.OLD_PASSWORD_MATCH;
			case ConstsServiceMessage.PASSWORDPOLICY.XW00000003 -> MessageKeys.PasswordPolicy.TOO_SHORT;
			case ConstsServiceMessage.PASSWORDPOLICY.XW00000004 -> MessageKeys.PasswordPolicy.TOO_LONG;
			case ConstsServiceMessage.PASSWORDPOLICY.XW00000005 -> MessageKeys.PasswordPolicy.INSUFFICIENT_DIGIT;
			case ConstsServiceMessage.PASSWORDPOLICY.XW00000006 -> MessageKeys.PasswordPolicy.INSUFFICIENT_LOWERCASE;
			case ConstsServiceMessage.PASSWORDPOLICY.XW00000007 -> MessageKeys.PasswordPolicy.INSUFFICIENT_UPPERCASE;
			case ConstsServiceMessage.PASSWORDPOLICY.XW00000008 -> MessageKeys.PasswordPolicy.INSUFFICIENT_SPECIAL;
			default -> MessageKeys.PasswordPolicy.PREFIX + errorCode.toLowerCase();
		};
	}

	public String getErrorCode() {
		return errorCode;
	}

	@Override
	public String getMessage() {
		if (filedValue != null) {
			return WebContext.getI18nValue(getKey(), new Object[] { filedValue });
		}
		return WebContext.getI18nValue(getKey());
	}
}
