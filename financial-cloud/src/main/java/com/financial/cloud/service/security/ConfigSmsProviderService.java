package com.financial.cloud.service.security;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.financial.cloud.domain.security.ConfigSmsProvider;
import com.financial.cloud.repository.security.ConfigSmsProviderMapper;
import com.financial.cloud.service.security.ConfigSmsProviderService;


@RequiredArgsConstructor
@Repository
public class ConfigSmsProviderService  extends ServiceImpl<ConfigSmsProviderMapper,ConfigSmsProvider>{

	private final ConfigSmsProviderMapper configSmsProviderMapper;

	public ConfigSmsProviderMapper getMapper() {
		return configSmsProviderMapper;
	}


}
