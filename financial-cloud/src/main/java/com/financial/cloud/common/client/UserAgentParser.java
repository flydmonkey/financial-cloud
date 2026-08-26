package com.financial.cloud.common.client;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import cn.hutool.crypto.digest.DigestUtil;

import jakarta.servlet.http.HttpServletRequest;

@Slf4j
public class UserAgentParser {
	
    public static ClientUserAgent  resolveUserAgent(HttpServletRequest  request) {
    	ClientUserAgent browserUserAgent = new ClientUserAgent();
        String userAgent = getUserAgent(request);
        if(StringUtils.isNotBlank(userAgent)) {
        	hash(browserUserAgent,userAgent);
	        if (userAgent.indexOf(ConstBrowserType.MSIE.getName()) > -1) {
	        	msie(browserUserAgent,userAgent);
	        } else if (userAgent.indexOf(ConstBrowserType.EDG.getName()) > -1) {
	        	edg(browserUserAgent,userAgent);
	        }else if (userAgent.indexOf(ConstBrowserType.TRIDENT.getName()) > -1) {
	        	trident(browserUserAgent,userAgent);
	        } else if (userAgent.indexOf(ConstBrowserType.CHROME.getName()) > -1) {
	        	chrome(browserUserAgent,userAgent);
	        } else if (userAgent.indexOf(ConstBrowserType.FIREFOX.getName()) > -1) {
	        	firefox(browserUserAgent,userAgent);
	        }else if (userAgent.indexOf(ConstBrowserType.CLIENTAPP.getName()) > -1) {
	        	clientAPP(browserUserAgent,userAgent);
	        }else if (userAgent.indexOf(ConstBrowserType.SAFARI.getName()) > -1) {
	        	safari(browserUserAgent,userAgent);
	        }else {
	        	browserUserAgent.setPlatform(userAgent);
	        }
        }
        log.debug("ClientUserAgent  {}" , browserUserAgent);
        return browserUserAgent;
    }
    
    static void msie(ClientUserAgent browserUserAgent,String userAgent) {
    	String[] arrayUserAgent =  userAgent.split(";");
    	browserUserAgent.setName(arrayUserAgent[1].trim());
    	browserUserAgent.setPlatform(arrayUserAgent[2].trim());
    }
    
    static void edg(ClientUserAgent browserUserAgent,String userAgent) {
    	String[] arrayUserAgent = userAgent.split(" ");
    	for (int i = 0; i < arrayUserAgent.length; i++) {
            if (arrayUserAgent[i].contains(ConstBrowserType.EDG.getName())) {
            	browserUserAgent.setName( arrayUserAgent[i].trim());
            	browserUserAgent.setName(browserUserAgent.getName().substring(0, browserUserAgent.getName().indexOf('.')));
            }
        }
    	 browserUserAgent.setPlatform( (arrayUserAgent[1].substring(1) + " " + arrayUserAgent[2] + " "
                 + arrayUserAgent[3].substring(0, arrayUserAgent[3].length() - 1)).trim());
    }
    
    static void trident(ClientUserAgent browserUserAgent,String userAgent) {
    	String[] arrayUserAgent = userAgent.split(";");
         browserUserAgent.setName( ConstBrowserType.TRIDENT.getBrowser()+"/" + arrayUserAgent[3].split("\\)")[0]);
         browserUserAgent.setPlatform( arrayUserAgent[0].split("\\(")[1]);
    }
    
    static void chrome(ClientUserAgent browserUserAgent,String userAgent) {
    	String[] arrayUserAgent = userAgent.split(" ");
         for (int i = 0; i < arrayUserAgent.length; i++) {
             if (arrayUserAgent[i].contains(ConstBrowserType.CHROME.getName())) {
             	browserUserAgent.setName( arrayUserAgent[i].trim());
             	browserUserAgent.setName( browserUserAgent.getName().substring(0, browserUserAgent.getName().indexOf('.')));
             }
         }
         browserUserAgent.setPlatform( (arrayUserAgent[1].substring(1) + " " + arrayUserAgent[2] + " "
                 + arrayUserAgent[3].substring(0, arrayUserAgent[3].length() - 1)).trim());
    }
    
    static void firefox(ClientUserAgent browserUserAgent,String userAgent) {
    	String[] arrayUserAgent = userAgent.split(" ");
    	for (int i = 0; i < arrayUserAgent.length; i++) {
		     if (arrayUserAgent[i].contains(ConstBrowserType.FIREFOX.getName())) {
		     	browserUserAgent.setName( arrayUserAgent[i].trim());
		     	browserUserAgent.setName(browserUserAgent.getName().substring(0, browserUserAgent.getName().indexOf('.')));
		     }
    	}
    	browserUserAgent.setPlatform( (arrayUserAgent[1].substring(1) + " " + arrayUserAgent[2] + " "
		     + arrayUserAgent[3].substring(0, arrayUserAgent[3].length() - 1)).trim());
    }
    
    static void safari(ClientUserAgent browserUserAgent,String userAgent) {
    	String[] arrayUserAgent = userAgent.split(" ");
    	browserUserAgent.setName(arrayUserAgent[arrayUserAgent.length-1]);
    	browserUserAgent.setPlatform((arrayUserAgent[3] + " " + arrayUserAgent[4] +" "+ arrayUserAgent[5]).trim());
    }
    
    static void clientAPP(ClientUserAgent browserUserAgent,String userAgent) {
    	String[] arrayUserAgent = userAgent.split(" ");
        browserUserAgent.setName(arrayUserAgent[arrayUserAgent.length-1]);
        browserUserAgent.setPlatform(arrayUserAgent[1].substring(1) + " " + arrayUserAgent[2].substring(0, arrayUserAgent[2].length() - 1).trim());
    }
    
    static String getUserAgent(HttpServletRequest  request){
        return (request != null ? request.getHeader("User-Agent") : null);
    }
    
    static void hash(ClientUserAgent browserUserAgent,String userAgent) {
    	browserUserAgent.setUserAgentHash(DigestUtil.md5Hex(userAgent));
    }

}
