package com.financial.cloud.service.security;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.financial.cloud.domain.security.ConfigEmailSenders;
import com.financial.cloud.repository.security.ConfigEmailSendersMapper;
import com.financial.cloud.service.security.ConfigEmailSendersService;


@RequiredArgsConstructor
@Repository
public class ConfigEmailSendersService  extends ServiceImpl<ConfigEmailSendersMapper,ConfigEmailSenders>{

	private final ConfigEmailSendersMapper configEmailSendersMapper;

	public ConfigEmailSendersMapper getMapper() {
		return configEmailSendersMapper;
	}


}
