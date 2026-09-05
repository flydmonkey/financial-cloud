package com.financial.cloud.service.idm;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.financial.cloud.common.Message;
import com.financial.cloud.dto.idm.UserInfoPageDto;
import com.financial.cloud.constants.common.MessageKeys;
import com.financial.cloud.context.WebContext;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.financial.cloud.util.LegacySecretCodec;
import com.financial.cloud.dto.auth.ChangePassword;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.enums.error.UsersBusinessCode;
import com.financial.cloud.exception.BusinessException;
import com.financial.cloud.repository.idm.UserInfoMapper;
import com.financial.cloud.service.security.PasswordPolicyValidatorService;
import com.financial.cloud.util.DateUtils;

import org.springframework.transaction.annotation.Transactional;


@RequiredArgsConstructor
@Slf4j
@Repository
public class UserInfoService extends ServiceImpl<UserInfoMapper, UserInfo>{

    private final PasswordEncoder passwordEncoder;

    private final PasswordPolicyValidatorService passwordPolicyValidatorService;

    private final UserInfoMapper userInfoMapper;

    private final LegacySecretCodec legacySecretCodec;

    public UserInfoMapper getMapper() {
        return userInfoMapper;
    }
    public Message<Page<UserInfo>> fetchPageResults(UserInfoPageDto dto) {
    	Page<UserInfo> page = userInfoMapper.fetchPageResults(dto.build(), dto);
    	for(UserInfo user : page.getRecords()) {
    		user.clearSensitive();
    	}
        return Message.ok(page);
    }
    @Transactional
    public boolean saveOneUser(UserInfo userInfo) {
        normalizeUserIdentity(userInfo);
        String username = userInfo.getUsername();
        String mobile = userInfo.getMobile();
        String email = userInfo.getEmail();
        String password = userInfo.getPassword();

        //校验登录账号
        checkUsernameDuplicate(username, null);
        //校验手机号码
        checkMobileDuplicate(mobile, null);
        //校验邮箱地址
        checkEmailDuplicate(email, null);
        //密码规则验证
        passwordPolicyValidatorService.validator(new ChangePassword(username, password));

        passwordEncoder(userInfo);

        return super.save(userInfo);
    }

    /**
     * Public self-registration (no admin, no book/role yet).
     */
    @Transactional
    public UserInfo registerPublic(String username, String password, String displayName) {
        UserInfo userInfo = new UserInfo();
        userInfo.setId(WebContext.genId());
        userInfo.setUsername(StringUtils.trim(username));
        userInfo.setPassword(password);
        userInfo.setDisplayName(StringUtils.trim(displayName));
        userInfo.setUserType("EMPLOYEE");
        userInfo.setUserState("RESIDENT");
        userInfo.setStatus(1);
        userInfo.setSortIndex(1);
        userInfo.setIsLocked(0);
        userInfo.setLoginCount(0);
        userInfo.setBadPasswordCount(0);
        userInfo.setBookId("");
        userInfo.setCreatedBy("register");
        userInfo.setCreatedDate(new Date());
        if (!saveOneUser(userInfo)) {
            throw new BusinessException(UsersBusinessCode.USERNAME_USED);
        }
        return userInfo;
    }

