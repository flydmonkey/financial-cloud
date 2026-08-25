package com.jinbooks.controller.auth;


import lombok.extern.slf4j.Slf4j;
import java.util.Map;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.webmvc.autoconfigure.error.AbstractErrorController;
import org.springframework.boot.webmvc.error.ErrorAttributes;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Exception.
 * 
 * @author Crystal.Sea
 *
 */
@Slf4j
@RestController
public class ExceptionController extends  AbstractErrorController    {
	
	static ErrorAttributeOptions errorAttributeOptions = ErrorAttributeOptions.of(
															ErrorAttributeOptions.Include.EXCEPTION,
															ErrorAttributeOptions.Include.STACK_TRACE,
															ErrorAttributeOptions.Include.MESSAGE,
															ErrorAttributeOptions.Include.BINDING_ERRORS
														);

	
	public ExceptionController(ErrorAttributes errorAttributes) {
		super(errorAttributes);
	}

	
    @GetMapping({ "/api/exception/error/400" })
    public ModelAndView error400(
            HttpServletRequest request, HttpServletResponse response) {
        log.debug("Exception BAD_REQUEST");
        return new ModelAndView("exception/400");
    }

    /**
     * //查看浏览器中的报错信息.
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @return
     */
    @GetMapping(value = { "/api/exception/error/404" })
    public ModelAndView error404(
            HttpServletRequest request, HttpServletResponse response) {
        log.debug("Exception PAGE NOT_FOUND ");
        return new ModelAndView("exception/404");
    }

    @GetMapping(value = { "/api/exception/error/500" })
    public ModelAndView error500(HttpServletRequest request, HttpServletResponse response) {
        log.debug("Exception INTERNAL_SERVER_ERROR ");
        Map<String, Object> attributes = getErrorAttributes(request, errorAttributeOptions);
        log.debug("Error attributes {} ",attributes);
        return new ModelAndView("exception/500",attributes);
    }

    @GetMapping(value = { "/api/exception/accessdeny" })
    public ModelAndView accessdeny(HttpServletRequest request, HttpServletResponse response) {
        log.debug("exception/accessdeny ");
        return new ModelAndView("exception/accessdeny");
    }
}
