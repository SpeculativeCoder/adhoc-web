/*
 * Copyright (c) 2022-2023 SpeculativeCoder (https://github.com/SpeculativeCoder)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package adhoc.web.logging;

import adhoc.ManagerProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.AbstractRequestLoggingFilter;

/**
 * Logging component to enable logging of POSTS/PUTS etc.
 */
@Component
@Slf4j
public class AdhocRequestLoggingFilter extends AbstractRequestLoggingFilter {

    @Autowired(required = false)
    private ManagerProperties managerProperties;

    @Autowired
    private HttpServletResponse httpServletResponse;

    public AdhocRequestLoggingFilter() {
        setIncludeQueryString(true);
        setIncludeHeaders(true);
        setIncludePayload(true);
        setMaxPayloadLength(2000);
        setIncludeClientInfo(true);
        // setHeaderPredicate(...);
    }

    @Override
    protected void beforeRequest(HttpServletRequest request, String message) {
        //log.debug("{}", message);
    }

    @Override
    protected void afterRequest(HttpServletRequest request, String message) {
        log.debug("{} status={}", message, httpServletResponse.getStatus());
    }

    @Override
    protected boolean shouldLog(HttpServletRequest request) {

        boolean debugLogging = log.isDebugEnabled();

        boolean errorStatus = httpServletResponse.getStatus() != HttpStatus.OK.value()
                && httpServletResponse.getStatus() != HttpStatus.CREATED.value();

        //boolean stompTraffic = (request.getRequestURI() != null && request.getRequestURI().contains("/stomp/"));

        boolean getRequest = "GET".equals(request.getMethod());

        boolean serverUser = (managerProperties != null && request.getUserPrincipal() != null
                && managerProperties.getServerBasicAuthUsername().equals(request.getUserPrincipal().getName()));

        return debugLogging && (errorStatus
                //|| stompTraffic
                || (!getRequest && !serverUser));
    }
}
