package com.financial.cloud.filter;


import lombok.extern.slf4j.Slf4j;
import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

import com.financial.cloud.constants.common.ConstsHttpHeader;
import com.financial.cloud.context.WebConstants;
import com.financial.cloud.context.WebContext;
import com.financial.cloud.domain.config.Institutions;
import com.financial.cloud.service.config.InstitutionsService;

/**
 * 多租户机构读取Filter
 */
@Slf4j
public class WebHttpInstRequestFilter  extends GenericFilterBean {

	InstitutionsService institutionsService;

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
		}
        chain.doFilter(servletRequest, servletResponse);
	}

	public WebHttpInstRequestFilter(InstitutionsService institutionsService) {
		super();
		this.institutionsService = institutionsService;
	}

}
