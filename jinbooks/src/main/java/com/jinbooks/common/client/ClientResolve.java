package com.jinbooks.common.client;

import java.io.Serializable;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ClientResolve implements Serializable{
	private static final long serialVersionUID = 3557014779495463632L;

	/**
	 * 登录IP地址
	 */
	String ipAddr;

	/**
	 * 登录IP地址归属
	 */
	String location;

	/**
	 * 登录IP地址归属国家
	 */
	String country;

	/**
	 * 登录IP地址归属省、州
	 */
	String province;

	/**
	 * 登录IP地址归属城市
	 */
	String city;

	/**
	 * 浏览器
	 */
	String browser;

	/**
	 * 操作系统平台
	 */
	String platform;

	/**
	 * userAgent Hash
	 */
	String userAgentHash;

	public ClientResolve(ClientUserAgent clientUserAgent) {
		super();
		this.browser = clientUserAgent.getName();
		this.platform = clientUserAgent.getPlatform();
		this.userAgentHash = clientUserAgent.getUserAgentHash();
	}
	
	public ClientResolve(String ipAddr, String location, String country, String province, String city) {
		super();
		this.ipAddr = ipAddr;
		this.location = location;
		this.country = country;
		this.province = province;
		this.city = city;
	}
}
