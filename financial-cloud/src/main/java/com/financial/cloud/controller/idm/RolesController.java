package com.financial.cloud.controller.idm;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Arrays;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.constants.auth.ProductRoles;
import com.financial.cloud.constants.common.ConstsAct;
import com.financial.cloud.constants.common.ConstsActResult;
import com.financial.cloud.constants.common.ConstsEntryType;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.domain.idm.Roles;
import com.financial.cloud.dto.idm.RolesPageDto;
import com.financial.cloud.service.idm.RolesService;
import com.financial.cloud.service.history.HistorySystemLogsService;
import com.financial.cloud.validation.AddGroup;
import com.financial.cloud.validation.EditGroup;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping(value={"/api/idm/groups"})
public class RolesController {

	private final RolesService groupsService;

	private final HistorySystemLogsService historySystemLogsService;

	private final IdentifierGenerator identifierGenerator;

	@GetMapping(value = { "/fetch" }, produces = {MediaType.APPLICATION_JSON_VALUE})
	public Message<Page<Roles>> fetch(
			RolesPageDto dto,
			@CurrentUser UserInfo currentUser) {

		LambdaQueryWrapper<Roles> wrapper = new LambdaQueryWrapper<>();

		if (StringUtils.isNotEmpty(dto.getRoleName())) {
			wrapper.like(Roles::getRoleName, dto.getRoleName());
		}
		return new Message<>(Message.SUCCESS, groupsService.page(dto.build(), wrapper));
	}

	@GetMapping(value={"/query"}, produces = {MediaType.APPLICATION_JSON_VALUE})
	public Message<Roles> query(@ModelAttribute Roles group,@CurrentUser UserInfo currentUser) {
		log.debug("-query  : {}" , group);
		LambdaQueryWrapper<Roles> wrapper = new LambdaQueryWrapper<>();
		if (ObjectUtils.isNotEmpty(groupsService.list(wrapper))) {
			 return new Message<>(Message.SUCCESS);
		} else {
			 return new Message<>(Message.FAIL);
		}

	}

	@GetMapping(value = { "/get/{id}" }, produces = {MediaType.APPLICATION_JSON_VALUE})
	public Message<Roles> get(@PathVariable("id") String id,@CurrentUser UserInfo currentUser) {
		Roles group = groupsService.getById(id);
		return new Message<>(group);
	}

	@PostMapping(value={"/add"}, produces = {MediaType.APPLICATION_JSON_VALUE})
	public Message<Roles> insert(@Validated(value = AddGroup.class) @RequestBody Roles group, @CurrentUser UserInfo currentUser) {
		ProductRoles.requireAdministrator();
		log.debug("-Add  : {}" , group);
		group.setId(identifierGenerator.nextId("groups").toString());
		if(StringUtils.isBlank(group.getRoleCode())) {
			group.setRoleCode(group.getId());
		}
		group.setCreatedBy(currentUser.getId());
		if (groupsService.save(group)) {
			groupsService.refreshDynamicRoles(group);
		    historySystemLogsService.log(
					ConstsEntryType.ROLE,
					group,
					ConstsAct.CREATE,
					ConstsActResult.SUCCESS,
					currentUser);
		    return new Message<>(Message.SUCCESS);
		} else {
			return new Message<>(Message.FAIL);
		}
	}

	@PutMapping(value={"/update"}, produces = {MediaType.APPLICATION_JSON_VALUE})
	public Message<Roles> update(@Validated(value = EditGroup.class) @RequestBody Roles group,@CurrentUser UserInfo currentUser) {
		ProductRoles.requireAdministrator();
		log.debug("-update  group : {}" , group);
		if(group.getId().equalsIgnoreCase("ROLE_ALL_USER")) {
			group.setDefaultAllUser();
		}
		group.setModifiedBy(currentUser.getId());
		if (groupsService.updateById(group)) {
			groupsService.refreshDynamicRoles(group);
		    historySystemLogsService.log(
					ConstsEntryType.ROLE,
					group,
					ConstsAct.UPDATE,
					ConstsActResult.SUCCESS,
					currentUser);
		    return new Message<>(Message.SUCCESS);
		} else {
			return new Message<>(Message.FAIL);
		}
	}

	@DeleteMapping(value={"/delete"}, produces = {MediaType.APPLICATION_JSON_VALUE})
	public Message<Roles> delete(@RequestParam("ids") List<String> ids,@CurrentUser UserInfo currentUser) {
		ProductRoles.requireAdministrator();
		log.debug("-delete ids : {}" , ids);
		ids.removeAll(Arrays.asList("ROLE_ALL_USER","ROLE_ADMINISTRATORS","-1"));
		if (groupsService.removeByIds(ids)) {
			historySystemLogsService.log(
					ConstsEntryType.ROLE,
					ids,
					ConstsAct.DELETE,
					ConstsActResult.SUCCESS,
					currentUser);
			 return new Message<>(Message.SUCCESS);
		} else {
			return new Message<>(Message.FAIL);
		}
	}
}
