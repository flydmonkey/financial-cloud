package com.financial.cloud.domain.security;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.baomidou.mybatisplus.annotation.TableName;
import com.financial.cloud.constants.system.ConstsServiceMessage;
import com.financial.cloud.constants.common.MessageKeys;
import com.financial.cloud.exception.PasswordPolicyException;
import com.financial.cloud.context.WebContext;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@TableName("config_password_policy")
public class ConfigPasswordPolicy implements Serializable {

    @Serial
    private static final long serialVersionUID = -4797776994287829182L;

    @TableId(type = IdType.ASSIGN_ID)
    String id;

    /**
     * minimum password lengths
     */

    @NotNull
    private int minLength;

    /**
     * maximum password lengths
     */
    @NotNull
    private int maxLength;

    /**
     * least lowercase letter
     */
    @NotNull
    private int lowerCase;

    /**
     * least uppercase letter
     */
    @NotNull
    private int upperCase;

    /**
     * inclusion of numerical digits
     */
    @NotNull
    private int digits;

    /**
     * inclusion of special characters
     */
    @NotNull
    private int specialChar;

    /**
     * require users to change passwords periodically
     */
    private int expiration;

    /**
     * 0 no 1 yes
     */
    private int username;

    /**
     * not include password list
     */
    private int history;

    private int dictionary;

    private int alphabetical;

    private int numerical;

    private int qwerty;

    private int occurances;

    @TableField(exist = false)
    private int randomPasswordLength = 12;

    @TableField(exist = false)
    List<String> policMessageList;

    public void buildMessage(){
        if(policMessageList==null){
            policMessageList = new ArrayList<>();
        }
        String msg;
        if (minLength != 0) {
           // msg = "新密码长度为"+minLength+"-"+maxLength+"位";
            msg =   WebContext.getI18nValue(MessageKeys.PasswordPolicy.TOO_SHORT,
                    new Object[]{minLength});
            policMessageList.add(msg);
        }
        if (maxLength != 0) {
            // msg = "新密码长度为"+minLength+"-"+maxLength+"位";
            msg =   WebContext.getI18nValue(MessageKeys.PasswordPolicy.TOO_LONG,
                    new Object[]{maxLength});
            policMessageList.add(msg);
        }

        if (lowerCase > 0) {
           //msg = "新密码至少需要包含"+lowerCase+"位【a-z】小写字母";
            msg =   WebContext.getI18nValue(MessageKeys.PasswordPolicy.INSUFFICIENT_LOWERCASE,
                    new Object[]{lowerCase});
            policMessageList.add(msg);
        }

        if (upperCase > 0) {
            //msg = "新密码至少需要包含"+upperCase+"位【A-Z】大写字母";
            msg =   WebContext.getI18nValue(MessageKeys.PasswordPolicy.INSUFFICIENT_UPPERCASE,
                    new Object[]{upperCase});
            policMessageList.add(msg);
        }

        if (digits > 0) {
            //msg = "新密码至少需要包含"+digits+"位【0-9】阿拉伯数字";
            msg =   WebContext.getI18nValue(MessageKeys.PasswordPolicy.INSUFFICIENT_DIGIT,
                    new Object[]{digits});
            policMessageList.add(msg);
        }

        if (specialChar > 0) {
            //msg = "新密码至少需要包含"+specialChar+"位特殊字符";
            msg =   WebContext.getI18nValue(MessageKeys.PasswordPolicy.INSUFFICIENT_SPECIAL,
                    new Object[]{specialChar});
            policMessageList.add(msg);
        }

        if (expiration > 0) {
            //msg = "新密码有效期为"+expiration+"天";
            msg =   WebContext.getI18nValue(MessageKeys.PasswordPolicy.INSUFFICIENT_EXPIRES_DAYS,
                    new Object[]{expiration});
            policMessageList.add(msg);
        }
    }


    public void check(String username, String newPassword, String oldPassword) throws PasswordPolicyException {
        if (newPassword == null || newPassword.length() < 6) {
            throw new PasswordPolicyException(ConstsServiceMessage.PASSWORDPOLICY.XW00000003, 6);
        }
        if (this.getMaxLength() > 0 && newPassword.length() > this.getMaxLength()) {
            throw new PasswordPolicyException(ConstsServiceMessage.PASSWORDPOLICY.XW00000004, this.getMaxLength());
        }
    }

}
