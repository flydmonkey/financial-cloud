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
 





package com.jinbooks.service.auth.impl;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinbooks.constants.ConstsStatus;
import com.jinbooks.dto.auth.ChangePassword;
import com.jinbooks.domain.permissions.SessionList;
import com.jinbooks.dto.book.BookVo;
import com.jinbooks.domain.security.ConfigLoginPolicy;
import com.jinbooks.domain.history.HistoryLogin;
import com.jinbooks.domain.idm.UserInfo;
import com.jinbooks.domain.permissions.Resources;
import com.jinbooks.repository.auth.LoginMapper;
import com.jinbooks.service.permissions.AuthzResourceService;
import com.jinbooks.service.permissions.AuthzService;
import com.jinbooks.service.book.BookService;
import com.jinbooks.service.security.ConfigLoginPolicyService;
import com.jinbooks.service.auth.FileStorageService;
import com.jinbooks.service.history.HistoryLoginService;
import com.jinbooks.service.auth.LoginService;
import com.jinbooks.service.permissions.SessionListService;
import com.jinbooks.service.idm.UserInfoService;
import com.jinbooks.util.DateUtils;
import com.jinbooks.context.WebConstants;
import com.jinbooks.context.WebContext;

@Repository
public class LoginServiceImpl  extends ServiceImpl<LoginMapper,UserInfo> implements LoginService {
	private static final Logger logger = LoggerFactory.getLogger(LoginServiceImpl.class);

	@Autowired
	LoginMapper loginMapper;

	@Autowired
    UserInfoService userInfoService;

	@Autowired
    ConfigLoginPolicyService configLoginPolicyService;

	@Autowired
    HistoryLoginService historyLoginService;

	@Autowired
    SessionListService sessionListService;

    @Autowired
    FileStorageService fileStorageService;

    @Autowired
    AuthzService authzService;

    @Autowired
    AuthzResourceService authzResourceService;
    
    @Autowired
    BookService bookService;

	public LoginMapper getMapper() {
		return loginMapper;
	}

    @Override
	public void updateLastLogin(UserInfo userInfo) {
		this.getMapper().updateLastLogin(userInfo);
	}

    @Override
	public UserInfo findById(String userId) {
		return this.getMapper().findById(userId);
	}

    @Override
	public UserInfo findByUsername(String loginName) {
    	UserInfo userInfo = this.getMapper().findByUsername(loginName);
        if(StringUtils.isBlank(userInfo.getBookId())) {
        	//未设置默认账号情况，读取有权限的账套的第一个
        	List<BookVo> books = bookService.listBooks(userInfo.getId());
        	if(CollectionUtils.isNotEmpty(books)) {
        		userInfo.setBookId(books.get(0).getId());
        		userInfoService.switchBook(userInfo);
        	}else {
        		userInfo.setBookId(userInfo.getId());
        	}
        }
		return userInfo;
	}

    @Override
	public List<GrantedAuthority> grantAuthority(UserInfo userInfo) {
		return authzService.grantAuthority(userInfo);
	}

     @Override
	 public Set<Resources> getResourcesBySubject(UserInfo user){
		 return authzResourceService.getResourcesBySubject(user);
	 }


    /**
     * dynamic ConfigLoginPolicy Valid for user login.
     * @param userInfo
     * @return boolean
     */
    @Override
    public boolean applyLoginPolicy(UserInfo userInfo) {

    	ConfigLoginPolicy configLoginPolicy = configLoginPolicyService.getConfigLoginPolicy();

        Date currentdateTime = new Date();
         /*
          * check login attempts fail times
          */
         if (userInfo.getLoginFailedCount() >= configLoginPolicy.getLoginAttempts() && userInfo.getLoginFailedTime() != null) {
             logger.debug("login Attempts is {}" , userInfo.getLoginFailedCount());
             Date loginFailedTime = userInfo.getLoginFailedTime();
             //duration
             logger.trace("Login Failed Time {}" , DateUtils.formatDateTime(loginFailedTime));

             long intDuration = (currentdateTime.getTime() - loginFailedTime.getTime()) / (60 * 1000);
             logger.debug("Login Failed duration {} , " +
                           "Login policy Duration {} , "+
                           "validate result {}" ,
                           intDuration,
                           configLoginPolicy.getLockInterval(),
                           (intDuration > configLoginPolicy.getLockInterval())
                     );
             //auto unlock attempts when intDuration >= set Duration
             if(intDuration >= configLoginPolicy.getLockInterval()) {
                 logger.debug("resetAttempts ...");
                 updateUnlockUser(userInfo);
             }else {
            	 updateLockUser(userInfo);
                 throw new BadCredentialsException(
                         WebContext.getI18nValue("login.error.attempts",
                                 new Object[]{userInfo.getLoginFailedCount(),configLoginPolicy.getLockInterval()})
                         );
             }
         }

         //locked
         if(userInfo.getIsLocked()==ConstsStatus.LOCK) {
             throw new BadCredentialsException(
                                 userInfo.getUsername()+ " "+
                                 WebContext.getI18nValue("login.error.locked")
                                 );
         }
         // inactive
         if(userInfo.getStatus()!=ConstsStatus.ACTIVE) {
             throw new BadCredentialsException(
                                 userInfo.getUsername()+
                                 WebContext.getI18nValue("login.error.inactive")
                                 );
         }

         return true;
     }

