/**
 *
 */
package com.jinbooks.repository.idm;

import com.jinbooks.repository.idm.RolesMapper;
import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jinbooks.domain.idm.Roles;

/**
 * @author Crystal.sea
 *
 */

public  interface RolesMapper extends BaseMapper<Roles> {

    public List<Roles> queryDynamicRoles(Roles roles);

    public List<Roles> queryRolesByUserId(String userId);
}
