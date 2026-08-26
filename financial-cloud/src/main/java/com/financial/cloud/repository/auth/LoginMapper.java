/**
 *
 */
package com.financial.cloud.repository.auth;

import com.financial.cloud.repository.auth.LoginMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.financial.cloud.domain.idm.UserInfo;

/**
 * @author Crystal.sea
 *
 */

public  interface LoginMapper extends BaseMapper<UserInfo> {

	@Select("select * from  userinfo where  id = #{id} and  deleted = 'n' ")
	public UserInfo findById(@Param ("id") String id ) ;

	@Select("select * from  userinfo where deleted = 'n' and (username = #{username} or mobile = #{username})")
	public UserInfo findByUsername(@Param ("username") String username ) ;

	@Update("""
			update userinfo set  
	            bad_password_count    = 0 , 
	            login_failed_count    = 0 ,  
	            login_count          = login_count + 1, 
	            last_login_time       = #{lastLoginTime} , 
	            last_login_ip         = #{lastLoginIp} , 
	            is_online            = 1
			where id = #{id}  
        """)
	public void updateLastLogin(UserInfo userInfo);


	@Update("""
            update userinfo 
			    set is_locked 	= #{isLocked} , 
			    un_lock_time 		= #{unLockTime} 
			where id = #{id}
        """)
	public void updateLockUser(UserInfo userInfo);

	@Update("""
            update userinfo 
			set bad_password_count = 0 , 
				login_failed_count = 0 ,
				is_locked 		 = #{isLocked} , 
			    un_lock_time 		 = #{unLockTime} 
			where id = #{id}
        """)
	public void updateUnLockUser(UserInfo userInfo);


	@Update("""
           update userinfo set 
                bad_password_count = bad_password_count + 1 , 
                bad_password_time  = #{badPasswordTime} ,
                login_failed_count = login_failed_count + 1 , 
                last_login_time  = #{loginFailedTime}
           where id = #{id}
        """)
	public void updateBadPasswordCount(UserInfo userInfo);

	@Update("""
	      update userinfo set 
				login_failed_count = login_failed_count + 1 , 
				last_login_time  = #{loginFailedTime}
		  where id = #{id}
	        """)
	public void updateLoginFailedCount(UserInfo userInfo);

	@Update("""
		  update userinfo set 
			  	bad_password_count = 0 , 
				bad_password_time = #{badPasswordTime} ,
                login_failed_count = 0 , 
                last_login_time = #{loginFailedTime}
		  where id = #{id}
	        """)
	public void updateLoginFailedCountRest(UserInfo userInfo);

}
