package com.financial.cloud.domain.permissions;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.baomidou.mybatisplus.annotation.TableName;


/**
 * @author Crystal.Sea
 *
 */

@Data
@NoArgsConstructor
@TableName("session_list")
public class SessionList implements Serializable{
	@Serial
	private static final long serialVersionUID = -1321470643357719383L;

	@TableId(type = IdType.ASSIGN_ID)
	String id;

	String sessionId;

	String style;

	String userId;

	String username;

	String displayName;

	String loginType;

	String message;

	String code;

	String provider;

	String ipAddr;

	String country;

	String province;

	String city;

	String location;

	String browser;

	String platform;

	String application;

	Date operateTime;

	@TableField(exist = false)
	private String instName;

	@TableField(exist = false)
	String startDate;

	@TableField(exist = false)
	String endDate;

	@TableField(exist = false)
	String gradingUserId;

}
