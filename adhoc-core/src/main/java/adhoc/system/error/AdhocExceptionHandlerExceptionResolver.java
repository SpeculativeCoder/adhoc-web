package adhoc.system.error;

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

        Level level = Level.INFO;
        if (!(request.getRequestURI().startsWith("/api/")
                || request.getRequestURI().startsWith("/ws/"))
                && (exception instanceof NoResourceFoundException)) { // invalid static resource attempts
                //exception instanceof EntityNotFoundException // row not found in database
            level = Level.DEBUG;
        }

        log.atLevel(level).log("resolveException: exception={} status={} method={} uri={}",
                exception.getClass().getSimpleName(), response.getStatus(), request.getMethod(), request.getRequestURI(), exception);

        return modelAndView;
    }
}
