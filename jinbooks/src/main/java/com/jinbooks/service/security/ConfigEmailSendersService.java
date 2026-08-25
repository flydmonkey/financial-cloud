package com.jinbooks.service.security;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinbooks.domain.security.ConfigEmailSenders;
import com.jinbooks.repository.security.ConfigEmailSendersMapper;
import com.jinbooks.service.security.ConfigEmailSendersService;


@RequiredArgsConstructor
@Repository
public class ConfigEmailSendersService  extends ServiceImpl<ConfigEmailSendersMapper,ConfigEmailSenders>{

	private final ConfigEmailSendersMapper configEmailSendersMapper;

	public ConfigEmailSendersMapper getMapper() {
		return configEmailSendersMapper;
	}


}
