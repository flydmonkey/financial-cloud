package com.financial.cloud.service.config;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.financial.cloud.domain.config.Institutions;
import com.financial.cloud.repository.config.InstitutionsMapper;
import com.financial.cloud.service.config.InstitutionsService;

import org.springframework.stereotype.Service;


@RequiredArgsConstructor
@Slf4j
@Service
public class InstitutionsService extends ServiceImpl<InstitutionsMapper,Institutions>{

	private final InstitutionsMapper institutionsMapper;

    protected static final Cache<String, Institutions> institutionsStore =
            Caffeine.newBuilder()
                	.expireAfterWrite(60, TimeUnit.MINUTES)
                	.build();

    //id to domain mapping
    protected static final  ConcurrentHashMap<String,String> mapper = new ConcurrentHashMap<>();

    private static final String DEFAULT_INSTID = "1";

	public InstitutionsMapper getMapper() {
		return institutionsMapper;
	}

	 public Institutions getByInstIdOrDomain(String instIdOrDomain) {
		 log.trace(" instId or domain {}" , instIdOrDomain);
		 Institutions inst = institutionsStore.getIfPresent(mapper.get(instIdOrDomain)== null ? DEFAULT_INSTID : mapper.get(instIdOrDomain) );
		 if(inst == null) {
	        inst = getMapper().getByInstIdOrDomain(instIdOrDomain);
	        if(inst != null ) {
		        institutionsStore.put(inst.getDomain(), inst);
		        mapper.put(inst.getId(), inst.getDomain());
	        }
		 }
		 if(inst == null) {//use default inst
	        	inst = getByInstIdOrDomain(DEFAULT_INSTID);
	        	institutionsStore.put(instIdOrDomain, inst);
	        	mapper.put(instIdOrDomain, inst.getDomain());
	        }
		 return inst;
	 }

}
