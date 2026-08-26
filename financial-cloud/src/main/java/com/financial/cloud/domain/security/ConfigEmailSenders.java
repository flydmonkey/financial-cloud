package com.financial.cloud.domain.security;

import java.io.Serial;
import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.EqualsAndHashCode;

import com.baomidou.mybatisplus.annotation.TableName;
import com.financial.cloud.common.BaseEntity;

import lombok.Data;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@TableName("config_email_senders")
public class ConfigEmailSenders  extends BaseEntity implements Serializable {

	@Serial
    private static final long serialVersionUID = 5564960495591334956L;

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String account;

    private String credentials;

    private String smtpHost;

    private Integer port;

    private int sslSwitch;

    private String sender;

    private String encoding;

    private String protocol;

    private int status;

    private String bookId;

    private String description;

}
