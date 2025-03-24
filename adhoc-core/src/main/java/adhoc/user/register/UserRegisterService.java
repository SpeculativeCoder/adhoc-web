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

package adhoc.user.register;

import adhoc.faction.FactionRepository;
import adhoc.region.RegionRepository;
import adhoc.system.properties.CoreProperties;
import adhoc.user.*;
import adhoc.user.programmaticlogin.ProgrammaticLoginService;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.LockAcquisitionException;
import org.slf4j.event.Level;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.lang.Nullable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class UserRegisterService {

    private final CoreProperties coreProperties;

    private final UserRepository userRepository;
    private final FactionRepository factionRepository;
    private final RegionRepository regionRepository;

    private final UserService userService;
    private final ProgrammaticLoginService programmaticLoginService;

    private final HttpServletRequest httpServletRequest;
    private final PasswordEncoder passwordEncoder;

    @Retryable(retryFor = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 100, maxDelay = 1000))
    public UserFullDto userRegister(UserRegisterRequest userRegisterRequest) {
        String userAgent = determineUserAgent();
        String remoteAddr = determineRemoteAddr();

        log.debug("userRegister: name={} password?={} human={} factionId={} remoteAddr={} userAgent={}",
                userRegisterRequest.getName(),
                userRegisterRequest.getPassword() != null,
                userRegisterRequest.getHuman(),
                userRegisterRequest.getFactionId(),
                remoteAddr,
                userAgent);

        if (!coreProperties.getFeatureFlags().contains("development")) {
            Preconditions.checkArgument(userRegisterRequest.getEmail() == null, "Registering with email not allowed yet");
            Preconditions.checkArgument(userRegisterRequest.getPassword() == null, "Registering with password not allowed yet");
            Preconditions.checkArgument(userRegisterRequest.getName() == null, "Registering with name not allowed yet");
        }

        boolean authenticatedAsServer = isAuthenticatedAsServer();

        Preconditions.checkArgument(userRegisterRequest.getHuman() != null);
        // human can only register user as human, but server may register users as human or bot
        Preconditions.checkArgument(userRegisterRequest.getHuman() || authenticatedAsServer);

        // TODO: think about existing name/email check before allowing name/email input
        Optional<User> existingUser;
        if (userRegisterRequest.getName() != null && userRegisterRequest.getEmail() != null) {
            existingUser = userRepository.findByNameOrEmail(userRegisterRequest.getName(), userRegisterRequest.getEmail());
        } else if (userRegisterRequest.getName() != null) {
            existingUser = userRepository.findByName(userRegisterRequest.getName());
        } else {
            existingUser = Optional.empty();
        }

        if (existingUser.isPresent()) {
            log.warn("User name or email already in use: name={} email={}", userRegisterRequest.getName(), userRegisterRequest.getEmail());
            throw new IllegalArgumentException("User name or email already in use");
        }

        UserRegisterRequest.UserRegisterRequestBuilder builder = userRegisterRequest.toBuilder();

        if (userRegisterRequest.getName() == null) {
            String prefix = userRegisterRequest.getHuman() ? "Anon" : "Bot";
            builder.name(prefix + (int) Math.floor(Math.random() * 1000000000)); // TODO
        }

        if (userRegisterRequest.getFactionId() == null) {
            builder.factionId(1 + (long) Math.floor(Math.random() * factionRepository.count()));
        }

        userRegisterRequest = builder.build();

        User user = userRepository.save(toEntity(userRegisterRequest));

        // if not an auto-register from server - log them in too
        if (!authenticatedAsServer) {
            programmaticLoginService.programmaticLogin(user.getId(), userRegisterRequest.getName(), userRegisterRequest.getPassword());
        }

        log.atLevel(Optional.ofNullable(userRegisterRequest.getHuman()).orElse(false) ? Level.INFO : Level.DEBUG)
                .log("User registered: id={} name={} password?={} human={} factionIndex={} remoteAddr={} userAgent={}",
                        user.getId(),
                        user.getName(),
                        user.getPassword() != null,
                        user.isHuman(),
                        user.getFaction().getIndex(),
                        remoteAddr,
                        userAgent);

        return userService.toFullDto(user);
    }

    private @Nullable String determineRemoteAddr() {
        return httpServletRequest.getRemoteAddr();
    }

    private @Nullable String determineUserAgent() {
        String userAgent = httpServletRequest.getHeader("user-agent");
        return userAgent == null ? null : userAgent.replaceAll("[^A-Za-z0-9 _()/;:,.+\\-]", "?");
    }

    private static boolean isAuthenticatedAsServer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.getAuthorities().stream().anyMatch(authority -> "ROLE_SERVER".equals(authority.getAuthority()));
    }

    private User toEntity(UserRegisterRequest userRegisterRequest) {
        User user = new User();

        user.setName(userRegisterRequest.getName());
        user.setEmail(userRegisterRequest.getEmail());
        user.setPassword(userRegisterRequest.getPassword() == null ? null
                : passwordEncoder.encode(userRegisterRequest.getPassword()));
        user.setHuman(userRegisterRequest.getHuman());
        user.setFaction(factionRepository.getReferenceById(userRegisterRequest.getFactionId()));
        user.setScore(BigDecimal.valueOf(0.0));
        user.setRoles(Sets.newHashSet(UserRole.USER));
        user.setToken(UUID.randomUUID());
        user.setNavigated(LocalDateTime.now());

        return user;
    }
}
