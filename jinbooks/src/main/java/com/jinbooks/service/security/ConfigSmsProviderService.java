package com.jinbooks.service.security;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinbooks.domain.security.ConfigSmsProvider;
import com.jinbooks.repository.security.ConfigSmsProviderMapper;
import com.jinbooks.service.security.ConfigSmsProviderService;


@RequiredArgsConstructor
@Repository
public class ConfigSmsProviderService  extends ServiceImpl<ConfigSmsProviderMapper,ConfigSmsProvider>{

	private final ConfigSmsProviderMapper configSmsProviderMapper;

	public ConfigSmsProviderMapper getMapper() {
		return configSmsProviderMapper;
	}


}
