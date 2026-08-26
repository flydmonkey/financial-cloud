package com.financial.cloud.repository.permissions;

import com.financial.cloud.repository.permissions.PermissionMapper;
import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.financial.cloud.domain.permissions.Permission;

public  interface PermissionMapper extends BaseMapper<Permission> {

    public int insertPermissions(List<Permission> permissionList);

    public int deletePermissions(List<Permission> permissionList);

    public List<Permission> queryPermissions(Permission permission);

}
