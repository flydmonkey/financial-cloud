package com.financial.cloud.repository.permissions;

import com.financial.cloud.repository.permissions.AuthzResourceMapper;
import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.financial.cloud.dto.auth.QueryAppResourceDto;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.domain.permissions.Resources;

public  interface AuthzResourceMapper extends BaseMapper<UserInfo> {

	public List<Resources> queryResourcesByUserId(QueryAppResourceDto dto) ;

	public List<Resources> queryResourcesByRoleId(QueryAppResourceDto dto) ;

	public List<Resources> queryResourcesByOrgId(QueryAppResourceDto dto) ;


}
