/**
 *
 */
package com.jinbooks.repository.permissions;

import com.jinbooks.repository.permissions.PermissionMapper;
import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jinbooks.domain.permissions.Permission;

/**
 * @author Crystal.sea
 *
 */

public  interface PermissionMapper extends BaseMapper<Permission> {

    public int insertPermissions(List<Permission> permissionList);

    public int deletePermissions(List<Permission> permissionList);

    public List<Permission> queryPermissions(Permission permission);

}
