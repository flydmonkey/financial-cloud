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






package com.jinbooks.service.security.impl;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;

import com.jinbooks.context.WebContext;
import org.apache.commons.lang3.ObjectUtils;
import org.passay.CharacterOccurrencesRule;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.EnglishSequenceData;
import org.passay.IllegalSequenceRule;
import org.passay.LengthRule;
import org.passay.Rule;
import org.passay.UsernameRule;
import org.passay.WhitespaceRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinbooks.constants.ConstsRegex;
import com.jinbooks.common.Message;
import com.jinbooks.domain.security.ConfigPasswordPolicy;
import com.jinbooks.domain.idm.UserInfo;
import com.jinbooks.exception.BusinessException;
import com.jinbooks.repository.security.ConfigPasswordPolicyMapper;
import com.jinbooks.service.security.ConfigPasswordPolicyService;

@Repository
public class ConfigPasswordPolicyServiceImpl extends ServiceImpl<ConfigPasswordPolicyMapper,ConfigPasswordPolicy> implements ConfigPasswordPolicyService {
	private static final Logger logger = LoggerFactory.getLogger(ConfigPasswordPolicyServiceImpl.class);

    @Autowired
    ConfigPasswordPolicyMapper configPasswordPolicyMapper;


	public ConfigPasswordPolicyMapper getMapper() {
		return configPasswordPolicyMapper;
	}

    /**
     * init PasswordPolicy and load Rules
     * @return
     */
    @Override
    public ArrayList<Rule> getRuleList(ConfigPasswordPolicy passwordPolicy) {
    	ArrayList <Rule> passwordPolicyRuleList;
        logger.debug("query PasswordPolicy : {}" , passwordPolicy);

        //RandomPasswordLength =(MaxLength +MinLength)/2
        passwordPolicy.setRandomPasswordLength(
            Math.round(
                    (
                            passwordPolicy.getMaxLength() +
                            passwordPolicy.getMinLength()
                    )/2
               )
        );

        passwordPolicyRuleList = new ArrayList<>();
        passwordPolicyRuleList.add(new WhitespaceRule());
        passwordPolicyRuleList.add(new LengthRule(passwordPolicy.getMinLength(), passwordPolicy.getMaxLength()));

        if(passwordPolicy.getUpperCase()>0) {
            passwordPolicyRuleList.add(new CharacterRule(EnglishCharacterData.UpperCase, passwordPolicy.getUpperCase()));
        }

        if(passwordPolicy.getLowerCase()>0) {
            passwordPolicyRuleList.add(new CharacterRule(EnglishCharacterData.LowerCase, passwordPolicy.getLowerCase()));
        }

        if(passwordPolicy.getDigits()>0) {
            passwordPolicyRuleList.add(new CharacterRule(EnglishCharacterData.Digit, passwordPolicy.getDigits()));
        }

        if(passwordPolicy.getSpecialChar()>0) {
            passwordPolicyRuleList.add(new CharacterRule(EnglishCharacterData.Special, passwordPolicy.getSpecialChar()));
        }

        if(passwordPolicy.getUsername()>0) {
            passwordPolicyRuleList.add(new UsernameRule());
        }

        if(passwordPolicy.getOccurances()>0) {
            passwordPolicyRuleList.add(new CharacterOccurrencesRule(passwordPolicy.getOccurances()));
        }

        if(passwordPolicy.getAlphabetical()>0) {
            passwordPolicyRuleList.add(new IllegalSequenceRule(EnglishSequenceData.Alphabetical, 4, false));
        }

        if(passwordPolicy.getNumerical()>0) {
            passwordPolicyRuleList.add(new IllegalSequenceRule(EnglishSequenceData.Numerical, 4, false));
        }

        if(passwordPolicy.getQwerty()>0) {
            passwordPolicyRuleList.add(new IllegalSequenceRule(EnglishSequenceData.USQwerty, 4, false));
        }

        return passwordPolicyRuleList;
    }

