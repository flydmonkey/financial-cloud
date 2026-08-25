package com.jinbooks.filter;


import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


import org.apache.commons.lang3.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

import com.jinbooks.constants.ConstsHttpHeader;
import com.jinbooks.context.WebContext;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 请求域名限定Filter
 */
@Slf4j
public class WebHttpRestrictHostRequestFilter  extends GenericFilterBean {

	ConcurrentMap<String,String> restrictHostMap ;

	boolean isRestrict;

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
			throws IOException, ServletException {
		log.trace("WebHttpRestrictHostRequestFilter");
		HttpServletRequest request= ((HttpServletRequest)servletRequest);
		if(log.isTraceEnabled()) {WebContext.printRequest(request);}

		String host = request.getHeader(ConstsHttpHeader.HEADER_HOST);
		if(StringUtils.isBlank(host)) {
			host = request.getHeader(ConstsHttpHeader.HEADER_HOSTNAME);
		}
		log.trace("host {}",host);

		if(host.indexOf(":")> -1 ) {
			host = host.split(":")[0];
			log.trace("host split {}",host);
		}

		//限制条件true and host不为空 and 不在限制的host请求 需要过滤
		if(isRestrict && StringUtils.isNotBlank(host) && !restrictHostMap.containsKey(host)) {
			log.error("host {} is restrict",host);
			return;
		}

        chain.doFilter(servletRequest, servletResponse);
	}

	public WebHttpRestrictHostRequestFilter(List<String> restrictHosts) {
		restrictHostMap = new ConcurrentHashMap<>();
		for(String restrictHost : restrictHosts) {
			if(StringUtils.isNotBlank(restrictHost)) {
				restrictHostMap.put(restrictHost, restrictHost);
				isRestrict = true;
			}
		}
		log.debug("isRestrict {}",isRestrict);
		if(isRestrict) {
			log.debug("Restrict Host {}",restrictHostMap);
		}
	}

}
