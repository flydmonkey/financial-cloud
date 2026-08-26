package com.financial.cloud.repository.permissions;

import com.financial.cloud.repository.permissions.SessionListMapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.financial.cloud.domain.permissions.SessionList;
import com.financial.cloud.domain.idm.UserInfo;

@Mapper
public interface SessionListMapper extends BaseMapper<SessionList> {

	@Select("select * from session_list  where session_id = #{sessionId}")
	public SessionList getBySessionId(@Param ("sessionId") String sessionId) ;

	@Update("delete from session_list  where session_id = #{sessionId}")
	public void removeById(@Param ("sessionId") String sessionId);

	@Update("update userinfo set last_logoff_time = #{lastLogoffTime} , is_online = 0  where id = #{id}")
	public void updateLastLogoffTime(UserInfo user);

	@Select("select * from session_list")
	public List<SessionList> listAll() ;

	@Select("select * from session_list where style = #{style}")
	public List<SessionList> listByStyle(@Param ("style") String style) ;


}
