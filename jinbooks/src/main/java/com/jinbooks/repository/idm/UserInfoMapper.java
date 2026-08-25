/*
 * Copyright [2025] [JinBooks of copyright http://www.jinbooks.com]
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
 





package com.jinbooks.repository.idm;

import com.jinbooks.repository.idm.UserInfoMapper;
import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinbooks.dto.idm.UserInfoPageDto;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jinbooks.constants.ConstsStatus;
import com.jinbooks.dto.auth.ChangePassword;
import com.jinbooks.domain.idm.Organizations;
import com.jinbooks.domain.idm.UserInfo;


/**
 * @author Crystal.Sea
 *
 */
public interface UserInfoMapper  extends BaseMapper<UserInfo>{

	Page<UserInfo> fetchPageResults(Page page, @Param("Dto") UserInfoPageDto dto);

	@Select("select * from  jbx_userinfo where deleted = 'n' and username = #{value} and status = " + ConstsStatus.ACTIVE)
	public UserInfo findByUsername(String username);

	@Select("select * from  jbx_userinfo where deleted = 'n' and ( email = #{value} or mobile= #{value} ) and status = " + ConstsStatus.ACTIVE)
	public UserInfo findByEmailMobile(String emailMobile);

	public List<Organizations> findOrganizationsByUserId(String userId);
	
	public void updateLocked(UserInfo userInfo);

	public void updateLockout(UserInfo userInfo);

	public int 	changePassword(ChangePassword changePassword);

	public int 	updateEmail(UserInfo userInfo);

	public int 	updateMobile(UserInfo userInfo);

	public int 	updateProfile(UserInfo userInfo);

    @Update("update jbx_userinfo set status =  #{status} where id = #{id}")
   	public int 	updateStatus(UserInfo userInfo) ;
    
    @Update("update jbx_userinfo set book_id =  #{bookId} where id = #{id}")
   	public int 	switchBook(UserInfo userInfo) ;
    
}
