package com.financial.cloud.dto.auth;

import java.util.ArrayList;
import java.util.List;

public class QueryAppResourceDto {
	
	String userId;
	
	List<String> orgIds;
	
	
	List<String> roleIds;

	public QueryAppResourceDto(String userId) {
		super();
		this.userId = userId;
		roleIds = new ArrayList<>();
		orgIds = new ArrayList<>();
	}


	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public List<String> getOrgIds() {
		return orgIds;
	}

	public void setOrgIds(List<String> orgIds) {
		this.orgIds = orgIds;
	}

	public List<String> getRoleIds() {
		return roleIds;
	}

	public void setRoleIds(List<String> roleIds) {
		this.roleIds = roleIds;
	}

}
