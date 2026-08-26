package com.financial.cloud.constants.common;

import java.util.regex.Pattern;

/**
 *  Regex for email , mobile and etc.
 */
public class ConstsRegex {
	
	 public static final Pattern 	EMAIL_PATTERN 		= Pattern.compile("^\\s*\\w+(?:\\.{0,1}[\\w-]+)*@[a-zA-Z0-9]+(?:[-.][a-zA-Z0-9]+)*\\.[a-zA-Z]+\\s*$");

	 public static final Pattern 	MOBILE_PATTERN 		= Pattern.compile("^[1][3,4,5,6,7,8,9][0-9]{9}$");
	 
	 public static final Pattern  	IPADDRESS_REGEX 	= Pattern.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
	 
	 public static final Pattern  	WHITESPACE_REGEX 	= Pattern.compile("\\s");
	 
	 public static final Pattern 	CHINESE_REGEX 		= Pattern.compile("[\\u4e00-\\u9fa5]");
}