    @Override
    public void buildTipMessage(ConfigPasswordPolicy passwordPolicy) {

        List<String> policMessageList = new ArrayList<>();

        String msg;
        if (passwordPolicy.getMinLength() != 0) {
            // msg = "新密码长度为"+minLength+"-"+maxLength+"位";
            msg =   WebContext.getI18nValue("PasswordPolicy.TOO_SHORT",
                    new Object[]{passwordPolicy.getMinLength()});
            policMessageList.add(msg);
        }
        if (passwordPolicy.getMaxLength() != 0) {
            // msg = "新密码长度为"+minLength+"-"+maxLength+"位";
            msg =   WebContext.getI18nValue("PasswordPolicy.TOO_LONG",
                    new Object[]{passwordPolicy.getMaxLength()});
            policMessageList.add(msg);
        }

        if (passwordPolicy.getLowerCase() > 0) {
            //msg = "新密码至少需要包含"+lowerCase+"位【a-z】小写字母";
            msg =   WebContext.getI18nValue("PasswordPolicy.INSUFFICIENT_LOWERCASE",
                    new Object[]{passwordPolicy.getLowerCase()});
            policMessageList.add(msg);
        }

        if (passwordPolicy.getUpperCase() > 0) {
            //msg = "新密码至少需要包含"+upperCase+"位【A-Z】大写字母";
            msg =   WebContext.getI18nValue("PasswordPolicy.INSUFFICIENT_UPPERCASE",
                    new Object[]{passwordPolicy.getUpperCase()});
            policMessageList.add(msg);
        }

        if (passwordPolicy.getDigits() > 0) {
            //msg = "新密码至少需要包含"+digits+"位【0-9】阿拉伯数字";
            msg =   WebContext.getI18nValue("PasswordPolicy.INSUFFICIENT_DIGIT",
                    new Object[]{passwordPolicy.getDigits()});
            policMessageList.add(msg);
        }

        if (passwordPolicy.getSpecialChar() > 0) {
            //msg = "新密码至少需要包含"+specialChar+"位特殊字符";
            msg =   WebContext.getI18nValue("PasswordPolicy.INSUFFICIENT_SPECIAL",
                    new Object[]{passwordPolicy.getSpecialChar()});
            policMessageList.add(msg);
        }

        if (passwordPolicy.getExpiration() > 0) {
            //msg = "新密码有效期为"+expiration+"天";
            msg =   WebContext.getI18nValue("PasswordPolicy.INSUFFICIENT_EXPIRES_DAY",
                    new Object[]{passwordPolicy.getExpiration()});
            policMessageList.add(msg);
        }

        passwordPolicy.setPolicMessageList(policMessageList);
    }

    @Override
    public ConfigPasswordPolicy  getPasswordPolicy() {
        LambdaQueryWrapper<ConfigPasswordPolicy> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(ConfigPasswordPolicy::getId);
        ConfigPasswordPolicy configPasswordPolicy = null;
        List<ConfigPasswordPolicy> list = super.list(wrapper);
        if (ObjectUtils.isNotEmpty(list)) {
            configPasswordPolicy = list.get(0);
        }
        return configPasswordPolicy;
    }

    public Message<String> validateUserPassword(UserInfo userInfo) {
        String password = userInfo.getPassword();

        //密码校验结果
        List<String> resultMsgList = new ArrayList<>();

        LambdaQueryWrapper<ConfigPasswordPolicy> lambdaQueryWrapper = new LambdaQueryWrapper<ConfigPasswordPolicy>();
    	//lambdaQueryWrapper.eq(ConfigPasswordPolicy::getInstId,instId);
        ConfigPasswordPolicy policy = this.getOne(lambdaQueryWrapper);
        if (Objects.isNull(policy)) {
            throw new BusinessException(
                    400,
                    "请联系系统管理员配置密码策略"
            );
        }

        if (containsChineseCharacters(password)) {
            resultMsgList.add("密码禁止输入中文字符");
        }

        Matcher matcher = ConstsRegex.WHITESPACE_REGEX.matcher(password);
        if (matcher.find()) {
            resultMsgList.add("密码不能包含空格");
        }

        int minLength = policy.getMinLength();
        int maxLength = policy.getMaxLength();
        int passwordLength = password.length();
        if (passwordLength < minLength || passwordLength > maxLength) {
            resultMsgList.add(MessageFormat.format("密码长度需要{0}~{1}位非空字符", minLength, maxLength));
        }

        //检查字母数字符号策略
        checkCharacterPolicy(password, policy, resultMsgList);

        if (ObjectUtils.isNotEmpty(resultMsgList)) {
            return new Message<>(Message.FAIL, String.join("、", resultMsgList));
        }

        return new Message<>(Message.SUCCESS);
    }

    /**
     * @Description: 正则表达式匹配中文字符
     * @Param: [password]
     * @return: boolean
     */
    private boolean containsChineseCharacters(String password) {
        return ConstsRegex.CHINESE_REGEX.matcher(password).find();
    }

    /**
     * 检查密码中的字母数量是否达到要求
     * @param password 密码字符串
     * @param policy 密码策略
     * @param resultMsgList 结果消息列表
     */
    private static void checkCharacterPolicy(String password, ConfigPasswordPolicy policy, List<String> resultMsgList) {
        int lowerCaseCount = 0;
        int upperCaseCount = 0;
        int specialCharCount = 0;
        int numericalCount = 0;
        for (char ch : password.toCharArray()) {
            if (Character.isLowerCase(ch)) {
                lowerCaseCount++;
            } else if (Character.isUpperCase(ch)) {
                upperCaseCount++;
            } else if (Character.isDigit(ch)) {
                numericalCount++;
            } else if (!Character.isLetterOrDigit(ch) && !Character.isWhitespace(ch)) {
                specialCharCount++;
            }
        }

        checkCount(lowerCaseCount, policy.getLowerCase(), "小写字母", resultMsgList);
        checkCount(upperCaseCount, policy.getUpperCase(), "大写字母", resultMsgList);
        checkCount(specialCharCount, policy.getSpecialChar(), "特殊符号", resultMsgList);
        checkCount(numericalCount, policy.getNumerical(), "数字", resultMsgList);
    }

    private static void checkCount(int actualCount, int requiredCount, String characterType, List<String> resultMsgList) {
        if (actualCount < requiredCount) {
            String message = MessageFormat.format("至少包含{0}个{1}", requiredCount, characterType);
            resultMsgList.add(message);
        }
    }
}
