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
import adhoc.system.properties.CoreProperties;
import adhoc.system.random_uuid.RandomUUIDUtils;
import adhoc.user.UserEntity;
import adhoc.user.UserFullDto;
import adhoc.user.UserRepository;
import adhoc.user.UserService;
import adhoc.user.UserStateEntity;
import adhoc.user.programmatic_login.ProgrammaticLoginService;
import adhoc.user.random_name.RandomNameUtils;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class UserRegisterService {

    private final CoreProperties coreProperties;

    private final UserRepository userRepository;
    private final FactionRepository factionRepository;

    private final UserService userService;
    private final ProgrammaticLoginService programmaticLoginService;

    private final HttpServletRequest httpServletRequest;
    private final PasswordEncoder passwordEncoder;

    @Retryable(retryFor = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 100, maxDelay = 1000))
    public UserFullDto userRegisterAndLogin(UserRegisterRequest userRegisterRequest) {

        UserEntity user = userRegisterInternal(userRegisterRequest);

        programmaticLoginService.programmaticLoginInternal(user, userRegisterRequest.getPassword());

        return userService.toFullDto(user);
    }

    public UserEntity userRegisterInternal(UserRegisterRequest userRegisterRequest) {

        String userAgent = determineUserAgent();
        String remoteAddr = determineRemoteAddr();
        log.debug("userRegister: remoteAddr={} userAgent={}", remoteAddr, userAgent);

        UserEntity user = new UserEntity();
        user.setState(new UserStateEntity());
        user.getState().setUser(user);

        user.setName(userRegisterRequest.getName());
        user.setEmail(userRegisterRequest.getEmail());
        user.setPassword(userRegisterRequest.getPassword(), passwordEncoder);

        // assume human user unless specified
        user.setHuman(userRegisterRequest.getHuman() == null || userRegisterRequest.getHuman());

        user.setFaction(userRegisterRequest.getFactionId() == null ? null : factionRepository.getReferenceById(userRegisterRequest.getFactionId()));

        if (!coreProperties.getFeatureFlags().contains("development")) {
            Preconditions.checkArgument(user.getEmail() == null, "Registering with email not allowed yet");
            Preconditions.checkArgument(user.getPassword() == null, "Registering with password not allowed yet");
            Preconditions.checkArgument(user.getName() == null, "Registering with name not allowed yet");
        }

        // TODO: think about existing name/email check before allowing name/email input
        if (user.getName() != null || user.getEmail() != null) {
            Optional<UserEntity> existingUser = userRepository.findByNameOrEmail(user.getName(), user.getEmail());
            if (existingUser.isPresent()) {
                log.warn("User name or email already in use. name={} email={}", user.getName(), user.getEmail());
                throw new IllegalArgumentException("User name or email already in use");
            }
        }

        if (user.getName() == null) {
            if (user.isHuman()) {
                user.setName(RandomNameUtils.randomName()); // TODO
            } else {
                user.setName("Bot" + System.currentTimeMillis()); // TODO
            }
        }

        if (user.getFaction() == null) {
            Long factionId = 1 + (long) Math.floor(Math.random() * factionRepository.count());
            user.setFaction(factionRepository.getReferenceById(factionId));
        }

        user.setScore(BigDecimal.valueOf(0.0));
        user.setRoles(Sets.newHashSet(UserEntity.Role.USER));
        user.getState().setToken(RandomUUIDUtils.randomUUID());

        user = userRepository.save(user);

        log.atLevel(user.isHuman() ? Level.INFO : Level.DEBUG)
                .addKeyValue("id", user.getId())
                .addKeyValue("name", user.getName())
                .addKeyValue("password?", user.getPassword() != null)
                .addKeyValue("human", user.isHuman())
                .addKeyValue("factionIndex", user.getFaction().getIndex())
                .addKeyValue("factionId", user.getFaction().getId())
                .addKeyValue("remoteAddr", remoteAddr)
                .addKeyValue("userAgent", userAgent)
                .log("User registered.");

        return user;
    }

    private @Nullable String determineRemoteAddr() {
        return httpServletRequest.getRemoteAddr();
    }

    private @Nullable String determineUserAgent() {
        String userAgent = httpServletRequest.getHeader("user-agent");
        return userAgent == null ? null : userAgent.replaceAll("[^A-Za-z0-9 _()/;:,.+\\-]", "?");
    }
}
