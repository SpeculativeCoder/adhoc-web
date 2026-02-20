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

package adhoc.system.exception;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.slf4j.event.Level;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

@Component
@Slf4j
public class AdhocStompSubProtocolErrorHandler extends StompSubProtocolErrorHandler {

    @NonNull
    @Override
    protected Message<byte[]> handleInternal(@NonNull StompHeaderAccessor errorHeaderAccessor, byte @NonNull [] errorPayload, Throwable exception, StompHeaderAccessor clientHeaderAccessor) {

        log.trace("handleInternal:", exception);

        Message<byte[]> message = super.handleInternal(errorHeaderAccessor, errorPayload, exception, clientHeaderAccessor);

        if (exception != null) {
            var exceptionClasses = Throwables.getCausalChain(exception).stream().map(Throwable::getClass).toList();
            boolean exceptionKnown = ImmutableList.of(MessageDeliveryException.class, InvalidCsrfTokenException.class).equals(exceptionClasses);

            LoggingEventBuilder logEvent = log.atLevel(!exceptionKnown ? Level.WARN : Level.INFO)
                    .addKeyValue("exception", exception);
            if (!exceptionKnown) {
                logEvent = logEvent.setCause(exception);
            }
            logEvent.log("Stomp failure:");
        }
        //else {
        //    // TODO
        //    log.debug("Stomp failure");
        //}

        return message;
    }
}
