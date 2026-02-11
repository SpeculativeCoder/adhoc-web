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

package adhoc.system.exception;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.jspecify.annotations.NonNull;
import org.slf4j.event.Level;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;

@Component
@Slf4j
public class AdhocExceptionHandlerExceptionResolver extends ExceptionHandlerExceptionResolver {

    // TODO
    //@Override
    //public void setWarnLogCategory(String loggerName) {
    //    // we do our own warn logging so does not need to be enabled
    //    throw new UnsupportedOperationException();
    //}

    @Override
    public ModelAndView resolveException(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, Object handler, @NonNull Exception exception) {

        String method = request.getMethod();
        String uri = request.getRequestURI();

        log.atTrace()
                .addKeyValue("method", method)
                .addKeyValue("uri", uri)
                .log("resolveException:", exception);

        ModelAndView modelAndView = super.resolveException(request, response, handler, exception);

        var exceptionClasses = Throwables.getCausalChain(exception).stream().map(Throwable::getClass).toList();
        boolean exceptionKnown = ImmutableList.of(AsyncRequestNotUsableException.class, ClientAbortException.class, IOException.class).equals(exceptionClasses)
                || ImmutableList.of(ClientAbortException.class, IOException.class).equals(exceptionClasses)
                || ImmutableList.of(MethodArgumentNotValidException.class).equals(exceptionClasses)
                || ImmutableList.of(NoResourceFoundException.class).equals(exceptionClasses);

        boolean uriApi = uri.startsWith("/adhoc_api/")
                || uri.startsWith("/adhoc_ws/");

        LoggingEventBuilder logEvent = log.atLevel(!exceptionKnown ? Level.WARN : (uriApi ? Level.INFO : Level.DEBUG))
                .addKeyValue("method", method)
                .addKeyValue("uri", uri)
                .addKeyValue("exception", exception);
        if (!exceptionKnown) {
            logEvent = logEvent.setCause(exception);
        }
        logEvent.log("Request failure: status={}", response.getStatus());

        return modelAndView;
    }
}
