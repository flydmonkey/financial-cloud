package com.financial.cloud.controller.permissions;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.financial.cloud.authn.annotation.CurrentUser;
import com.financial.cloud.common.Message;
import com.financial.cloud.constants.auth.ProductRoles;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.domain.permissions.Permission;
import com.financial.cloud.dto.permissions.PermissionDto;
import com.financial.cloud.service.history.HistorySystemLogsService;
import com.financial.cloud.service.permissions.PermissionService;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping(value={"/api/permissions/permission"})
public class PermissionController {

	private final PermissionService permissionService;

	private final HistorySystemLogsService historySystemLogsService;

	private final IdentifierGenerator identifierGenerator;

	@PutMapping(value={"/update"}, produces = {MediaType.APPLICATION_JSON_VALUE})
	public Message<Permission> update(
			@RequestBody PermissionDto dto,
			@CurrentUser UserInfo currentUser) {
		ProductRoles.requireAdministrator();
		log.debug("-update  : {}" , dto);
		//have
		Permission queryPermission =
				new Permission(
						dto.roleId(),
						currentUser.getBookId());
		List<Permission> permissionsList = permissionService.queryPermissions(queryPermission);

		HashMap<String,String >permissionsMap =new HashMap<>();
		for(Permission permission : permissionsList) {
			permissionsMap.put(permission.getUniqueId(),permission.getId());
		}
		//Maybe insert
		ArrayList<Permission> newPermissionsList =new ArrayList<>();
		HashMap<String,String >newPermissionsMap =new HashMap<>();
		for(String resourceId : dto.resourceIds()) {
		    Permission newPermission=new Permission(
		    		dto.roleId(),
                    resourceId,
                    currentUser.getId(),
                    currentUser.getBookId());
		    newPermission.setId(identifierGenerator.nextId(newPermission).toString());
		    newPermissionsMap.put(newPermission.getUniqueId(), dto.appId());

		    if(!dto.appId().equalsIgnoreCase(resourceId) &&
		            !permissionsMap.containsKey(newPermission.getUniqueId())) {
		    	newPermissionsList.add(newPermission);
		    }
		}

		//delete
		ArrayList<Permission> deletePermissionsList =new ArrayList<>();
		for(Permission deletePermission : permissionsList) {
           if(!newPermissionsMap.containsKey(deletePermission.getUniqueId())) {
        	   deletePermission.setBookId(currentUser.getBookId());
        	   deletePermissionsList.add(deletePermission);
           }
        }
		if (!deletePermissionsList.isEmpty()) {
			log.debug("-remove  : {}" , deletePermissionsList);
			permissionService.deleteGroupPrivileges(deletePermissionsList);
		}

		if (!newPermissionsList.isEmpty() && permissionService.insertGroupPrivileges(newPermissionsList)) {
			log.debug("-insert  : {}" , newPermissionsList);
			return new Message<>(Message.SUCCESS);

		} else {
			return new Message<>(Message.SUCCESS);
		}

	}

    @GetMapping(value={"/get"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public Message<List<Permission>> get(
    		@ModelAttribute Permission permission,
    		@CurrentUser UserInfo currentUser) {
        log.debug("-get  : {}" , permission);
        //have
        Permission queryPermission =
        		new Permission(
        				permission.getRoleId(),
        				currentUser.getBookId());

        List<Permission> queryPermissionList = permissionService.queryPermissions(queryPermission);

        return new Message<>(queryPermissionList);
	}


}
