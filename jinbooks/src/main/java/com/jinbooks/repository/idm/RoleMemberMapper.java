/*
 * Copyright [2025] [JinBooks of copyright http://www.jinbooks.com]
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
 





/**
 *
 */
package com.jinbooks.repository.idm;

import com.jinbooks.repository.idm.RoleMemberMapper;
import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinbooks.domain.idm.UserInfo;
import com.jinbooks.domain.idm.RoleMember;
import com.jinbooks.domain.idm.Roles;
import com.jinbooks.dto.idm.RoleMemberPageDto;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * @author Crystal.sea
 *
 */

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
