/*
 * Copyright (c) 2022-2026 SpeculativeCoder (https://github.com/SpeculativeCoder)
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

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.slf4j.event.Level;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.stream.Collectors;

@Component
@Slf4j
public class AdhocAuthenticationFailureHandler implements AuthenticationFailureHandler {

    //@Setter(onMethod_ = {@Autowired}, onParam_ = {@Lazy})
    //private UserAuthService userAuthService;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull AuthenticationException exception) throws IOException, ServletException {

        String method = request.getMethod();
        String uri = request.getRequestURI();

        log.atTrace()
                .addKeyValue("method", method)
                .addKeyValue("uri", uri)
                .log("onAuthenticationFailure:", exception);

        //userAuthService.onAuthenticationFailure(exception);

        Authentication authentication = exception.getAuthenticationRequest();
        //Verify.verifyNotNull(authentication);

        var exceptionClasses = Throwables.getCausalChain(exception).stream().map(Throwable::getClass).toList();
        boolean exceptionKnown = ImmutableList.of(BadCredentialsException.class, UsernameNotFoundException.class).equals(exceptionClasses)
                || ImmutableList.of(BadCredentialsException.class).equals(exceptionClasses);
        //|| ImmutableList.of(DisabledException.class).equals(exceptionClasses);

        int status = HttpStatus.UNAUTHORIZED.value();
        String message = HttpStatus.UNAUTHORIZED.getReasonPhrase();

        LoggingEventBuilder logEvent = log.atLevel(!exceptionKnown ? Level.WARN : Level.INFO)
                .addKeyValue("status", status)
                .addKeyValue("method", method)
                .addKeyValue("uri", uri)
                .addKeyValue("principal", authentication == null ? null : authentication.getPrincipal())
                .addKeyValue("chain", exceptionClasses.stream().map(Class::getSimpleName).collect(Collectors.joining("->")));
        if (!exceptionKnown) {
            logEvent = logEvent
                    .addKeyValue("authentication", authentication)
                    .setCause(exception);
        }
        logEvent.log("Authentication failure:");

        response.sendError(status, message);
    }
}
