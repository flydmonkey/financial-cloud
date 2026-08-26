package com.financial.cloud.repository.idm;

import com.financial.cloud.repository.idm.RoleMemberMapper;
import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.domain.idm.RoleMember;
import com.financial.cloud.domain.idm.Roles;
import com.financial.cloud.dto.idm.RoleMemberPageDto;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@Mapper
public  interface RoleMemberMapper extends BaseMapper<RoleMember> {

	Page<RoleMember> memberInRole(Page page, @Param("Dto") RoleMemberPageDto dto);

	Page<RoleMember> memberNotInRole(Page page, @Param("Dto") RoleMemberPageDto dto);

	Page<Roles> rolesNoMember(Page page, @Param("Dto") RoleMemberPageDto dto);

	public int addDynamicRoleMember(Roles dynamicRole);

	public int deleteDynamicRoleMember(Roles dynamicRole);

	public int deleteByRoleId(String roleId);

	public List<UserInfo> queryMemberByRoleId(String roleId);



}
