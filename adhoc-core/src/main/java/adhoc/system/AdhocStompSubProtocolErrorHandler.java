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

package adhoc.system;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.event.Level;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

import java.util.stream.Collectors;

/** Stomp exception logging. */
@Component
@Slf4j
public class AdhocStompSubProtocolErrorHandler extends StompSubProtocolErrorHandler {

    @Override
    public @Nullable Message<byte[]> handleClientMessageProcessingError(@Nullable Message<byte[]> clientMessage, @NonNull Throwable exception) {
        log.atTrace()
                .addKeyValue("clientMessage", clientMessage)
                .log("handleClientMessageProcessingError:", exception);

        Message<byte[]> message = super.handleClientMessageProcessingError(clientMessage, exception);

        var exceptionChain = Throwables.getCausalChain(exception).stream().map(Throwable::getClass).toList();
        boolean exceptionKnown = ImmutableList.of(MessageDeliveryException.class, InvalidCsrfTokenException.class).equals(exceptionChain);

        LoggingEventBuilder logEvent = log.atLevel(!exceptionKnown ? Level.WARN : Level.INFO)
                .addKeyValue("exceptionChain", exceptionChain.stream().map(Class::getSimpleName).collect(Collectors.joining("->")))
                .addKeyValue("clientMessage", clientMessage);
        if (!exceptionKnown) {
            logEvent = logEvent.setCause(exception);
        }
        logEvent.log("Stomp failure.");

        return message;
    }

    @Override
    public @Nullable Message<byte[]> handleErrorMessageToClient(@NonNull Message<byte[]> errorMessage) {
        log.atTrace()
                .addKeyValue("errorMessage", errorMessage)
                .log("handleErrorMessageToClient:");

        Message<byte[]> message = super.handleErrorMessageToClient(errorMessage);

        LoggingEventBuilder logEvent = log.atLevel(Level.INFO)
                .addKeyValue("errorMessage", errorMessage);
        logEvent.log("Stomp issue.");

        return message;
    }
}
