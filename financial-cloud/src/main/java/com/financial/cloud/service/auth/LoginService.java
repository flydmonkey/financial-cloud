package com.financial.cloud.service.auth;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import com.financial.cloud.authn.core.BadCredentialsException;
import com.financial.cloud.authn.core.Authority;
import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.financial.cloud.constants.ConstsStatus;
import com.financial.cloud.constants.MessageKeys;
import com.financial.cloud.dto.auth.ChangePassword;
import com.financial.cloud.domain.permissions.SessionList;
import com.financial.cloud.dto.book.BookVo;
import com.financial.cloud.domain.security.ConfigLoginPolicy;
import com.financial.cloud.domain.history.HistoryLogin;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.domain.permissions.Resources;
import com.financial.cloud.repository.auth.LoginMapper;
import com.financial.cloud.service.permissions.AuthzResourceService;
import com.financial.cloud.service.permissions.AuthzService;
import com.financial.cloud.service.book.BookService;
import com.financial.cloud.service.security.ConfigLoginPolicyService;
import com.financial.cloud.service.auth.FileStorageService;
import com.financial.cloud.service.history.HistoryLoginService;
import com.financial.cloud.service.auth.LoginService;
import com.financial.cloud.service.permissions.SessionListService;
import com.financial.cloud.service.idm.UserInfoService;
import com.financial.cloud.util.DateUtils;
import com.financial.cloud.context.WebConstants;
import com.financial.cloud.context.WebContext;

@RequiredArgsConstructor
@Slf4j
@Repository
public class LoginService  extends ServiceImpl<LoginMapper,UserInfo>{

	private final LoginMapper loginMapper;

	private final UserInfoService userInfoService;

	private final ConfigLoginPolicyService configLoginPolicyService;

	private final HistoryLoginService historyLoginService;

	private final SessionListService sessionListService;

    private final FileStorageService fileStorageService;

    private final AuthzService authzService;

    private final AuthzResourceService authzResourceService;
    
    private final BookService bookService;

	public LoginMapper getMapper() {
		return loginMapper;
	}
	public void updateLastLogin(UserInfo userInfo) {
		this.getMapper().updateLastLogin(userInfo);
	}
	public UserInfo findById(String userId) {
		return this.getMapper().findById(userId);
	}
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
	public List<Authority> grantAuthority(UserInfo userInfo) {
		return authzService.grantAuthority(userInfo);
	}
	 public Set<Resources> getResourcesBySubject(UserInfo user){
		 return authzResourceService.getResourcesBySubject(user);
	 }


    /**
     * dynamic ConfigLoginPolicy Valid for user login.
     * @param userInfo
     * @return boolean
     */
    public boolean applyLoginPolicy(UserInfo userInfo) {

    	ConfigLoginPolicy configLoginPolicy = configLoginPolicyService.getConfigLoginPolicy();

        Date currentdateTime = new Date();
         /*
          * check login attempts fail times
          */
         if (userInfo.getLoginFailedCount() >= configLoginPolicy.getLoginAttempts() && userInfo.getLoginFailedTime() != null) {
             log.debug("login Attempts is {}" , userInfo.getLoginFailedCount());
             Date loginFailedTime = userInfo.getLoginFailedTime();
             //duration
             log.trace("Login Failed Time {}" , DateUtils.formatDateTime(loginFailedTime));

             long intDuration = (currentdateTime.getTime() - loginFailedTime.getTime()) / (60 * 1000);
             log.debug("Login Failed duration {} , " +
                           "Login policy Duration {} , "+
                           "validate result {}" ,
                           intDuration,
                           configLoginPolicy.getLockInterval(),
                           (intDuration > configLoginPolicy.getLockInterval())
                     );
             //auto unlock attempts when intDuration >= set Duration
             if(intDuration >= configLoginPolicy.getLockInterval()) {
                 log.debug("resetAttempts ...");
                 updateUnlockUser(userInfo);
             }else {
            	 updateLockUser(userInfo);
                 throw new BadCredentialsException(
                         WebContext.getI18nValue(MessageKeys.Login.ERROR_ATTEMPTS,
                                 new Object[]{userInfo.getLoginFailedCount(),configLoginPolicy.getLockInterval()})
                         );
             }
         }

         //locked
         if(userInfo.getIsLocked()==ConstsStatus.LOCK) {
             throw new BadCredentialsException(
                                 userInfo.getUsername()+ " "+
                                 WebContext.getI18nValue(MessageKeys.Login.ERROR_LOCKED)
                                 );
         }
         // inactive
         if(userInfo.getStatus()!=ConstsStatus.ACTIVE) {
             throw new BadCredentialsException(
                                 userInfo.getUsername()+
                                 WebContext.getI18nValue(MessageKeys.Login.ERROR_INACTIVE)
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
            log.error("lockUser Exception",e);
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
            log.error("unlockUser Exception",e);
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
            log.error("setBadPasswordCount Exception",e);
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
                log.error("setBadPasswordCount Exception",e);
            }
            ConfigLoginPolicy configLoginPolicy = configLoginPolicyService.getConfigLoginPolicy();
            if(userInfo.getBadPasswordCount() >= configLoginPolicy.getLoginAttempts()) {
         	   log.debug("Bad Password Count {} , Max Attempts {}",
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
