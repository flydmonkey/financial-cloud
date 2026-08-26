package com.financial.cloud.service.permissions;


import lombok.RequiredArgsConstructor;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.financial.cloud.domain.permissions.SessionList;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.repository.permissions.SessionListMapper;
import com.financial.cloud.context.WebContext;

import lombok.extern.slf4j.Slf4j;

import org.springframework.core.task.TaskExecutor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Slf4j
@Service
public class SessionListService  extends ServiceImpl<SessionListMapper,SessionList>{

	private final SessionListMapper sessionListMapper;

	@Qualifier("historyTaskExecutor")
	private final TaskExecutor historyTaskExecutor;

	public SessionListMapper getMapper() {
		return sessionListMapper;
	}


    public void insertOnline(SessionList sessionList) {
    	sessionList.setId(WebContext.genId());
		historyTaskExecutor.execute(() -> {
			log.debug(" sessionList {}" , sessionList);
			sessionList.setOperateTime(new Date());
			save(sessionList);
		});
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
}
