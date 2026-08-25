package com.jinbooks.service.permissions;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinbooks.domain.permissions.Permission;
import com.jinbooks.repository.permissions.PermissionMapper;
import com.jinbooks.service.permissions.PermissionService;

@RequiredArgsConstructor
@Slf4j
@Repository
public class PermissionService  extends ServiceImpl<PermissionMapper,Permission>{

	private final PermissionMapper permissionMapper;

	public PermissionMapper getMapper() {
		return permissionMapper;
	}

	public boolean insertGroupPrivileges(List<Permission> permissionsList) {
	    return getMapper().insertPermissions(permissionsList)>0;
	}

	public boolean deleteGroupPrivileges(List<Permission> permissionsList) {
	     return getMapper().deletePermissions(permissionsList)>=0;
	 }

    public List<Permission> queryPermissions(Permission rolePermissions){
        return getMapper().queryPermissions(rolePermissions);
    }

}
