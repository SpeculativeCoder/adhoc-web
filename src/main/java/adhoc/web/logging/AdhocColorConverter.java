/*
 * Copyright 2022-2023 SpeculativeCoder (https://github.com/SpeculativeCoder)
 *
 * Modification of code from Spring Boot (https://spring.io/projects/spring-boot) - subject to Apache License, Version 2.0 (see adjacent *-LICENSE file)
 * https://github.com/spring-projects/spring-boot/blob/main/spring-boot-project/spring-boot/src/main/java/org/springframework/boot/logging/logback/ColorConverter.java
 * https://github.com/spring-projects/spring-boot/blob/main/LICENSE.txt
 *
 * Copyright 2012-2019 the original author or authors.
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

package adhoc.web.logging;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.CompositeConverter;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiElement;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.ansi.AnsiStyle;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Modified version of {@link org.springframework.boot.logging.logback.ColorConverter}
 * to add more coloring options and highlight any log messages from our loggers.
 *
 * @author Phillip Webb (original {@link org.springframework.boot.logging.logback.ColorConverter})
 * @author <a href="https://github.com/SpeculativeCoder">SpeculativeCoder</a> (modified {@link AdhocColorConverter})
 */
public class AdhocColorConverter extends CompositeConverter<ILoggingEvent> {

    private static final Map<String, AnsiElement> ELEMENTS;
    private static final Map<String, AnsiElement> BRIGHT_ELEMENTS;

    private static final Map<Integer, AnsiElement> LEVELS;
    private static final Map<Integer, AnsiElement> BRIGHT_LEVELS;

    static {
        Map<String, AnsiElement> ansiElements = new HashMap<>();
        ansiElements.put("faint", AnsiStyle.FAINT);
        ansiElements.put("red", AnsiColor.RED);
        ansiElements.put("green", AnsiColor.GREEN);
        ansiElements.put("yellow", AnsiColor.YELLOW);
        ansiElements.put("blue", AnsiColor.BLUE);
        ansiElements.put("magenta", AnsiColor.MAGENTA);
        ansiElements.put("cyan", AnsiColor.CYAN);
        ansiElements.put("white", AnsiColor.WHITE);
        ansiElements.put("bright_black", AnsiColor.BRIGHT_BLACK);
        ELEMENTS = Collections.unmodifiableMap(ansiElements);

        Map<String, AnsiElement> brightAnsiElements = new HashMap<>();
        brightAnsiElements.put("faint", AnsiStyle.FAINT);
        brightAnsiElements.put("red", AnsiColor.BRIGHT_RED);
        brightAnsiElements.put("green", AnsiColor.BRIGHT_GREEN);
        brightAnsiElements.put("yellow", AnsiColor.BRIGHT_YELLOW);
        brightAnsiElements.put("blue", AnsiColor.BRIGHT_BLUE);
        brightAnsiElements.put("magenta", AnsiColor.BRIGHT_MAGENTA);
        brightAnsiElements.put("cyan", AnsiColor.BRIGHT_CYAN);
        brightAnsiElements.put("white", AnsiColor.BRIGHT_WHITE);
        brightAnsiElements.put("bright_black", AnsiColor.DEFAULT);
        BRIGHT_ELEMENTS = Collections.unmodifiableMap(brightAnsiElements);

        Map<Integer, AnsiElement> ansiLevels = new HashMap<>();
        ansiLevels.put(Level.ERROR_INTEGER, AnsiColor.RED);
        ansiLevels.put(Level.WARN_INTEGER, AnsiColor.YELLOW);
        ansiLevels.put(Level.INFO_INTEGER, AnsiColor.DEFAULT);
        ansiLevels.put(Level.DEBUG_INTEGER, AnsiColor.WHITE);
        ansiLevels.put(Level.TRACE_INTEGER, AnsiColor.BRIGHT_BLACK);
        LEVELS = Collections.unmodifiableMap(ansiLevels);

        Map<Integer, AnsiElement> brightAnsiLevels = new HashMap<>();
        brightAnsiLevels.put(Level.ERROR_INTEGER, AnsiColor.BRIGHT_RED);
        brightAnsiLevels.put(Level.WARN_INTEGER, AnsiColor.BRIGHT_YELLOW);
        brightAnsiLevels.put(Level.INFO_INTEGER, AnsiColor.BRIGHT_WHITE);
        brightAnsiLevels.put(Level.DEBUG_INTEGER, AnsiColor.DEFAULT);
        brightAnsiLevels.put(Level.TRACE_INTEGER, AnsiColor.WHITE);
        BRIGHT_LEVELS = Collections.unmodifiableMap(brightAnsiLevels);
    }

    @Override
    protected String transform(ILoggingEvent event, String in) {
        boolean adhocLog = event.getLoggerName().contains("adhoc.");

        AnsiElement element = adhocLog ? BRIGHT_ELEMENTS.get(getFirstOption()) : ELEMENTS.get(getFirstOption());
        //AnsiElement element = ELEMENTS.get(getFirstOption());
        if (element == null) {
            element = adhocLog ? BRIGHT_LEVELS.get(event.getLevel().toInteger()) : LEVELS.get(event.getLevel().toInteger());
            // element = LEVELS.get(event.getLevel().toInteger());
            element = (element != null) ? element : AnsiColor.GREEN;
        }
        return toAnsiString(in, element);
    }

    protected String toAnsiString(String in, AnsiElement element) {
        return AnsiOutput.toString(element, in);
    }


}
