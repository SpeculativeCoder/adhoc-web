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

package adhoc.system;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.event.Level;
import org.springframework.http.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Extension of default Spring MVC exception handler to gracefully handle some additional exceptions. */
@Slf4j
@ControllerAdvice
public class AdhocExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    @Nullable
    public ResponseEntity<Object> handleEntityNotFoundException(Exception exception, WebRequest webRequest) {
        EntityNotFoundException entityNotFoundException = (EntityNotFoundException) exception;
        HttpStatus httpStatus = HttpStatus.NOT_FOUND;
        ProblemDetail problemDetail = createProblemDetail(exception, httpStatus, entityNotFoundException.getMessage(), null, null, webRequest);
        return handleExceptionInternal(exception, problemDetail, new HttpHeaders(), httpStatus, webRequest);
    }

    //@ExceptionHandler(Throwable.class)
    //@Nullable
    //public ResponseEntity<Object> handleThrowable(Exception exception, WebRequest webRequest) {
    //    HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
    //    ProblemDetail problemDetail = createProblemDetail(exception, httpStatus, httpStatus.getReasonPhrase(), null, null, webRequest);
    //    HttpHeaders httpHeaders = (exception instanceof ErrorResponse errorResponse)
    //            ? errorResponse.getHeaders()
    //            : new HttpHeaders();
    //    return handleExceptionInternal(exception, problemDetail, httpHeaders, httpStatus, webRequest);
    //}

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(@NonNull Exception exception, Object body, @NonNull HttpHeaders httpHeaders, @NonNull HttpStatusCode statusCode, @NonNull WebRequest webRequest) {

        ResponseEntity<Object> responseEntity = super.handleExceptionInternal(exception, body, httpHeaders, statusCode, webRequest);

        String status = "?";
        HttpStatusCode httpStatusCode = null;
        if (responseEntity != null) {
            httpStatusCode = responseEntity.getStatusCode();
            status = String.valueOf(httpStatusCode.value());
        }

        String method = "?";
        String uri = "?";
        if (webRequest instanceof ServletWebRequest servletWebRequest) {
            HttpServletRequest request = servletWebRequest.getRequest();
            method = request.getMethod();
            uri = request.getRequestURI();
        }

        Level level = Level.WARN;

        if (exception instanceof NoResourceFoundException) {
            level = Level.DEBUG;
        }

        log.atLevel(level).log("Handled: exception={} status={} method={} uri={}", exception.getClass().getSimpleName(), status, method, uri, exception);
        //LogFormatUtils.formatValue(exception, 500, true)

        return responseEntity;
    }
}
