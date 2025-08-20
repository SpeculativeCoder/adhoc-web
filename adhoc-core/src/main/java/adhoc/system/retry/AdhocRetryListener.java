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
import org.slf4j.event.Level;
import org.springframework.retry.RetryContext;
import org.springframework.retry.interceptor.MethodInvocationRetryCallback;
import org.springframework.retry.listener.MethodInvocationRetryListenerSupport;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AdhocRetryListener extends MethodInvocationRetryListenerSupport {

    @Override
    protected <T, E extends Throwable> void doOnError(RetryContext context, MethodInvocationRetryCallback<T, E> callback, Throwable throwable) {
        Level level;
        if (context.getRetryCount() >= 3) {
            level = Level.WARN;
        } else if (context.getRetryCount() >= 2) {
            level = Level.INFO;
        } else {
            level = Level.DEBUG;
        }
        log.atLevel(level).log("Retry: label={} context={}", callback.getLabel(), context);
    }
}
