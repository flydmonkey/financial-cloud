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




package com.jinbooks.service.idm.impl;

import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinbooks.common.Message;
import com.jinbooks.dto.idm.UserInfoPageDto;
import com.jinbooks.context.WebContext;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinbooks.util.LegacySecretCodec;
import com.jinbooks.dto.auth.ChangePassword;
import com.jinbooks.domain.idm.UserInfo;
import com.jinbooks.enums.UsersBusinessCode;
import com.jinbooks.exception.BusinessException;
import com.jinbooks.repository.idm.UserInfoMapper;
import com.jinbooks.service.security.PasswordPolicyValidatorService;
import com.jinbooks.service.security.impl.PasswordPolicyValidatorServiceImpl;
import com.jinbooks.service.idm.UserInfoService;
import com.jinbooks.util.DateUtils;

import org.springframework.transaction.annotation.Transactional;


/**
 * @author Crystal.Sea
 */
@Repository
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {
    private static final Logger logger = LoggerFactory.getLogger(UserInfoServiceImpl.class);

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    PasswordPolicyValidatorService passwordPolicyValidatorService;

    @Autowired
    UserInfoMapper userInfoMapper;

    public UserInfoMapper getMapper() {
        return userInfoMapper;
    }


    @Override
    public Message<Page<UserInfo>> fetchPageResults(UserInfoPageDto dto) {
    	Page<UserInfo> page = userInfoMapper.fetchPageResults(dto.build(), dto);
    	for(UserInfo user : page.getRecords()) {
    		user.clearSensitive();
    	}
        return Message.ok(page);
    }

    @Override
    @Transactional
    public boolean saveOneUser(UserInfo userInfo) {
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
            throw new BusinessException(
                    UsersBusinessCode.USERNAME_USED.getCode(),
                    UsersBusinessCode.USERNAME_USED.getMsg()
            );
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
            throw new BusinessException(
                    UsersBusinessCode.MOBILE_USED.getCode(),
                    UsersBusinessCode.MOBILE_USED.getMsg()
            );
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
            throw new BusinessException(
                    UsersBusinessCode.EMAIL_USED.getCode(),
                    UsersBusinessCode.EMAIL_USED.getMsg()
            );
        }
    }


    @Override
    @Transactional
    public boolean updateOneUser(UserInfo userInfo) {
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

    public boolean delete(UserInfo userInfo) {
        return super.removeById(userInfo.getId());
    }

	@Override
    public UserInfo findByUsername(String username) {
        return getMapper().findByUsername(username);
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
            changePassword.setDecipherable(LegacySecretCodec.getInstance().encode(changePassword.getPassword()));
            logger.debug("decipherable : {}", changePassword.getDecipherable());
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
    @Override
    @Transactional
    public boolean changePassword(ChangePassword changePassword, boolean passwordPolicy) {
        logger.debug("decipherable old : {}", changePassword.getDecipherable());
        logger.debug("decipherable new : {}", LegacySecretCodec.getInstance().encode(changePassword.getDecipherable()));

        if (passwordPolicy) {
            passwordPolicyValidatorService.validator(changePassword);
        }

        changePassword = passwordEncoder(changePassword);

        if (getMapper().changePassword(changePassword) > 0) {
            return true;
        }
        return false;
    }

	@Override
    public String randomPassword() {
        return passwordPolicyValidatorService.generateRandomPassword();
    }

	@Override
    public boolean updateStatus(UserInfo userInfo) {
        return getMapper().updateStatus(userInfo) > 0;
    }


	@Override
	public boolean switchBook(UserInfo userInfo) {
		return getMapper().switchBook(userInfo) > 0;
	}

    @Override
    public boolean updatePassword(ChangePassword changePassword) {
        try {
            WebContext.setAttribute(PasswordPolicyValidatorServiceImpl.PASSWORD_POLICY_VALIDATE_RESULT, "");
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
                        WebContext.setAttribute(PasswordPolicyValidatorServiceImpl.PASSWORD_POLICY_VALIDATE_RESULT,
                                WebContext.getI18nValue("PasswordPolicy.OLD_PASSWORD_MATCH"));
                    }else {
                        WebContext.setAttribute(PasswordPolicyValidatorServiceImpl.PASSWORD_POLICY_VALIDATE_RESULT,
                                WebContext.getI18nValue("PasswordPolicy.OLD_PASSWORD_NOT_MATCH"));
                    }
                }
            }else {
                WebContext.setAttribute(PasswordPolicyValidatorServiceImpl.PASSWORD_POLICY_VALIDATE_RESULT,
                        WebContext.getI18nValue("PasswordPolicy.CONFIRMPASSWORD_NOT_MATCH"));
            }
        } catch (Exception e) {
            throw new BusinessException(50001, e.getMessage());
        }

        return false;
    }
}
