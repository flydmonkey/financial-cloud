package com.jinbooks.repository.permissions;

import com.jinbooks.repository.permissions.AuthzMapper;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jinbooks.dto.auth.QueryGroupMembersDto;
import com.jinbooks.domain.idm.UserInfo;
import com.jinbooks.domain.idm.Roles;

@Mapper
public  interface AuthzMapper extends BaseMapper<UserInfo> {


	public List<Roles> queryRolesByMembers(QueryGroupMembersDto dto) ;
	
	@Select("select * from  jbx_userinfo where id = #{userId} and deleted = 'n'")
	public UserInfo findUserById(@Param ("userId") String userId ) ;

}
