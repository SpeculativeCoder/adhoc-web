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

package adhoc.system.logback;

import adhoc.system.util.LogUtils;
import ch.qos.logback.classic.pattern.RootCauseFirstThrowableProxyConverter;
import ch.qos.logback.classic.spi.IThrowableProxy;
import lombok.RequiredArgsConstructor;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

// TODO: consider a one-line exception?
public class AdhocSpecialCharsLogbackThrowableProxyConverter extends RootCauseFirstThrowableProxyConverter {

    @Override
    @SuppressWarnings("CallToPrintStackTrace")
    protected String throwableProxyToString(IThrowableProxy tp) {
        try {
            tp = (IThrowableProxy) Enhancer.create(IThrowableProxy.class, new ThrowableProxyMethodInterceptor(tp));
        } catch (Throwable e) {
            // any enhancer issues just output to stderr before it gets eaten by logback
            e.printStackTrace();
            throw e;
        }
        return super.throwableProxyToString(tp);
    }

    @RequiredArgsConstructor
    private static class ThrowableProxyMethodInterceptor implements MethodInterceptor {

        private final IThrowableProxy throwableProxy;

        @Override
        public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
            Object result = proxy.invoke(throwableProxy, args);

            if ("getMessage".equals(method.getName()) && result instanceof String text) {
                result = LogUtils.replaceSpecialChars(text);
            }
            return result;
        }
    }
}
