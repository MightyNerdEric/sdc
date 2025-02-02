/*-
 * ============LICENSE_START=======================================================
 * SDC
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */
package org.openecomp.sdc.be.filters;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import org.apache.http.HttpStatus;
import org.openecomp.sdc.be.components.impl.exceptions.ComponentException;
import org.openecomp.sdc.be.servlets.exception.ComponentExceptionMapper;
import org.openecomp.sdc.common.api.FilterDecisionEnum;
import org.openecomp.sdc.common.util.ThreadLocalsHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("gatewayFilter")
public class GatewayFilter implements Filter {

    @Autowired
    private ThreadLocalUtils threadLocalUtils;
    @Autowired
    private ComponentExceptionMapper componentExceptionMapper;

    public GatewayFilter() {
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) req;
        HttpServletResponse httpResponse = (HttpServletResponse) res;
        try {
            ThreadLocalsHolder.setApiType(FilterDecisionEnum.NA);
            threadLocalUtils.setUserContextFromDB(httpRequest);
            filterChain.doFilter(httpRequest, res);
        } catch (ComponentException ce) {
            componentExceptionMapper.writeToResponse(ce, httpResponse);
        } catch (WebApplicationException we) {
            httpResponse.setStatus(we.getResponse().getStatus());
            setDefaultHttpParams(httpResponse);
            httpResponse.getWriter().write(we.getMessage());
        } catch (Exception ex) {
            httpResponse.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            setDefaultHttpParams(httpResponse);
            httpResponse.getWriter().write(ex.getMessage());
        }
    }

    private void setDefaultHttpParams(HttpServletResponse httpResponse) {
        httpResponse.setContentType("application/json");
        httpResponse.setCharacterEncoding("UTF-8");
    }

    @Override
    public void destroy() {
    }
}
