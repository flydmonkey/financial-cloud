package com.jinbooks.authn.provider;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.core.Authentication;

import com.jinbooks.authn.LoginCredential;

/**
 * 认证提供者工厂
 * 
 * @author Crystal.Sea
 *
 */
public class AuthenticationProviderFactory extends AbstractAuthenticationProvider {

    static ConcurrentHashMap<String,AbstractAuthenticationProvider> providers = new ConcurrentHashMap<>();
    
    /**
     * 登录传入类型AuthType，读取认证提供者，进行登录认证
     */
    @Override
    public Authentication authenticate(LoginCredential credential){
    	AbstractAuthenticationProvider provider = providers.get(credential.getAuthType() + PROVIDER_SUFFIX);
    	
    	return provider == null ? null : provider.doAuthenticate(credential);
    }
    
    /**
     * 增加认证提供者
     * @param provider
     */
    public void addAuthenticationProvider(AbstractAuthenticationProvider provider) {
    	if(provider != null && provider.isSupported()) {
    		providers.put(provider.getProviderName(), provider);
    	}
    }

	@Override
	public String getProviderName() {
		return "AuthenticationProviderFactory";
	}

	@Override
	public Authentication doAuthenticate(LoginCredential credential) {
		//AuthenticationProvider Factory do nothing 
		return null;
	}
}
