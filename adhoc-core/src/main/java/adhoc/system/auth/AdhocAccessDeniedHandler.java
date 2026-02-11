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

package adhoc.system.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.slf4j.event.Level;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdhocAccessDeniedHandler implements AccessDeniedHandler {

    //private final UserAuthService userAuthService;

    @Override
    public void handle(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull AccessDeniedException exception) throws IOException, ServletException {

        String method = request.getMethod();
        String uri = request.getRequestURI();

        log.atTrace()
                .addKeyValue("method", method)
                .addKeyValue("uri", uri)
                .log("handle:", exception);

        //userAuthService.onAccessDenied(method, uri, exception);

        boolean exceptionKnown = exception instanceof MissingCsrfTokenException
                || exception instanceof InvalidCsrfTokenException;

        boolean uriApi = uri.startsWith("/adhoc_api/")
                || uri.startsWith("/adhoc_ws/");

        LoggingEventBuilder logEvent = log.atLevel(!exceptionKnown ? Level.WARN : (uriApi ? Level.INFO : Level.DEBUG))
                .addKeyValue("method", method)
                .addKeyValue("uri", uri)
                .addKeyValue("exception", exception);
        if (!exceptionKnown) {
            logEvent = logEvent.setCause(exception);
        }

        int status = HttpStatus.FORBIDDEN.value();
        String message = HttpStatus.FORBIDDEN.getReasonPhrase();

        logEvent.log("Access denied: status={}", status);

        response.sendError(status, message);
    }
}
