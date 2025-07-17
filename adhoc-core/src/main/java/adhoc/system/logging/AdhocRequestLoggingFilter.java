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

import com.google.common.base.Verify;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.AbstractRequestLoggingFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Logging component to enable logging of POSTs/PUTs etc.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class AdhocRequestLoggingFilter extends AbstractRequestLoggingFilter {

    private static final Logger userLogger = LoggerFactory.getLogger(AdhocRequestLoggingFilter.class.getName() + ".user");
    private static final Logger serverLogger = LoggerFactory.getLogger(AdhocRequestLoggingFilter.class.getName() + ".server");

    @Value("${adhoc.server.basic-auth.username:#{null}}")
    private Optional<String> serverBasicAuthUsername;

    @Value("${adhoc.server.basic-auth.password:#{null}}")
    private Optional<String> serverBasicAuthPassword;

    private String encodedServerBasicAuth;

    public AdhocRequestLoggingFilter() {
        setIncludeQueryString(true);
        setIncludeHeaders(true);
        setIncludePayload(true);
        setMaxPayloadLength(2000);
        setIncludeClientInfo(true);
        setHeaderPredicate(
                header -> !"cookie".equalsIgnoreCase(header)
                        && !"x-csrf-token".equalsIgnoreCase(header));
    }

    @PostConstruct
    public void postConstruct() {
        if (serverBasicAuthUsername.isPresent() && serverBasicAuthPassword.isPresent()) {
            encodedServerBasicAuth = HttpHeaders.encodeBasicAuth(serverBasicAuthUsername.get(), serverBasicAuthPassword.get(), null);
        }
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Verify.verify(!isAsyncDispatch(request)); // not properly supported by this logger

        ContentCachingRequestWrapper requestWrapper = null;
        ContentCachingResponseWrapper responseWrapper = null;

        if (request.getRequestURI().startsWith("/api/")
                || request.getRequestURI().startsWith("/ws/")) {

            requestWrapper = new ContentCachingRequestWrapper(request, getMaxPayloadLength());
            responseWrapper = new ContentCachingResponseWrapper(response);
        }

        try {
            filterChain.doFilter(
                    requestWrapper != null ? requestWrapper : request,
                    responseWrapper != null ? responseWrapper : response);

        } finally {

            //response.getStatus() != HttpStatus.SWITCHING_PROTOCOLS.value() // 101 (websocket causes this)
            //&& response.getStatus() != HttpStatus.OK.value() // 200
            //&& response.getStatus() != HttpStatus.CREATED.value() // 201
            //&& response.getStatus() != HttpStatus.NO_CONTENT.value() // 204
            //&& response.getStatus() != HttpStatus.NOT_MODIFIED.value() // 304
            //&& response.getStatus() != HttpStatus.NOT_FOUND.value() // 404
            //&& response.getStatus() != HttpStatus.METHOD_NOT_ALLOWED.value() // 405

            boolean statusServerError;
            boolean statusBadRequest;
            try {
                HttpStatus httpStatus = HttpStatus.valueOf(response.getStatus());

                statusServerError = httpStatus.is5xxServerError();
                statusBadRequest = httpStatus == HttpStatus.BAD_REQUEST;

            } catch (IllegalArgumentException e) {
                statusServerError = true;
                statusBadRequest = true;
            }

            boolean methodGet = "GET".equals(request.getMethod());

            boolean authServer = false;
            if (encodedServerBasicAuth != null) {
                String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
                authServer = encodedServerBasicAuth != null
                        && ("Basic " + encodedServerBasicAuth).equals(authorizationHeader);
            }

            Logger logger = authServer ? serverLogger : userLogger;

            LoggingEventBuilder builder = null;

            if (logger.isWarnEnabled() && statusServerError) {
                builder = logger.atWarn();

            } else if (logger.isInfoEnabled() && statusBadRequest) {
                builder = logger.atInfo();

            } else if (logger.isDebugEnabled() && !methodGet) {
                builder = logger.atDebug();

            } else if (logger.isTraceEnabled()) {
                builder = logger.atTrace();
            }

            if (builder != null) {
                String body = null;
                if (responseWrapper != null) {
                    body = getResponseBody(responseWrapper);
                }

                builder.log("{}, status={}, response={}",
                        createMessage(requestWrapper != null ? requestWrapper : request, "", ""),
                        response.getStatus(), body);
            }

            Verify.verify(!isAsyncStarted(request)); // not properly supported by this logger

            if (responseWrapper != null) {
                responseWrapper.copyBodyToResponse();
            }
        }
    }

    private String getResponseBody(ContentCachingResponseWrapper responseWrapper) {
        int length = Math.min(responseWrapper.getContentSize(), getMaxPayloadLength());
        return new String(responseWrapper.getContentAsByteArray(), 0, length, StandardCharsets.UTF_8);
    }

    @Override
    protected void beforeRequest(@NonNull HttpServletRequest request, @NonNull String message) {
        // will do our own logging for now
    }

    @Override
    protected void afterRequest(@NonNull HttpServletRequest request, @NonNull String message) {
        // will do our own logging for now
    }

    @Override
    protected boolean shouldLog(@NonNull HttpServletRequest request) {
        // will do our own logging for now
        return false;
    }

    @Override
    protected String getMessagePayload(@NonNull HttpServletRequest request) {
        if (isRegisterOrLogin(request)) {
            return "***";
        }
        return super.getMessagePayload(request);
    }

    private boolean isRegisterOrLogin(HttpServletRequest request) {
        // TODO

        boolean methodPost = "POST".equals(request.getMethod());
        boolean uriRegister = "/api/users/register".equals(request.getRequestURI());
        boolean uriLogin = "/api/login".equals(request.getRequestURI());

        return methodPost && (uriRegister || uriLogin);
    }
}
