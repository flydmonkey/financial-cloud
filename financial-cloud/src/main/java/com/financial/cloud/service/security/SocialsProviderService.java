package com.financial.cloud.service.security;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.financial.cloud.domain.security.SocialsProvider;
import com.financial.cloud.repository.security.SocialsProviderMapper;
import com.financial.cloud.service.security.SocialsProviderService;

@RequiredArgsConstructor
@Slf4j
@Repository
public class SocialsProviderService  extends ServiceImpl<SocialsProviderMapper,SocialsProvider>{

	private final SocialsProviderMapper socialsProviderMapper;

	public SocialsProviderMapper getMapper() {
		return socialsProviderMapper;
	}


}
