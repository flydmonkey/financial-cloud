package com.financial.cloud.domain.security;

import java.io.Serial;
import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.EqualsAndHashCode;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.baomidou.mybatisplus.annotation.TableName;
import com.financial.cloud.common.BaseEntity;

/**
 * @author 24096
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@TableName("config_sms_provider")
public class ConfigSmsProvider extends BaseEntity implements Serializable {
	@Serial
    private static final long serialVersionUID = -4595539647817265938L;

    @TableId(type = IdType.ASSIGN_ID)
    String id;

    String provider;

    String message;

    String appKey;

    String appSecret;

    String templateId;

    String signName;

    String smsSdkAppId;

    String description;

    int status;

	private String bookId;

}
