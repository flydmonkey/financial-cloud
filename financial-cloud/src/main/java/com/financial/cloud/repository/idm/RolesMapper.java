package com.financial.cloud.repository.idm;

import com.financial.cloud.repository.idm.RolesMapper;
import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.financial.cloud.domain.idm.Roles;

public  interface RolesMapper extends BaseMapper<Roles> {

    public List<Roles> queryDynamicRoles(Roles roles);

    public List<Roles> queryRolesByUserId(String userId);
}
