package com.jinbooks.constants;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ConstBrowserType {

	MSIE  		("MSIE"			,  "MSIE"),
	EDG         ("Edg"			,  "Edg"),
	TRIDENT  	("Trident"		,  "MSIE"),
	CHROME  	("Chrome"		,  "Chrome"),
	FIREFOX  	("Firefox"		,  "Firefox"),
	SAFARI  	("Safari"		,  "Safari"),
	CLIENTAPP  	("ClientAPP"	,  "Client App"),
    ;
	
	
    /**
     * name
     */
    @JsonValue
    private final String name;
    
    /**
     * browser
     */
    private final String browser;
    
	public String getName() {
		return name;
	}

	public String getBrowser() {
		return browser;
	}

	private ConstBrowserType(String name, String browser) {
		this.name = name;
		this.browser = browser;
	}
    
    
}
