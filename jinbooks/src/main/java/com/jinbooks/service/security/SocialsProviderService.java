package com.jinbooks.service.security;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinbooks.domain.security.SocialsProvider;
import com.jinbooks.repository.security.SocialsProviderMapper;
import com.jinbooks.service.security.SocialsProviderService;

@RequiredArgsConstructor
@Slf4j
@Repository
public class SocialsProviderService  extends ServiceImpl<SocialsProviderMapper,SocialsProvider>{

	private final SocialsProviderMapper socialsProviderMapper;

	public SocialsProviderMapper getMapper() {
		return socialsProviderMapper;
	}


}
