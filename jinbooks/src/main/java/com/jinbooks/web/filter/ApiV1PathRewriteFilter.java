package com.jinbooks.web.filter;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Maps /api/v1/** onto existing controller paths without changing @RequestMapping.
 */
public class ApiV1PathRewriteFilter extends OncePerRequestFilter {

    public static final String API_V1_PREFIX = "/api/v1";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.startsWith(API_V1_PREFIX + "/") || path.equals(API_V1_PREFIX)) {
            String newPath = path.equals(API_V1_PREFIX) ? "/" : path.substring(API_V1_PREFIX.length());
            filterChain.doFilter(new RewrittenRequest(request, newPath), response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    static final class RewrittenRequest extends HttpServletRequestWrapper {
        private final String newPath;

        RewrittenRequest(HttpServletRequest request, String newPath) {
            super(request);
            this.newPath = newPath;
        }

        @Override
        public String getRequestURI() {
            return getContextPath() + newPath;
        }

        @Override
        public String getServletPath() {
            return newPath;
        }

        @Override
        public String getPathInfo() {
            return null;
        }
    }
}
