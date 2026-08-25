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

package com.jinbooks.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

import com.jinbooks.filter.ApiV1PathRewriteFilter;

@AutoConfiguration
public class ApiV1PathRewriteAutoConfiguration {

    @Bean
    public FilterRegistrationBean<ApiV1PathRewriteFilter> apiV1PathRewriteFilter() {
        FilterRegistrationBean<ApiV1PathRewriteFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new ApiV1PathRewriteFilter());
        bean.addUrlPatterns("/*");
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        bean.setName("apiV1PathRewriteFilter");
        return bean;
    }
}
