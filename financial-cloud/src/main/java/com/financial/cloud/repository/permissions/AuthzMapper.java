package com.financial.cloud.repository.permissions;

import com.financial.cloud.repository.permissions.AuthzMapper;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.financial.cloud.dto.auth.QueryGroupMembersDto;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.domain.idm.Roles;

@Mapper
public  interface AuthzMapper extends BaseMapper<UserInfo> {


	public List<Roles> queryRolesByMembers(QueryGroupMembersDto dto) ;
	
	@Select("select * from  userinfo where id = #{userId} and deleted = 'n'")
	public UserInfo findUserById(@Param ("userId") String userId ) ;

}
