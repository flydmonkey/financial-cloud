package com.jinbooks.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

class ApiV1PathRewriteFilterTest {

    @Test
    void stripsApiV1Prefix() throws Exception {
        ApiV1PathRewriteFilter filter = new ApiV1PathRewriteFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/jinbooks-api/api/v1/book/fetch");
        request.setContextPath("/jinbooks-api");
        request.setServletPath("/api/v1/book/fetch");
        MockHttpServletResponse response = new MockHttpServletResponse();
        final ServletRequest[] seen = new ServletRequest[1];
        FilterChain chain = (req, res) -> seen[0] = req;
        filter.doFilter(request, response, chain);
        assertInstanceOf(ApiV1PathRewriteFilter.RewrittenRequest.class, seen[0]);
        assertEquals("/jinbooks-api/book/fetch", ((jakarta.servlet.http.HttpServletRequest) seen[0]).getRequestURI());
        assertEquals("/book/fetch", ((jakarta.servlet.http.HttpServletRequest) seen[0]).getServletPath());
    }

    @Test
    void leavesNonV1PathsUntouched() throws Exception {
        ApiV1PathRewriteFilter filter = new ApiV1PathRewriteFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/jinbooks-api/book/fetch");
        request.setContextPath("/jinbooks-api");
        request.setServletPath("/book/fetch");
        MockHttpServletResponse response = new MockHttpServletResponse();
        final ServletRequest[] seen = new ServletRequest[1];
        FilterChain chain = (req, res) -> seen[0] = req;
        filter.doFilter(request, response, chain);
        assertSame(request, seen[0]);
    }
}
