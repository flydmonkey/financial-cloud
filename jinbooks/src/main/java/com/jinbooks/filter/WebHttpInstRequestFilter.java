package com.jinbooks.filter;


import lombok.extern.slf4j.Slf4j;
import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

import com.jinbooks.configuration.ApplicationConfig;
import com.jinbooks.constants.ConstsHttpHeader;
import com.jinbooks.context.WebConstants;
import com.jinbooks.context.WebContext;
import com.jinbooks.domain.config.Institutions;
import com.jinbooks.service.config.InstitutionsService;

/**
 * 多租户机构读取Filter
 */
@Slf4j
public class WebHttpInstRequestFilter  extends GenericFilterBean {

	InstitutionsService institutionsService;

	ApplicationConfig applicationConfig;

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
			throws IOException, ServletException {
		log.trace("WebHttpInstRequestFilter");
		HttpServletRequest request= ((HttpServletRequest)servletRequest);

		if(request.getSession().getAttribute(WebConstants.CURRENT_INST) == null) {
			if(log.isTraceEnabled()) {WebContext.printRequest(request);}
			String host = request.getHeader(ConstsHttpHeader.HEADER_HOSTNAME);
			log.trace("hostname {}",host);
			if(StringUtils.isEmpty(host)) {
				host = request.getHeader(ConstsHttpHeader.HEADER_HOST);
				log.trace("host {}",host);
			}
			if(host.indexOf(":")> -1 ) {
				host = host.split(":")[0];
				log.trace("domain split {}",host);
			}
			Institutions institution = institutionsService.getByInstIdOrDomain(host);
			log.trace("institution {}" ,institution);
			request.getSession().setAttribute(WebConstants.CURRENT_INST, institution);

			String origin = request.getHeader(ConstsHttpHeader.HEADER_ORIGIN);
			if(StringUtils.isEmpty(origin)) {
				origin = applicationConfig.getFrontendUri();
			}
			log.trace("origin {}" ,origin);
		}
        chain.doFilter(servletRequest, servletResponse);
	}

	public WebHttpInstRequestFilter(InstitutionsService institutionsService,ApplicationConfig applicationConfig) {
		super();
		this.institutionsService = institutionsService;
		this.applicationConfig = applicationConfig;
	}

}
