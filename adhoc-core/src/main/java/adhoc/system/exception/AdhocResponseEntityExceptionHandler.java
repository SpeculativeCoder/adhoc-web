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

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Extension of default Spring MVC exception handler to gracefully handle some additional exceptions. */
@ControllerAdvice
@Slf4j
public class AdhocResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Object> handleEntityNotFoundException(EntityNotFoundException exception, WebRequest webRequest) {
        HttpStatus httpStatus = HttpStatus.UNPROCESSABLE_CONTENT;
        ProblemDetail problemDetail = createProblemDetail(exception, httpStatus, exception.getMessage(), null, null, webRequest);
        return handleExceptionInternal(exception, problemDetail, new HttpHeaders(), httpStatus, webRequest);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgumentException(IllegalArgumentException exception, WebRequest webRequest) {
        HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
        ProblemDetail problemDetail = createProblemDetail(exception, httpStatus, exception.getMessage(), null, null, webRequest);
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

    //@Override
    //protected ResponseEntity<Object> handleExceptionInternal(@NonNull Exception exception, Object body, @NonNull HttpHeaders httpHeaders, @NonNull HttpStatusCode statusCode, @NonNull WebRequest webRequest) {
    //
    //    String method = "?";
    //    String uri = "?";
    //    if (webRequest instanceof ServletWebRequest servletWebRequest) {
    //        HttpServletRequest request = servletWebRequest.getRequest();
    //        method = request.getMethod();
    //        uri = request.getRequestURI();
    //    }
    //    log.debug("handleExceptionInternal: exception={} method={} uri={}",
    //            exception.getClass().getName(), method, uri, exception);
    //
    //    ResponseEntity<Object> responseEntity = super.handleExceptionInternal(exception, body, httpHeaders, statusCode, webRequest);
    //
    //    String status = "?";
    //    HttpStatusCode httpStatusCode;
    //    if (responseEntity != null) {
    //        httpStatusCode = responseEntity.getStatusCode();
    //        status = String.valueOf(httpStatusCode.value());
    //    }
    //    log.debug("handleExceptionInternal: status={}", status);
    //
    //    return responseEntity;
    //}
}
