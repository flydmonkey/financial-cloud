package com.financial.cloud.service.history;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Date;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.financial.cloud.dto.auth.ChangePassword;
import com.financial.cloud.domain.config.Institutions;
import com.financial.cloud.domain.security.SocialsProvider;
import com.financial.cloud.domain.history.HistorySystemLogs;
import com.financial.cloud.domain.idm.Organizations;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.domain.idm.RoleMember;
import com.financial.cloud.domain.idm.Roles;
import com.financial.cloud.domain.permissions.Permission;
import com.financial.cloud.domain.permissions.Resources;
import com.financial.cloud.repository.history.HistorySystemLogsMapper;
import com.financial.cloud.service.history.HistorySystemLogsService;
import com.financial.cloud.util.JsonUtils;

import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Slf4j
@Service
public class HistorySystemLogsService  extends ServiceImpl<HistorySystemLogsMapper,HistorySystemLogs>{

	private final HistorySystemLogsMapper historySystemLogsMapper;

	public HistorySystemLogsMapper getMapper() {
		return historySystemLogsMapper;
	}

	public void log(String topic,Object entity,String action,String result,UserInfo operator) {
		String message = "";
		String targetId = "";
		String targetName = "";
		String cipherText = "";
		if(entity != null) {
			if(entity instanceof UserInfo model) {
				targetId = model.getId();
				targetName =  model.getUsername();
				message = buildMsg(model);
			}else if(entity instanceof Organizations model) {
				targetId = model.getId();
				targetName =  model.getOrgName();
				message = buildMsg(model);
			}else if(entity instanceof ChangePassword model) {
				targetId = model.getId();
				targetName =  model.getUsername();
				cipherText = model.getPassword();
				message = buildMsg(model);
			} else if(entity instanceof Roles model) {
				targetId = model.getId();
				targetName =  model.getRoleName();
				message = buildMsg(model);
			}else if(entity instanceof RoleMember model) {
				targetId = model.getId();
				targetName =  model.getRoleName();
				message = buildMsg(model);
			}else if(entity instanceof Resources model) {
				message = buildMsg(model);
			}else if(entity instanceof SocialsProvider model) {
				targetId = model.getId();
				targetName =  model.getProviderName();
				message = buildMsg(model);
			}else if(entity instanceof Permission model) {
				targetId = model.getId();
				targetName =  model.getRoleId();
				message = buildMsg(model);
			}else if(entity instanceof Institutions model) {
				targetId = model.getId();
				targetName =  model.getFullName();
				message = buildMsg(model);
			}else if(entity instanceof String) {
				message = entity.toString();
			}else {
				message = entity.toString();
			}

		}

		log(topic,targetId,targetName,cipherText,message,action,result,operator, entity);
	}

	public void log(String topic,String targetId,String targetName,String cipherText ,String message,String action,String result,UserInfo operator,Object entity) {
		HistorySystemLogs systemLog = new HistorySystemLogs();
		//systemLog.setId(systemLog.generateId());
		systemLog.setTargetId(targetId);
		systemLog.setTargetName(targetName);
		systemLog.setCipherText(cipherText);
		systemLog.setTopic(topic);
		systemLog.setMessage(message);
		systemLog.setMessageAction(action);
		systemLog.setMessageResult(result);
		systemLog.setUserId(operator.getId());
		systemLog.setUsername(operator.getUsername());
		systemLog.setDisplayName(operator.getDisplayName());
		systemLog.setBookId(operator.getBookId());
		systemLog.setJsonCotent(JsonUtils.toString(entity));
		systemLog.setExecuteTime(new Date());
		log.trace("System Log {}" ,systemLog);
		getMapper().insert(systemLog);
	}

	public String buildMsg(UserInfo userInfo) {
		return new StringBuilder()
				.append(userInfo.getDisplayName())
				.append("[")
				.append(userInfo.getUsername())
				.append("]")
				.toString();
	}

	public String buildMsg(Organizations org) {
		return new StringBuilder()
				.append(org.getOrgName())
				.append("[")
				.append(org.getOrgCode())
				.append("]")
				.toString();
	}

	public String buildMsg(ChangePassword changePassword) {
		return new StringBuilder()
				.append(changePassword.getDisplayName())
				.append("[")
				.append(changePassword.getUsername())
				.append("]")
				.toString();
	}

	public String buildMsg(Roles g) {
		return new StringBuilder()
				.append(g.getRoleName())
				.toString();
	}


	public String buildMsg(RoleMember rm) {
		return new StringBuilder()
				.append(rm.getRoleName())
				.append("[")
				.append(rm.getUsername()).append(",")
				.append(rm.getDisplayName())
				.append("]")
				.toString();
	}

	public String buildMsg(Permission privilege) {
		return new StringBuilder()
				.append(privilege.getRoleId())
				.append("[")
				.append(privilege.getResourceId())
				.append("]")
				.toString();
	}


	public String buildMsg(Resources r) {
		return new StringBuilder()
				.append(r.getResName())
				.append("[")
				.append(r.getClassify())
				.append("]")
				.toString();
	}


	public String buildMsg(SocialsProvider s) {
		return new StringBuilder()
				.append(s.getProviderName())
				.append("[")
				.append(s.getProvider())
				.append("]")
				.toString();
	}


	public String buildMsg(Institutions inst) {
		return new StringBuilder()
				.append(inst.getFullName())
				.append("[")
				.append(inst.getId())
				.append(inst.getDomain())
				.append("]")
				.toString();
	}
}
