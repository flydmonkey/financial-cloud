package com.financial.cloud.repository.idm;

import com.financial.cloud.repository.idm.UserInfoMapper;
import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.dto.idm.UserInfoPageDto;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.financial.cloud.constants.common.ConstsStatus;
import com.financial.cloud.dto.auth.ChangePassword;
import com.financial.cloud.domain.idm.Organizations;
import com.financial.cloud.domain.idm.UserInfo;


/**
 * @author Crystal.Sea
 *
 */
public interface UserInfoMapper  extends BaseMapper<UserInfo>{

	Page<UserInfo> fetchPageResults(Page page, @Param("Dto") UserInfoPageDto dto);

	@Select("select * from  userinfo where deleted = 'n' and username = #{value} and status = " + ConstsStatus.ACTIVE)
	public UserInfo findByUsername(String username);

	@Select("select * from  userinfo where deleted = 'n' and ( email = #{value} or mobile= #{value} ) and status = " + ConstsStatus.ACTIVE)
	public UserInfo findByEmailMobile(String emailMobile);

	public List<Organizations> findOrganizationsByUserId(String userId);
	
	public void updateLocked(UserInfo userInfo);

	public void updateLockout(UserInfo userInfo);

	public int 	changePassword(ChangePassword changePassword);

	public int 	updateEmail(UserInfo userInfo);

	public int 	updateMobile(UserInfo userInfo);

	public int 	updateProfile(UserInfo userInfo);

    @Update("update userinfo set status =  #{status} where id = #{id}")
   	public int 	updateStatus(UserInfo userInfo) ;
    
    @Update("update userinfo set book_id =  #{bookId} where id = #{id}")
   	public int 	switchBook(UserInfo userInfo) ;
    
}
