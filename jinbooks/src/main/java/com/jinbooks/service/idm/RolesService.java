package com.jinbooks.service.idm;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.jinbooks.service.config.InstitutionsService;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinbooks.constants.ConstsRoles;
import com.jinbooks.constants.ConstsStatus;
import com.jinbooks.domain.config.Institutions;
import com.jinbooks.domain.idm.Roles;
import com.jinbooks.repository.idm.RolesMapper;
import com.jinbooks.service.idm.RolesService;
import com.jinbooks.util.StrUtils;

@RequiredArgsConstructor
@Slf4j
@Repository
public class RolesService  extends ServiceImpl<RolesMapper,Roles> {

    private final RoleMemberService groupMemberService;

    private final InstitutionsService institutionsService;

    private final RolesMapper groupsMapper;

	public RolesMapper getMapper() {
		return groupsMapper;
	}


	public List<Roles> queryDynamicRoles(Roles groups){
	    return this.getMapper().queryDynamicRoles(groups);
	}

	public boolean deleteById(String groupId) {
	    this.removeById(groupId);
	    groupMemberService.deleteByRoleId(groupId);
	    return true;
	}

	public List<Roles> queryRolesByUserId(String userId){
		return this.getMapper().queryRolesByUserId(userId);
	}

	public void refreshDynamicRoles(Roles dynamicGroup){
	    if(dynamicGroup.getPattern().equals(ConstsRoles.Pattern.DYNAMIC)) {

	        if(StringUtils.isNotBlank(dynamicGroup.getOrgIdsList())) {
    	    	String []orgIds = dynamicGroup.getOrgIdsList().split(",");
    	    	StringBuffer orgIdFilters = new StringBuffer();
    	    	for(String orgId : orgIds) {
    	    		if(StringUtils.isNotBlank(orgId)) {
	    	    		if(orgIdFilters.length() > 0) {
	    	    			orgIdFilters.append(",");
	    	    		}
	    	    		orgIdFilters.append("'").append(orgId).append("'");
    	    		}
    	    	}
    	    	if(orgIdFilters.length() > 0) {
    	    		dynamicGroup.setOrgIdsList(orgIdFilters.toString());
    	    	}
    	    }

    	    String filters = dynamicGroup.getFilters();
    	    log.debug("filters {}" , filters);
    	    if(StringUtils.isNotBlank(filters)) {
	    		if(StrUtils.filtersSQLInjection(filters.toLowerCase())) {
	    			log.info("filters include SQL Injection Attack Risk.");
	    			return;
	    		}
	    		filters = filters.replace("&", " AND ").replace("\\|", " OR ");

	    		log.debug("set filters {}" , filters);
	    	    dynamicGroup.setFilters(filters);
    	    }

	    	groupMemberService.deleteDynamicRoleMember(dynamicGroup);
	    	groupMemberService.addDynamicRoleMember(dynamicGroup);
	    }
    }

	public void refreshAllDynamicRoles(){
		 LambdaQueryWrapper<Institutions> queryWrapper = new LambdaQueryWrapper<Institutions>();
		 queryWrapper.eq(Institutions::getStatus, ConstsStatus.ACTIVE);
		List<Institutions> instList = institutionsService.list(queryWrapper);
		for(Institutions inst : instList) {
			Roles group = new Roles();
		    List<Roles>  groupsList = queryDynamicRoles(group);
	        for(Roles r : groupsList) {
	            log.debug("group {}" , groupsList);
	            refreshDynamicRoles(r);
	        }
		}
	}

}
