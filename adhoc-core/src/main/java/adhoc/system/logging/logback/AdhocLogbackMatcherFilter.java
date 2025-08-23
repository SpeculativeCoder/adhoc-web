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

package adhoc.system.logging.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.AbstractMatcherFilter;
import ch.qos.logback.core.spi.FilterReply;

public class AdhocLogbackMatcherFilter extends AbstractMatcherFilter<ILoggingEvent> {

    @Override
    public FilterReply decide(ILoggingEvent event) {

        String loggerName = event.getLoggerName();
        String message = event.getMessage();
        Level level = event.getLevel();

        if ("org.apache.activemq.artemis.core.server".equals(loggerName)
                && level.toInt() > Level.DEBUG.toInt()) {

            if (message.startsWith("AMQ224037: cluster connection Failed to handle message")) {
                return FilterReply.DENY;
            }

            if (message.startsWith("AMQ224091: Bridge ClusterConnectionBridge") && message.endsWith("is unable to connect to destination. Retrying")) {
                return FilterReply.DENY;
            }
        }

        if ("org.hibernate.engine.jdbc.spi.SqlExceptionHelper".equals(loggerName)
                && level.toInt() > Level.DEBUG.toInt()) {

            // suppress no data warnings from UPDATEs etc. which don't update any columns
            if ("SQL Warning Code: -1100, SQLState: 02000".equals(message)
                    || "no data".equals(message)) {
                return FilterReply.DENY;
            }

            // pessimistic locking failure messages
            if ("SQL Error: -4861, SQLState: 40001".equals(message)
                    || "transaction rollback: serialization failure".equals(message)) {
                return FilterReply.DENY;
            }

            // deadlocks
            if ("SQL Error: 40001, SQLState: 40001".equals(message)
                    || message.startsWith("Deadlock detected. The current transaction was rolled back. Details: ")) {
                return FilterReply.DENY;
            }

            // deadlocks
            if ("SQL Error: 0, SQLState: 40P01".equals(message)
                    || message.startsWith("ERROR: deadlock detected")) {
                return FilterReply.DENY;
            }
        }

        if ("org.hibernate.orm.jdbc.batch".equals(loggerName)
                && level.toInt() > Level.DEBUG.toInt()) {

            // batch insert failures due to concurrency
            if ("HHH100503: On release of batch it still contained JDBC statements".equals(message)) {
                return FilterReply.DENY;
            }
        }

        if ("org.springframework.messaging.simp.stomp.StompBrokerRelayMessageHandler".equals(loggerName)
                && level.toInt() > Level.DEBUG.toInt()) {

            // web browser stomp disconnection failures
            if (message.startsWith("Failed to forward DISCONNECT ")) {
                return FilterReply.DENY;
            }
        }

        if ("org.hibernate.SQL".equals(loggerName)) {

            if (message.startsWith("select")
                    || message.startsWith("/* select")
                    || message.startsWith("/* <criteria> */ select")) {
                return FilterReply.DENY;
            }
        }

        return FilterReply.NEUTRAL;
    }
}
