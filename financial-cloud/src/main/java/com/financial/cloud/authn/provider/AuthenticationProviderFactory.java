package com.financial.cloud.authn.provider;

import java.util.concurrent.ConcurrentHashMap;

import com.financial.cloud.authn.core.AuthAuthentication;

import com.financial.cloud.authn.LoginCredential;
import com.financial.cloud.context.WebConstants;
import com.financial.cloud.context.WebContext;

public class AuthenticationProviderFactory extends AbstractAuthenticationProvider {

    static ConcurrentHashMap<String,AbstractAuthenticationProvider> providers = new ConcurrentHashMap<>();
    
    /**
     * 登录传入类型AuthType，读取认证提供者，进行登录认证
     */
    @Override
    public AuthAuthentication authenticate(LoginCredential credential){
    	AbstractAuthenticationProvider provider = providers.get(credential.getAuthType() + PROVIDER_SUFFIX);
    	if (provider == null) {
    		WebContext.setAttribute(WebConstants.LOGIN_ERROR_SESSION_MESSAGE, "不支持的登录方式");
    		return null;
    	}
    	return provider.doAuthenticate(credential);
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
	public AuthAuthentication doAuthenticate(LoginCredential credential) {
		//AuthenticationProvider Factory do nothing 
		return null;
	}
}
