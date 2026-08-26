package com.financial.cloud.domain.security;

import com.financial.cloud.common.BaseEntity;
import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author Crystal.Sea
 *
 */

@Data
@EqualsAndHashCode(callSuper=false)
@NoArgsConstructor
@TableName("socials_provider")
public class SocialsProvider extends BaseEntity implements Serializable {
    static final long serialVersionUID = 1636727203025187769L;

	@TableId(type = IdType.ASSIGN_ID)
    String id;

	String provider;

	String providerName;

	String icon;

	String clientId;

	String clientSecret;

    String agentId;

    String display;

    long sortIndex;

    String scanCode;

    int status;

	String bookId;

	String redirectUri;

	String accountId;
	String bindTime;
	String unBindTime;
	String lastLoginTime;
	String state;

	boolean userBind;

	@TableField(fill = FieldFill.INSERT)
	@TableLogic(value="n",delval="y")
	String deleted;

	public SocialsProvider(SocialsProvider copy) {
		this.clientId = copy.getClientId();
		this.id = copy.getId();
		this.provider = copy.getProvider();
		this.providerName = copy.getProviderName();
		this.agentId = copy.getAgentId();
		this.icon = copy.getIcon();
		this.scanCode = copy.getScanCode();
	}
}
