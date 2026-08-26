package com.financial.cloud.authn.endpoint;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.financial.cloud.domain.config.Institutions;
import com.financial.cloud.common.Message;
import com.financial.cloud.service.config.InstitutionsService;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping(value = "/api/inst")
public class InstitutionEndpoint {

	public static final String  HEADER_HOST 		= "host";

	public static final String  HEADER_HOSTNAME 	= "hostname";

	private final InstitutionsService institutionsService;

	/**
	 * 根据header参数读取机构信息
	 * @param request
	 * @param originURL
	 * @param headerHostName
	 * @param headerHost
	 * @return inst
	 */
 	@GetMapping(value={"/get"}, produces = {MediaType.APPLICATION_JSON_VALUE})
	public Message<Institutions> get(
			HttpServletRequest request,
			@RequestHeader(value = "Origin",required=false) String originURL,
			@RequestHeader(value = HEADER_HOSTNAME,required=false) String headerHostName,
			@RequestHeader(value = HEADER_HOST,required=false) String headerHost) {
 		log.debug("get Institution" );

		String host = headerHostName;
		log.trace("hostname {}",host);
		if(StringUtils.isEmpty(host)) {
			host = headerHost;
			log.trace("host {}",host);
		}

		if(host.indexOf(":")> -1 ) {
			host = host.split(":")[0];
			log.trace("domain split {}",host);
		}

		Institutions inst = institutionsService.getByInstIdOrDomain(host);
		if(inst != null) {
			log.debug("inst {}",inst);
			return new Message<>(inst);
		}else {
			Institutions defaultInst = institutionsService.getByInstIdOrDomain("1");
			log.debug("default inst {}",inst);
			return new Message<>(defaultInst);
		}
 	}
}
