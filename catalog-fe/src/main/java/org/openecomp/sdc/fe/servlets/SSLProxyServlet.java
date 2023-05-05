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
package org.openecomp.sdc.fe.servlets;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.dynamic.HttpClientTransportDynamic;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.proxy.ProxyServlet;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.onap.config.api.JettySSLUtils;
import org.openecomp.sdc.common.api.Constants;
import org.openecomp.sdc.fe.config.Configuration;
import org.openecomp.sdc.fe.config.ConfigurationManager;
import org.openecomp.sdc.fe.utils.BeProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;

public abstract class SSLProxyServlet extends ProxyServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(SSLProxyServlet.class);

    @Override
    protected HttpClient createHttpClient() throws ServletException {
        Configuration config = ((ConfigurationManager) getServletConfig().getServletContext().getAttribute(Constants.CONFIGURATION_MANAGER_ATTR))
                .getConfiguration();
        boolean isSecureClient = !config.getBeProtocol().equals(BeProtocol.HTTP.getProtocolName());
        HttpClient client = (isSecureClient) ? getSecureHttpClient() : super.createHttpClient();
        int requestTimeout = config.getRequestTimeout() * 1000;
        if (requestTimeout == 0) {
            requestTimeout = 1200_000;
        }
        setTimeout(requestTimeout);
        client.setIdleTimeout(requestTimeout);
        return client;
    }

    private HttpClient getSecureHttpClient() throws ServletException {
        final SslContextFactory.Client sslContextFactory = new SslContextFactory.Client(true);
        try {
            sslContextFactory.setSslContext(JettySSLUtils.getSslContext());
        } catch (Exception e) {
            LOGGER.error("Exception thrown while getting SslContext", e);
            throw new ServletException(e);
        }
        final ClientConnector clientConnector = new ClientConnector();
        clientConnector.setSslContextFactory(sslContextFactory);
        final HttpClient httpClient = new HttpClient(new HttpClientTransportDynamic(clientConnector));
        // Configure HttpClient, for example:
        httpClient.setFollowRedirects(false);
        // Start HttpClient
        try {
            httpClient.start();
        } catch (Exception x) {
            LOGGER.error("Exception thrown while starting httpClient", x);
            throw new ServletException(x);
        }
        return httpClient;
    }

}
