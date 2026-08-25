package com.jinbooks.authn.resolver;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import com.jinbooks.authn.annotation.CurrentUser;
import com.jinbooks.authn.support.AuthorizationUtils;
import com.jinbooks.context.WebConstants;
import com.jinbooks.domain.idm.UserInfo;

/**
 * CurrentUser注解的注入实现
 * 
 * @author Crystal.Sea
 *
 */
public class CurrentUserMethodArgumentResolver implements HandlerMethodArgumentResolver {
	
    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
    	//读取认证信息
    	Authentication  authentication  = 
    			(Authentication ) webRequest.getAttribute(
    					WebConstants.AUTHENTICATION, RequestAttributes.SCOPE_SESSION);
    	UserInfo userInfo  = AuthorizationUtils.getUserInfo(authentication);
    	if (userInfo != null) {
            return userInfo;
        }
        throw new MissingServletRequestPartException("currentUser");
    }
    
    /**
     * 判断参数类型及注解
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().isAssignableFrom(UserInfo.class)
                && parameter.hasParameterAnnotation(CurrentUser.class);
    }
    
}
