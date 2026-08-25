package com.jinbooks.repository.permissions;

import com.jinbooks.repository.permissions.AuthzResourceMapper;
import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jinbooks.dto.auth.QueryAppResourceDto;
import com.jinbooks.domain.idm.UserInfo;
import com.jinbooks.domain.permissions.Resources;

public  interface AuthzResourceMapper extends BaseMapper<UserInfo> {

	public List<Resources> queryResourcesByUserId(QueryAppResourceDto dto) ;

	public List<Resources> queryResourcesByRoleId(QueryAppResourceDto dto) ;

	public List<Resources> queryResourcesByOrgId(QueryAppResourceDto dto) ;


}
