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

package adhoc.system.retry;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.LockAcquisitionException;
import org.slf4j.event.Level;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.context.ApplicationListener;
import org.springframework.core.retry.RetryException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.resilience.retry.MethodRetryEvent;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AdhocRetryListener implements ApplicationListener<MethodRetryEvent> {

    @Override
    public void onApplicationEvent(MethodRetryEvent event) {

        // we will log only for the transient exceptions
        Throwable exception = event.getFailure();
        if (exception instanceof RetryException e) {
            exception = e.getCause();
        }

        boolean exceptionRelevant = exception instanceof TransientDataAccessException
                || exception instanceof LockAcquisitionException;

        Level level;
        if (exceptionRelevant && event.isRetryAborted()) {
            level = Level.WARN;
        } else if (exceptionRelevant) {
            level = Level.DEBUG;
        } else {
            level = Level.TRACE;
        }

        LoggingEventBuilder logEvent = log.atLevel(level)
                .addKeyValue("method", event.getMethod())
                .addKeyValue("exception", exception);
        // include full stack trace for cases where all retries of these transient exceptions failed
        //if (exceptionRelevant && event.isRetryAborted()) {
        //    logEvent = logEvent.setCause(exception);
        //}
        logEvent.log(event.isRetryAborted() ? "Abort." : "Retry.");
    }

    @Override
    public boolean supportsAsyncExecution() {
        // prefer this to be run in the thread which is doing the retry
        return false;
    }
}
