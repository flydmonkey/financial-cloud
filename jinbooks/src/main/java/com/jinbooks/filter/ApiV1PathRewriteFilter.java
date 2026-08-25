/*
 * Copyright [2025] [JinBooks of copyright http://www.jinbooks.com]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.jinbooks.filter;

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
