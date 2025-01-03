/*
 * Copyright (c) 2022-2025 SpeculativeCoder (https://github.com/SpeculativeCoder)
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

package adhoc.system.logging;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.AbstractRequestLoggingFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;

/**
 * Logging component to enable logging of POSTs/PUTs etc.
 */
@Component
@Order(value = Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class LoggingRequestFilter extends AbstractRequestLoggingFilter {

    //@Value("${adhoc.server.basic-auth.username:#{null}}")
    //private Optional<String> serverBasicAuthUsername;

    //@Value("${adhoc.server.basic-auth.password:#{null}}")
    //private Optional<String> serverBasicAuthPassword;

    //private String encodedServerBasicAuth;

    public LoggingRequestFilter() {
        setIncludeQueryString(true);
        setIncludeHeaders(true);
        setIncludePayload(true);
        setMaxPayloadLength(2000);
        setIncludeClientInfo(true);
        // setHeaderPredicate(...);
    }

    @PostConstruct
    public void postConstruct() {
        //if (serverBasicAuthUsername.isPresent() && serverBasicAuthPassword.isPresent()) {
        //    encodedServerBasicAuth = HttpHeaders.encodeBasicAuth(serverBasicAuthUsername.get(), serverBasicAuthPassword.get(), null);
        //}
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request, getMaxPayloadLength());
        //ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(requestWrapper, response);

        } finally {

            //response.getStatus() != HttpStatus.SWITCHING_PROTOCOLS.value() // 101 (websocket causes this)
            //&& response.getStatus() != HttpStatus.OK.value() // 200
            //&& response.getStatus() != HttpStatus.CREATED.value() // 201
            //&& response.getStatus() != HttpStatus.NO_CONTENT.value() // 204
            //&& response.getStatus() != HttpStatus.NOT_MODIFIED.value() // 304
            //&& response.getStatus() != HttpStatus.NOT_FOUND.value() // 404
            //&& response.getStatus() != HttpStatus.METHOD_NOT_ALLOWED.value() // 405

            boolean statusError;
            try {
                HttpStatus httpStatus = HttpStatus.valueOf(response.getStatus());
                statusError = httpStatus.is5xxServerError(); // || httpStatus.value() == 400;

            } catch (
                    IllegalArgumentException e) {
                statusError = true;
            }

            boolean methodGet = "GET".equals(request.getMethod());

            //boolean userServer = false;
            //if (encodedServerBasicAuth != null) {
            //    String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            //    userServer = encodedServerBasicAuth != null
            //            && ("Basic " + encodedServerBasicAuth).equals(authorizationHeader);
            //}

            LoggingEventBuilder loggingEventBuilder = null;

            if (log.isWarnEnabled() && statusError) {
                loggingEventBuilder = log.atWarn();

                //} else if (log.isInfoEnabled() && isRegisterOrLoginRequest(request)) {
                //    loggingEventBuilder = log.atInfo();

            } else if (log.isDebugEnabled() && !methodGet) { //&& !userServer) {
                loggingEventBuilder = log.atDebug();

            } else if (log.isTraceEnabled()) { //&& !userServer) {
                loggingEventBuilder = log.atTrace();
            }

            if (loggingEventBuilder != null) {
                loggingEventBuilder.log("{}, status={}",
                        createMessage(requestWrapper, "", ""),
                        response.getStatus());
            }

            //responseWrapper.copyBodyToResponse();
        }
    }

    @Override
    protected void beforeRequest(HttpServletRequest request, String message) {
        // will do our own logging for now
    }

    @Override
    protected void afterRequest(HttpServletRequest request, String message) {
        // will do our own logging for now
    }

    @Override
    protected boolean shouldLog(HttpServletRequest request) {
        // will do our own logging for now
        return false;
    }

    @Override
    protected String getMessagePayload(HttpServletRequest request) {
        if (isRegisterOrLoginRequest(request)) {
            return "***";
        }
        return super.getMessagePayload(request);
    }

    private boolean isRegisterOrLoginRequest(HttpServletRequest request) {
        // TODO

        boolean methodPost = "POST".equals(request.getMethod());
        boolean uriRegister = "/api/users/register".equals(request.getRequestURI());
        boolean uriLogin = "/login".equals(request.getRequestURI());

        return methodPost && (uriRegister || uriLogin);
    }
}
