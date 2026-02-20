/*
 * Copyright 2022-2026 SpeculativeCoder (https://github.com/SpeculativeCoder)
 *
 * Modification of code from Spring Boot (https://spring.io/projects/spring-boot)
 * which is subject to Apache License, Version 2.0 (see adjacent *.LICENSE file)
 *
 * Copyright 2012-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package adhoc.system.log.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.CompositeConverter;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiElement;
import org.springframework.boot.ansi.AnsiOutput;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Modified version of {@link org.springframework.boot.logging.logback.ColorConverter}
 * to add more coloring options and highlight any log messages from our loggers.
 *
 * @author Phillip Webb (original {@link org.springframework.boot.logging.logback.ColorConverter})
 * @author <a href="https://github.com/SpeculativeCoder">SpeculativeCoder</a> (modified {@link AdhocColorLogbackConverter})
 */
public class AdhocColorLogbackConverter extends CompositeConverter<ILoggingEvent> {

    private static final Map<String, AnsiElement> COLOR_ELEMENTS;

    private static final Map<Integer, AnsiElement> LOG_LEVEL_ELEMENTS;
    private static final Map<Integer, AnsiElement> ADHOC_LOG_LEVEL_ELEMENTS;

    static {
        Map<String, AnsiElement> colorElements = new HashMap<>();
        //colorElements.put("faint", AnsiStyle.FAINT);
        colorElements.put("red", AnsiColor.RED);
        colorElements.put("green", AnsiColor.GREEN);
        colorElements.put("yellow", AnsiColor.YELLOW);
        colorElements.put("blue", AnsiColor.BLUE);
        colorElements.put("magenta", AnsiColor.MAGENTA);
        colorElements.put("cyan", AnsiColor.CYAN);
        colorElements.put("white", AnsiColor.WHITE);
        colorElements.put("black", AnsiColor.BLACK);
        colorElements.put("bright_red", AnsiColor.BRIGHT_RED);
        colorElements.put("bright_green", AnsiColor.BRIGHT_GREEN);
        colorElements.put("bright_yellow", AnsiColor.BRIGHT_YELLOW);
        colorElements.put("bright_blue", AnsiColor.BRIGHT_BLUE);
        colorElements.put("bright_magenta", AnsiColor.BRIGHT_MAGENTA);
        colorElements.put("bright_cyan", AnsiColor.BRIGHT_CYAN);
        colorElements.put("bright_white", AnsiColor.BRIGHT_WHITE);
        colorElements.put("bright_black", AnsiColor.BRIGHT_BLACK);
        COLOR_ELEMENTS = Collections.unmodifiableMap(colorElements);

        Map<Integer, AnsiElement> logLevelElements = new HashMap<>();
        logLevelElements.put(Level.ERROR_INTEGER, AnsiColor.RED);
        logLevelElements.put(Level.WARN_INTEGER, AnsiColor.YELLOW);
        logLevelElements.put(Level.INFO_INTEGER, AnsiColor.WHITE);
        logLevelElements.put(Level.DEBUG_INTEGER, AnsiColor.WHITE);
        logLevelElements.put(Level.TRACE_INTEGER, AnsiColor.WHITE);
        LOG_LEVEL_ELEMENTS = Collections.unmodifiableMap(logLevelElements);

        Map<Integer, AnsiElement> adhocLogLevelElements = new HashMap<>();
        adhocLogLevelElements.put(Level.ERROR_INTEGER, AnsiColor.RED);
        adhocLogLevelElements.put(Level.WARN_INTEGER, AnsiColor.YELLOW);
        adhocLogLevelElements.put(Level.INFO_INTEGER, AnsiColor.GREEN);
        adhocLogLevelElements.put(Level.DEBUG_INTEGER, AnsiColor.DEFAULT);
        adhocLogLevelElements.put(Level.TRACE_INTEGER, AnsiColor.DEFAULT);
        ADHOC_LOG_LEVEL_ELEMENTS = Collections.unmodifiableMap(adhocLogLevelElements);
    }

    @Override
    protected String transform(ILoggingEvent event, String in) {

        AnsiElement element = COLOR_ELEMENTS.get(getFirstOption());
        if (element == null) {
            boolean adhocLog = event.getLoggerName().contains("adhoc.");
            element = adhocLog ? ADHOC_LOG_LEVEL_ELEMENTS.get(event.getLevel().toInteger()) : LOG_LEVEL_ELEMENTS.get(event.getLevel().toInteger());
            element = (element != null) ? element : AnsiColor.DEFAULT;
        }
        return toAnsiString(in, element);
    }

    protected String toAnsiString(String in, AnsiElement element) {
        return AnsiOutput.toString(element, in);
    }
}
