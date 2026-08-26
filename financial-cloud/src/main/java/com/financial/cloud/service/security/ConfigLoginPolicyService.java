package com.financial.cloud.service.security;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.TimeUnit;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.financial.cloud.domain.security.ConfigLoginPolicy;
import com.financial.cloud.repository.security.ConfigLoginPolicyMapper;
import com.financial.cloud.service.security.ConfigLoginPolicyService;

import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Slf4j
@Service
public class ConfigLoginPolicyService extends ServiceImpl<ConfigLoginPolicyMapper,ConfigLoginPolicy>{

	static final String CONFIG_LOGIN_POLICY_KEY = "CONFIG_LOGIN_POLICY_KEY";

	private final ConfigLoginPolicyMapper configLoginPolicyMapper;

    //Cache ConfigLoginPolicy in memory ONE_HOUR
    static final Cache<String,  ConfigLoginPolicy> configLoginPolicyStore =
            Caffeine.newBuilder()
                .expireAfterWrite(60, TimeUnit.MINUTES)
                .build();

	public ConfigLoginPolicyMapper getMapper() {
		return configLoginPolicyMapper;
	}
	public ConfigLoginPolicy getConfigLoginPolicy() {
		ConfigLoginPolicy configLoginPolicy = configLoginPolicyStore.getIfPresent(CONFIG_LOGIN_POLICY_KEY);
        if (configLoginPolicy == null) {
			LambdaQueryWrapper<ConfigLoginPolicy> wrapper = new LambdaQueryWrapper<>();
			wrapper.isNotNull(ConfigLoginPolicy::getId);
			configLoginPolicy = super.getOne(wrapper);
            configLoginPolicyStore.put(CONFIG_LOGIN_POLICY_KEY,configLoginPolicy);
            log.debug("get ConfigLoginPolicy : {}" , configLoginPolicy);
        }
        return configLoginPolicy;
    }



}
