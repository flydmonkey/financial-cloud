package com.financial.cloud.service.security;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.List;

import com.financial.cloud.constants.common.MessageKeys;
import com.financial.cloud.context.WebContext;
import org.passay.LengthRule;
import org.passay.Rule;
import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.financial.cloud.common.Message;
import com.financial.cloud.domain.security.ConfigPasswordPolicy;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.repository.security.ConfigPasswordPolicyMapper;

@RequiredArgsConstructor
@Slf4j
@Repository
public class ConfigPasswordPolicyService extends ServiceImpl<ConfigPasswordPolicyMapper,ConfigPasswordPolicy>{

    private final ConfigPasswordPolicyMapper configPasswordPolicyMapper;


	public ConfigPasswordPolicyMapper getMapper() {
		return configPasswordPolicyMapper;
	}

    /**
     * init PasswordPolicy and load Rules
     * @return
     */
    public ArrayList<Rule> getRuleList(ConfigPasswordPolicy passwordPolicy) {
    	ArrayList <Rule> passwordPolicyRuleList = new ArrayList<>();
        log.debug("query PasswordPolicy : {}" , passwordPolicy);

        // Product policy: only enforce password length >= 6.
        int minLength = 6;
        int maxLength = passwordPolicy != null && passwordPolicy.getMaxLength() >= minLength
                ? passwordPolicy.getMaxLength()
                : 128;
        if (passwordPolicy != null) {
            passwordPolicy.setRandomPasswordLength(Math.max(minLength, Math.min(maxLength, 12)));
        }
        passwordPolicyRuleList.add(new LengthRule(minLength, maxLength));
        return passwordPolicyRuleList;
    }
    public void buildTipMessage(ConfigPasswordPolicy passwordPolicy) {
        List<String> policMessageList = new ArrayList<>();
        String msg = WebContext.getI18nValue(MessageKeys.PasswordPolicy.TOO_SHORT, new Object[]{6});
        if (msg != null && !msg.isEmpty()) {
            policMessageList.add(msg);
        }
        passwordPolicy.setPolicMessageList(policMessageList);
    }

    public ConfigPasswordPolicy  getPasswordPolicy() {
        LambdaQueryWrapper<ConfigPasswordPolicy> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(ConfigPasswordPolicy::getId);
        ConfigPasswordPolicy configPasswordPolicy = null;
        List<ConfigPasswordPolicy> list = super.list(wrapper);
        if (list != null && !list.isEmpty()) {
            configPasswordPolicy = list.get(0);
        }
        return configPasswordPolicy;
    }
    public Message<String> validateUserPassword(UserInfo userInfo) {
        String password = userInfo.getPassword();
        if (password == null || password.length() < 6) {
            return new Message<>(Message.FAIL, "密码至少6位");
        }
        return new Message<>(Message.SUCCESS);
    }
}
