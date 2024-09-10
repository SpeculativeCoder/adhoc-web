/*
 * Copyright (c) 2022-2024 SpeculativeCoder (https://github.com/SpeculativeCoder)
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
import lombok.extern.slf4j.Slf4j;
import org.slf4j.event.Level;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.*;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Extension of default Spring MVC exception handler to gracefully handle some additional exceptions. */
@Slf4j
@ControllerAdvice
public class AdhocResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    @Nullable
    public ResponseEntity<Object> handleEntityNotFoundException(Exception exception, WebRequest webRequest) {
        EntityNotFoundException entityNotFoundException = (EntityNotFoundException) exception;
        HttpStatus httpStatus = HttpStatus.NOT_FOUND;
        ProblemDetail problemDetail = createProblemDetail(exception, httpStatus, entityNotFoundException.getMessage(), null, null, webRequest);
        return handleExceptionInternal(exception, problemDetail, new HttpHeaders(), httpStatus, webRequest);
    }

    @ExceptionHandler(Throwable.class)
    @Nullable
    public ResponseEntity<Object> handleThrowable(Exception exception, WebRequest webRequest) {
        HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        ProblemDetail problemDetail = createProblemDetail(exception, httpStatus, httpStatus.getReasonPhrase(), null, null, webRequest);
        return handleExceptionInternal(exception, problemDetail, new HttpHeaders(), httpStatus, webRequest);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        Level level = (ex instanceof NoResourceFoundException) ? Level.DEBUG : Level.WARN;
        log.atLevel(level).log("{}", LogFormatUtils.formatValue(ex, -1, true));
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }
}