    /**
     * lockUser
     *
     * @param userInfo
     */
    public void updateLockUser(UserInfo userInfo) {
        try {
            if (userInfo != null && StringUtils.isNotEmpty(userInfo.getId())&&userInfo.getIsLocked() == ConstsStatus.ACTIVE) {
            	userInfo.setIsLocked(ConstsStatus.LOCK);
            	userInfo.setUnLockTime(new Date());
            	getMapper().updateLockUser(userInfo);
     	   }
        } catch (Exception e) {
            logger.error("lockUser Exception",e);
        }
    }


    /**
     * unlockUser
     *
     * @param userInfo
     */
    public void updateUnlockUser(UserInfo userInfo) {
        try {
            if (userInfo != null && StringUtils.isNotEmpty(userInfo.getId())) {
                userInfo.setIsLocked(ConstsStatus.ACTIVE);
                userInfo.setUnLockTime(new Date());
                getMapper().updateLockUser(userInfo);
            }
        } catch (Exception e) {
            logger.error("unlockUser Exception",e);
        }
    }

    /**
     * if login password is error ,BadPasswordCount++ and set bad date
     *
     * @param userId
     */
    public void updateLoginFailedCount(String userId) {
    	try {
        	Date currentDate = new Date();
        	UserInfo user = new UserInfo();
        	user.setId(userId);
        	user.setLoginFailedTime(currentDate);
        	getMapper().updateLoginFailedCount(user);
        } catch (Exception e) {
            logger.error("setBadPasswordCount Exception",e);
        }
    }

    public void updateBadPasswordCount(UserInfo userInfo) {
        if (userInfo != null && StringUtils.isNotEmpty(userInfo.getId())) {
            userInfo.setBadPasswordCount(userInfo.getBadPasswordCount() + 1);
            try {
            	Date currentDate = new Date();
            	userInfo.setLoginFailedTime(currentDate);
            	userInfo.setBadPasswordTime(currentDate);
            	getMapper().updateBadPasswordCount(userInfo);
            } catch (Exception e) {
                logger.error("setBadPasswordCount Exception",e);
            }
            ConfigLoginPolicy configLoginPolicy = configLoginPolicyService.getConfigLoginPolicy();
            if(userInfo.getBadPasswordCount() >= configLoginPolicy.getLoginAttempts()) {
         	   logger.debug("Bad Password Count {} , Max Attempts {}",
         			   userInfo.getBadPasswordCount() + 1,configLoginPolicy.getLoginAttempts());
         	   this.updateLockUser(userInfo);
            }
        }
    }

    public void updateLoginFailedCountReset(UserInfo userInfo) {
        if (userInfo != null && StringUtils.isNotEmpty(userInfo.getId()) && userInfo.getBadPasswordCount()>0) {
        	Date currentDate = new Date();
        	userInfo.setLoginFailedTime(currentDate);
        	userInfo.setBadPasswordTime(currentDate);
        	getMapper().updateLoginFailedCountRest(userInfo);
        }
    }

    public void coverPassword(UserInfo userInfo, String password) {
    	 //write password to database Realm
    	ChangePassword changePassword = new ChangePassword(userInfo);
        changePassword.setPassword(password);
        userInfoService.changePassword(changePassword, false);
    }



    public ConfigLoginPolicy getConfigLoginPolicy() {
    	return configLoginPolicyService.getConfigLoginPolicy();
    }

    public void insertHistory(HistoryLogin historyLogin) {
    	historyLogin.setOperateTime(new Date());
    	this.historyLoginService.save(historyLogin);
    	//insert online session
    	if(WebConstants.LOGIN_RESULT.SUCCESS.equals(historyLogin.getMessage())) {
	       SessionList onlineSession = new SessionList();
	       BeanUtils.copyProperties(historyLogin, onlineSession);
	       sessionListService.insertOnline(onlineSession);
       }
    }
}
