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
 





package com.jinbooks.controller.idm;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinbooks.authn.annotation.CurrentUser;
import com.jinbooks.authn.support.AuthorizationUtils;
import com.jinbooks.constants.ConstsRoles;
import com.jinbooks.common.Message;
import com.jinbooks.domain.idm.UserInfo;
import com.jinbooks.domain.idm.RoleMember;
import com.jinbooks.domain.idm.Roles;
import com.jinbooks.dto.idm.RoleMemberDto;
import com.jinbooks.dto.idm.RoleMemberPageDto;
import com.jinbooks.dto.idm.RoleMemberUserGroupsDto;
import com.jinbooks.service.idm.RoleMemberService;
import com.jinbooks.service.idm.RolesService;
import com.jinbooks.service.history.HistorySystemLogsService;
import com.jinbooks.service.idm.UserInfoService;
import com.jinbooks.context.WebContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping(value={"/idm/groupmembers"})
public class RoleMemberController {
	static final Logger logger = LoggerFactory.getLogger(RoleMemberController.class);

	@Autowired
	RoleMemberService groupMemberService;

	@Autowired
	RolesService groupsService;

	@Autowired
	UserInfoService userInfoService;

	@Autowired
	HistorySystemLogsService historySystemLogsService;

	@GetMapping(value = { "/fetch" }, produces = {MediaType.APPLICATION_JSON_VALUE})
	public Message<Page<RoleMember>> fetch(
			RoleMemberPageDto dto,
			@CurrentUser UserInfo currentUser) {
		logger.debug("fetch {}",dto);
		LambdaQueryWrapper<RoleMember> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(RoleMember::getBookId, currentUser.getBookId());
		if(AuthorizationUtils.getAuthentication().getAuthorities().contains(ConstsRoles.ROLE_MANAGER)){
			logger.debug("Has ROLE_MANAGERS {}" ,currentUser.getId());
			wrapper.eq(RoleMember::getGradingUserId, currentUser.getId());
		}
		return new Message<>(Message.SUCCESS, groupMemberService.page(dto.build(), wrapper));
	}

	@GetMapping(value = { "/memberInGroup" })
	public Message<Page<RoleMember>> memberInGroup(RoleMemberPageDto dto,
													@CurrentUser UserInfo currentUser) {
		logger.debug("groupMember : {}",dto);
		dto.setBookId(currentUser.getBookId());
		return new Message<>(Message.SUCCESS, groupMemberService.memberInRole(dto.build(), dto));
	}

	@GetMapping(value = { "/memberNotInGroup" })
	public Message<Page<RoleMember>> memberNotInGroup(RoleMemberPageDto dto,
													   @CurrentUser UserInfo currentUser) {
		dto.setBookId(currentUser.getBookId());

		return new Message<>(groupMemberService.memberNotInRole(dto.build(), dto));
	}

	@GetMapping(value = { "/groupsNoMember" })
	public Message<Page<Roles>> groupsNoMember(RoleMemberPageDto dto, @CurrentUser UserInfo currentUser) {
		dto.setBookId(currentUser.getBookId());
		return new Message<>(Message.SUCCESS, groupMemberService.rolesNoMember(dto.build(), dto));
	}

	/**
	 * Members add to the Group
	 * @param currentUser
	 * @return
	 */
	@PostMapping(value = {"/add"})
	public Message<RoleMember> addGroupMember(@Validated @RequestBody RoleMemberDto dto,@CurrentUser UserInfo currentUser) {
		boolean result = true;
		for (int i = 0; i < dto.getMemberIds().size(); i++) {
			RoleMember newGroupMember =
					new RoleMember(
							dto.getRoleId(),
							dto.getMemberIds().get(i),
							dto.getType(),
							currentUser.getBookId());
			newGroupMember.setId(WebContext.genId());
			result = groupMemberService.save(newGroupMember);
		}
		if(result) {
			return new Message<>(Message.SUCCESS);
		}
		return new Message<>(Message.FAIL);
	}


	/**
	 * Member add to Groups
	 * @param currentUser
	 * @return
	 */
	@PostMapping(value = {"/addMember2Groups"})
	public Message<RoleMember> addMember2Groups(@Validated @RequestBody RoleMemberUserGroupsDto dto, @CurrentUser UserInfo currentUser) {
		UserInfo userInfo = userInfoService.findByUsername(dto.getUsername());

		boolean result = true;
		for (int i = 0; i < dto.getGroupIds().size(); i++) {
			RoleMember newGroupMember =
					new RoleMember(
							dto.getGroupIds().get(i),
							userInfo.getId(),
							"USER",
							currentUser.getBookId());
			newGroupMember.setId(WebContext.genId());
			result = groupMemberService.save(newGroupMember);
		}
		if(result) {
			return new Message<>(Message.SUCCESS);
		}
		return new Message<>(Message.FAIL);
	}

	@DeleteMapping(value={"/delete"}, produces = {MediaType.APPLICATION_JSON_VALUE})
	public Message<RoleMember> delete(@RequestParam("ids") List<String> ids,@CurrentUser UserInfo currentUser) {
		logger.debug("-delete ids : {}" , ids);
		if (groupMemberService.removeBatchByIds(ids)) {
			 return new Message<>(Message.SUCCESS);
		} else {
			return new Message<>(Message.FAIL);
		}
	}
}
