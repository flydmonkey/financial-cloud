package com.jinbooks.service.idm;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinbooks.domain.idm.UserInfo;
import com.jinbooks.domain.idm.RoleMember;
import com.jinbooks.domain.idm.Roles;
import com.jinbooks.dto.idm.RoleMemberPageDto;
import com.jinbooks.repository.idm.RoleMemberMapper;
import com.jinbooks.service.idm.RoleMemberService;

import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Slf4j
@Service
public class RoleMemberService  extends ServiceImpl<RoleMemberMapper,RoleMember>{

	private final RoleMemberMapper groupMemberMapper;

	public RoleMemberMapper getMapper() {
		return groupMemberMapper;
	}

	public int addDynamicRoleMember(Roles dynamicGroup) {
	    return getMapper().addDynamicRoleMember(dynamicGroup);
	}

	public int deleteDynamicRoleMember(Roles dynamicGroup) {
	    return getMapper().deleteDynamicRoleMember(dynamicGroup);
	}

	public int deleteByRoleId(String groupId) {
        return getMapper().deleteByRoleId(groupId);
    }
	public List<UserInfo> queryMemberByRoleId(String groupId){
		return getMapper().queryMemberByRoleId(groupId);
	}
	public Page<Roles> rolesNoMember(Page page, RoleMemberPageDto dto) {
		return groupMemberMapper.rolesNoMember(page, dto);
	}
	public Page<RoleMember> memberInRole(Page page, RoleMemberPageDto dto) {
		return groupMemberMapper.memberInRole(page, dto);
	}
	public Page<RoleMember> memberNotInRole(Page page, RoleMemberPageDto dto) {
		return groupMemberMapper.memberNotInRole(page, dto);
	}

}
