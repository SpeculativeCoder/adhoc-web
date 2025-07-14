package adhoc.system.error;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.event.Level;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Component
@Slf4j
public class AdhocExceptionHandlerExceptionResolver extends ExceptionHandlerExceptionResolver {

    @Override
    public ModelAndView resolveException(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, Object handler, @NonNull Exception exception) {
        ModelAndView modelAndView = super.resolveException(request, response, handler, exception);

        String method = request.getMethod();
        String uri = request.getRequestURI();

        Level level = Level.WARN;
        if (exception instanceof EntityNotFoundException) { // row not found in database
            level = Level.INFO;
        } else if (exception instanceof NoResourceFoundException) { // invalid static resource attempts
            level = Level.DEBUG;
        }

        log.atLevel(level).log("resolveException: exception={} method={} uri={}",
                exception.getClass().getSimpleName(), method, uri, exception);

        return modelAndView;
    }
}
