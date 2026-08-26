package com.financial.cloud.authn.session;

import java.time.LocalDateTime;
import java.util.List;

import com.financial.cloud.domain.permissions.SessionList;

/**
 * 会话管理接口
 * 
 * @author Crystal.Sea
 *
 */
public interface SessionManager {

	public  void create(String sessionId, Session session);
	
    public  Session remove(String sessionId);
    
    public  Session get(String sessionId);
   
    public Session refresh(String sessionId ,LocalDateTime refreshTime);
    
    public Session refresh(String sessionId);
    
    public List<SessionList> sessionList(String style);
    
    public int getValiditySeconds();
    
    public void terminate(String sessionId,String userId,String username);
    
    public void terminateByUserId(String userId);
    
    public void setLimit(int sessionLimit);
    
    public void put(String style,String userId, String sessionKey) ;
    
}
