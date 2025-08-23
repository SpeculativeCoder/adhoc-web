package adhoc.user.auth;

import adhoc.system.auth.AdhocAuthenticationFailureHandler;
import adhoc.system.auth.AdhocAuthenticationSuccessHandler;
import adhoc.system.auth.AdhocUserDetails;
import adhoc.user.User;
import adhoc.user.UserRepository;
import com.google.common.base.Verify;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.LockAcquisitionException;
import org.slf4j.event.Level;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class UserAuthenticateService {

    private final UserRepository userRepository;

    /**
     * Called by {@link AdhocAuthenticationSuccessHandler}. Sets a new "token" every time a user logs in.
     * The "token" is used when logging into an Unreal server to make sure the user is who they say they are.
     */
    @Retryable(retryFor = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 100, maxDelay = 1000))
    public void onAuthenticationSuccess(Authentication authentication) {

        Object principal = authentication.getPrincipal();
        Verify.verify(principal instanceof AdhocUserDetails);
        AdhocUserDetails userDetails = (AdhocUserDetails) principal;

        User user = userRepository.getReferenceById(userDetails.getUserId());

        UUID newToken = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        user.getState().setToken(newToken);
        user.setLastLogin(now);

        log.debug("Authentication success: id={} name={} human={} token={}", user.getId(), user.getName(), user.isHuman(), user.getState().getToken());
    }

    /**
     * Called by {@link AdhocAuthenticationFailureHandler}.
     */
    public void onAuthenticationFailure(AuthenticationException exception) {

        Authentication authentication = exception.getAuthenticationRequest();
        //Verify.verifyNotNull(authentication);

        boolean typical = exception instanceof BadCredentialsException;

        Level level = Level.INFO;
        // TODO
        if (typical) {
            level = Level.DEBUG;
        }
        LoggingEventBuilder logEvent = log.atLevel(level);
        if (!typical) {
            logEvent = logEvent.setCause(exception);
        }
        logEvent.log("Authentication failure: authentication={} exception={}",
                authentication, exception.getClass().getSimpleName());
    }
}
