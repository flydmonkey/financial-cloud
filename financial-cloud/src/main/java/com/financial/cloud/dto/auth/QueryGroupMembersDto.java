package com.financial.cloud.dto.auth;

import java.util.ArrayList;
import java.util.List;


public class QueryGroupMembersDto {
	
	List<String> members;

	public QueryGroupMembersDto() {
		members = new ArrayList<>();
	}

	public QueryGroupMembersDto(List<String> members) {
		this.members = members;
	}
	
	public void add(String memberId) {
		this.members.add(memberId);
	}

	public List<String> getMembers() {
		return members;
	}

	public void setMembers(List<String> members) {
		this.members = members;
	}
	
}
