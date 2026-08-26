package com.financial.cloud.authn.handler;


import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;

import com.financial.cloud.authn.SignedPrincipal;
import com.financial.cloud.util.DateUtils;
import com.financial.cloud.context.WebConstants;

/**
 * 监听会话创建和销毁时间
 * 
 * @author Crystal.Sea
 *
 */
@Slf4j
@WebListener
public class HttpSessionListenerAdapter implements HttpSessionListener {
    
    public HttpSessionListenerAdapter() {
        super();
        log.debug("SessionListenerAdapter inited . ");
    }

    /**
     * session Created
     */
    @Override
    public void sessionCreated(HttpSessionEvent sessionEvent) {
        log.trace("new session Created : {} " , sessionEvent.getSession().getId());
    }

    /**
     * session Destroyed
     */
    @Override
    public void sessionDestroyed(HttpSessionEvent sessionEvent) {
        HttpSession session = sessionEvent.getSession();
        Authentication  authentication  = (Authentication ) session.getAttribute(WebConstants.AUTHENTICATION);
        Object principal  = authentication == null ? null : authentication.getPrincipal();
        log.trace("principal {}",principal);
        if(principal != null ) {
        	if(principal instanceof SignedPrincipal  signPrincipal && signPrincipal.getUserInfo() != null) {
        		log.trace("{} HttpSession Id  {} for userId  {} , username {} @Ticket {} Destroyed" ,
        			DateUtils.getCurrentDateTimeAsString(),
        			session.getId(), 
        			signPrincipal.getUserInfo().getId(),
        			signPrincipal.getUserInfo().getUsername(),
        			signPrincipal.getSessionId());
        	}else if(principal instanceof User user) {
        		log.trace("{} HttpSession Id  {} for username {} password {} Destroyed" ,
        			DateUtils.getCurrentDateTimeAsString(),
        			session.getId(), 
        			user.getUsername(),
        			user.getPassword());
        	}else{
        		log.trace("{} HttpSession Id  {} for principal {} Destroyed" ,
        			DateUtils.getCurrentDateTimeAsString(),
        			session.getId(), 
        			principal);
        	}
        }else {
        	log.trace("{} HttpSession Id  {} Destroyed" ,
        			DateUtils.getCurrentDateTimeAsString(),
        			session.getId());
        }
    }

}
