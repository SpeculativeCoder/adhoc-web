package adhoc.system.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.event.Level;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Component
@Slf4j
public class AdhocExceptionHandlerExceptionResolver extends ExceptionHandlerExceptionResolver {

    @Override
    public void setWarnLogCategory(@NonNull String loggerName) {
        // ignore - we do our own logging below
    }

    @Override
    public ModelAndView resolveException(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, Object handler, @NonNull Exception exception) {

        log.debug("resolveException: method={} uri={}",
                request.getMethod(), request.getRequestURI(), exception);

        ModelAndView modelAndView = super.resolveException(request, response, handler, exception);

        boolean typical = exception instanceof NoResourceFoundException; // invalid static resource attempts
        // exception instanceof EntityNotFoundException // row not found in database

        Level level = Level.INFO;
        if (typical && !(request.getRequestURI().startsWith("/api/")
                || request.getRequestURI().startsWith("/ws/"))) {
            level = Level.DEBUG;
        }
        LoggingEventBuilder logEvent = log.atLevel(level);
        if (!typical) {
            logEvent = logEvent.setCause(exception);
        }
        logEvent.log("Request failure: method={} uri={} status={} exception={}",
                request.getMethod(), request.getRequestURI(), response.getStatus(), exception.getClass().getSimpleName());

        return modelAndView;
    }
}