    /**
     * @Description: 校验登录账号是否重复
     * @Param: [username]
     * @return: void
     */
    public void checkUsernameDuplicate(String username, String id) {
        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserInfo::getUsername, username);
        if (StringUtils.isNotEmpty(id)) {
            wrapper.ne(UserInfo::getId, id);
        }
        List<UserInfo> query = super.list(wrapper);
        if (ObjectUtils.isNotEmpty(query)) {
            throw new BusinessException(UsersBusinessCode.USERNAME_USED);
        }
    }

    /**
     * @Description: 校验手机号码是否重复
     * @Param: [mobile, id]
     * @return: void
     */
    public void checkMobileDuplicate(String mobile, String id) {
        if (StringUtils.isBlank(mobile)) {
            return;
        }
        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserInfo::getMobile, mobile);
        if (StringUtils.isNotEmpty(id)) {
            wrapper.ne(UserInfo::getId, id);
        }
        List<UserInfo> query = super.list(wrapper);
        if (ObjectUtils.isNotEmpty(query)) {
            throw new BusinessException(UsersBusinessCode.MOBILE_USED);
        }
    }

    /**
     * @Description: 校验邮箱是否重复
     * @Param: [email, id]
     * @return: void
     */
    public void checkEmailDuplicate(String email, String id) {
        if (StringUtils.isBlank(email)) {
            return;
        }

        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<UserInfo>();
        wrapper.eq(UserInfo::getEmail, email);
        if (StringUtils.isNotEmpty(id)) {
            wrapper.ne(UserInfo::getId, id);
        }
        List<UserInfo> query = super.list(wrapper);
        if (ObjectUtils.isNotEmpty(query)) {
            throw new BusinessException(UsersBusinessCode.EMAIL_USED);
        }
    }
    @Transactional
    public boolean updateOneUser(UserInfo userInfo) {
        normalizeUserIdentity(userInfo);
        String username = userInfo.getUsername();
        String mobile = userInfo.getMobile();
        String email = userInfo.getEmail();
        String id = userInfo.getId();


        //校验登录账号
        checkUsernameDuplicate(username, id);
        //校验手机号码
        checkMobileDuplicate(mobile, id);
        //校验邮箱地址
        checkEmailDuplicate(email, id);

        userInfo.setPassword(super.getById(id).getPassword());
        userInfo.setDecipherable(super.getById(id).getDecipherable());

        return super.updateById(userInfo);
    }

    private void normalizeUserIdentity(UserInfo userInfo) {
        if (userInfo.getUsername() != null) {
            userInfo.setUsername(StringUtils.trim(userInfo.getUsername()));
        }
        if (userInfo.getDisplayName() != null) {
            userInfo.setDisplayName(StringUtils.trim(userInfo.getDisplayName()));
        }
        if (userInfo.getMobile() != null) {
            userInfo.setMobile(StringUtils.trimToNull(userInfo.getMobile()));
        }
        if (userInfo.getEmail() != null) {
            userInfo.setEmail(StringUtils.trimToNull(userInfo.getEmail()));
        }
    }

    public boolean delete(UserInfo userInfo) {
        return super.removeById(userInfo.getId());
    }
    public UserInfo findByUsername(String username) {
        return getMapper().findByUsername(StringUtils.trim(username));
    }

    public void passwordEncoder(UserInfo userInfo) {
        ChangePassword changePassword = null;
        if (StringUtils.isNotBlank(userInfo.getPassword())) {
            changePassword = new ChangePassword(userInfo);
            passwordEncoder(changePassword);
            userInfo.setPassword(changePassword.getPassword());
            userInfo.setDecipherable(changePassword.getDecipherable());
            userInfo.setPasswordLastSetTime(new Date());
        } else {
            userInfo.setPassword(null);
            userInfo.setDecipherable(null);
        }
	}

    public ChangePassword passwordEncoder(ChangePassword changePassword) {
        //密码不为空，则需要进行加密处理
        if (StringUtils.isNotBlank(changePassword.getPassword())) {
            String password = passwordEncoder.encode(changePassword.getPassword());
            changePassword.setDecipherable(legacySecretCodec.encode(changePassword.getPassword()));
            log.debug("decipherable : {}", changePassword.getDecipherable());
            changePassword.setPassword(password);
            changePassword.setPasswordLastSetTime(DateUtils.getCurrentDateTimeAsString());

        } else {
            changePassword.setPassword(null);
            changePassword.setDecipherable(null);
        }
        return changePassword;
    }

    /**
     * @Description: 后台密码修改
     * @Param: [changePassword, passwordPolicy]
     * @return: boolean
     */
    @Transactional
    public boolean changePassword(ChangePassword changePassword, boolean passwordPolicy) {
        log.debug("decipherable old : {}", changePassword.getDecipherable());
        log.debug("decipherable new : {}", legacySecretCodec.encode(changePassword.getDecipherable()));

        if (passwordPolicy) {
            passwordPolicyValidatorService.validator(changePassword);
        }

        changePassword = passwordEncoder(changePassword);

        if (getMapper().changePassword(changePassword) > 0) {
            return true;
        }
        return false;
    }
    public String randomPassword() {
        return passwordPolicyValidatorService.generateRandomPassword();
    }
    public boolean updateStatus(UserInfo userInfo) {
        return getMapper().updateStatus(userInfo) > 0;
    }
	public boolean switchBook(UserInfo userInfo) {
		return getMapper().switchBook(userInfo) > 0;
	}
    public boolean updatePassword(ChangePassword changePassword) {
        try {
            WebContext.setAttribute(PasswordPolicyValidatorService.PASSWORD_POLICY_VALIDATE_RESULT, "");
            UserInfo userInfo = this.findByUsername(changePassword.getUsername());
            if(changePassword.getPassword().equals(changePassword.getConfirmPassword())){
                if(StringUtils.isNotBlank(changePassword.getOldPassword()) &&
                        passwordEncoder.matches(changePassword.getOldPassword(), userInfo.getPassword())){
                    if(changePassword(changePassword,true) ){
                        return true;
                    }
                    return false;
                }else {
                    if(StringUtils.isNotBlank(changePassword.getOldPassword())&&
                            passwordEncoder.matches(changePassword.getPassword(), userInfo.getPassword())) {
                        WebContext.setAttribute(PasswordPolicyValidatorService.PASSWORD_POLICY_VALIDATE_RESULT,
                                WebContext.getI18nValue(MessageKeys.PasswordPolicy.OLD_PASSWORD_MATCH));
                    }else {
                        WebContext.setAttribute(PasswordPolicyValidatorService.PASSWORD_POLICY_VALIDATE_RESULT,
                                WebContext.getI18nValue(MessageKeys.PasswordPolicy.OLD_PASSWORD_NOT_MATCH));
                    }
                }
            }else {
                WebContext.setAttribute(PasswordPolicyValidatorService.PASSWORD_POLICY_VALIDATE_RESULT,
                        WebContext.getI18nValue(MessageKeys.PasswordPolicy.CONFIRM_PASSWORD_NOT_MATCH));
            }
        } catch (Exception e) {
            throw new BusinessException(50001, e.getMessage());
        }

        return false;
    }
}
