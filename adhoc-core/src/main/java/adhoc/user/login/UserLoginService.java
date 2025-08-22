package adhoc.user.login;

import adhoc.system.auth.AdhocAuthenticationSuccessHandler;
import adhoc.user.User;
import adhoc.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class UserLoginService {

    private final UserRepository userRepository;

    /**
     * Called by {@link AdhocAuthenticationSuccessHandler}. Sets a new "token" every time a user logs in.
     * The "token" is used when logging into an Unreal server to make sure the user is who they say they are.
     */
    @Retryable(retryFor = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 100, maxDelay = 1000))
    public void onAuthenticationSuccess(Long userId) {
        User user = userRepository.getReferenceById(userId);

        UUID newToken = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        user.getState().setToken(newToken);
        user.setLastLogin(now);

        log.debug("Authentication success: id={} name={} human={} token={}", user.getId(), user.getName(), user.isHuman(), user.getState().getToken());
    }
}
