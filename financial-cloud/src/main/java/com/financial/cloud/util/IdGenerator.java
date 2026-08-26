package com.financial.cloud.util;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;

public class IdGenerator {

	private String strategy = "uuid";

	private Snowflake snowflake = IdUtil.getSnowflake(0, 0);

	public IdGenerator() {
	}

	public IdGenerator(String strategy) {
		this.strategy = strategy;
	}

	public String generate() {
		if ("uuid".equalsIgnoreCase(strategy)) {
			return IdUtil.fastSimpleUUID();
		}
		if ("SnowFlake".equalsIgnoreCase(strategy)) {
			return snowflake.nextIdStr();
		}
		return RandomUtil.randomString(6);
	}

	public void setSnowflake(Snowflake snowflake) {
		this.snowflake = snowflake;
	}
}
