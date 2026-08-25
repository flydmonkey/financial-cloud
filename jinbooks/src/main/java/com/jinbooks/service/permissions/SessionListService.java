package com.jinbooks.service.permissions;


import lombok.RequiredArgsConstructor;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinbooks.domain.permissions.SessionList;
import com.jinbooks.domain.idm.UserInfo;
import com.jinbooks.repository.permissions.SessionListMapper;
import com.jinbooks.service.permissions.SessionListService;
import com.jinbooks.context.WebContext;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Slf4j
@Service
public class SessionListService  extends ServiceImpl<SessionListMapper,SessionList>{

	private final SessionListMapper sessionListMapper;

	public SessionListMapper getMapper() {
		return sessionListMapper;
	}


    public void insertOnline(SessionList sessionList) {
    	sessionList.setId(WebContext.genId());

        new Thread(new SessionInsertRunnable(this,sessionList)).start();
    }

	public SessionList getBySessionId(String sessionId) {
		return getMapper().getBySessionId(sessionId);
	}

	public void removeById(String sessionId) {
		getMapper().removeById(sessionId);
	}

	public void updateLastLogoffTime(String userId) {
		UserInfo user = new UserInfo();
		user.setId(userId);
		user.setLastLogoffTime(new Date());
		getMapper().updateLastLogoffTime(user);
	}

	public List<SessionList> list(String style) {
		if(StringUtils.isBlank(style)){
			return getMapper().listAll();
		}else {
			return getMapper().listByStyle(style);
		}
	}

	public class SessionInsertRunnable implements Runnable{

		SessionListService service;

		SessionList sessionList;

		public SessionInsertRunnable(SessionListService sessionListService, SessionList sessionList) {
			super();
			this.service = sessionListService;
			this.sessionList = sessionList;
		}
	    public void run() {
			log.debug(" sessionList {}" , sessionList);
			sessionList.setOperateTime(new Date());
			service.save(sessionList);
		}
	}

}
