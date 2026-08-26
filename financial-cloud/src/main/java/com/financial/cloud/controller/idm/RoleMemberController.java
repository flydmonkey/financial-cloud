package com.financial.cloud.controller.idm;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.authn.support.AuthorizationUtils;
import com.financial.cloud.constants.auth.ConstsRoles;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.domain.idm.RoleMember;
import com.financial.cloud.domain.idm.Roles;
import com.financial.cloud.dto.idm.RoleMemberDto;
import com.financial.cloud.dto.idm.RoleMemberPageDto;
import com.financial.cloud.dto.idm.RoleMemberUserGroupsDto;
import com.financial.cloud.service.idm.RoleMemberService;
import com.financial.cloud.service.idm.RolesService;
import com.financial.cloud.service.history.HistorySystemLogsService;
import com.financial.cloud.service.idm.UserInfoService;
import com.financial.cloud.context.WebContext;

import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping(value={"/api/idm/groupmembers"})
public class RoleMemberController {

	private final RoleMemberService groupMemberService;

	private final RolesService groupsService;

	private final UserInfoService userInfoService;

	private final HistorySystemLogsService historySystemLogsService;

	@GetMapping(value = { "/fetch" }, produces = {MediaType.APPLICATION_JSON_VALUE})
	public Message<Page<RoleMember>> fetch(
			RoleMemberPageDto dto,
			@CurrentUser UserInfo currentUser) {
		log.debug("fetch {}",dto);
		LambdaQueryWrapper<RoleMember> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(RoleMember::getBookId, currentUser.getBookId());
		if(AuthorizationUtils.getAuthentication().getAuthorities().contains(ConstsRoles.ROLE_MANAGER)){
			log.debug("Has ROLE_MANAGERS {}" ,currentUser.getId());
			wrapper.eq(RoleMember::getGradingUserId, currentUser.getId());
		}
		return new Message<>(Message.SUCCESS, groupMemberService.page(dto.build(), wrapper));
	}

	@GetMapping(value = { "/memberInGroup" })
	public Message<Page<RoleMember>> memberInGroup(RoleMemberPageDto dto,
													@CurrentUser UserInfo currentUser) {
		log.debug("groupMember : {}",dto);
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
		log.debug("-delete ids : {}" , ids);
		if (groupMemberService.removeBatchByIds(ids)) {
			 return new Message<>(Message.SUCCESS);
		} else {
			return new Message<>(Message.FAIL);
		}
	}
}
