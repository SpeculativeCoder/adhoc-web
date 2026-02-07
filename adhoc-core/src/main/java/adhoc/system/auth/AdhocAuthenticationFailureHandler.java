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
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.slf4j.event.Level;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class AdhocAuthenticationFailureHandler implements AuthenticationFailureHandler {

    //@Setter(onMethod_ = {@Autowired}, onParam_ = {@Lazy})
    //private UserAuthService userAuthService;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull AuthenticationException exception) throws IOException, ServletException {

        //String method = request.getMethod();
        //String uri = request.getRequestURI();

        log.atTrace()
                //.addKeyValue("method", method)
                //.addKeyValue("uri", uri)
                .log("onAuthenticationFailure:", exception);

        //userAuthService.onAuthenticationFailure(exception);

        Authentication authentication = exception.getAuthenticationRequest();
        //Verify.verifyNotNull(authentication);

        boolean exceptionKnown = exception instanceof BadCredentialsException
                || exception instanceof DisabledException;

        LoggingEventBuilder logEvent = log.atLevel(!exceptionKnown ? Level.WARN : Level.INFO)
                //.addKeyValue("method", method)
                //.addKeyValue("uri", uri)
                .addKeyValue("authentication", authentication)
                .addKeyValue("exception", exception);
        if (!exceptionKnown) {
            logEvent = logEvent.setCause(exception);
        }

        int status = HttpStatus.UNAUTHORIZED.value();
        String message = HttpStatus.UNAUTHORIZED.getReasonPhrase();

        logEvent.log("Authentication failure: status={}", status);

        response.sendError(status, message);
    }
}
