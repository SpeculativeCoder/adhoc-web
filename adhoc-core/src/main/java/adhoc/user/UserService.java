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

package adhoc.user;

import adhoc.region.RegionEntity;
import adhoc.server.ServerEntity;
import adhoc.shared.properties.CoreProperties;
import adhoc.shared.random_uuid.RandomUUIDUtils;
import adhoc.system.auth.AdhocAuthenticationSuccessHandler;
import adhoc.system.auth.AdhocUserDetails;
import adhoc.user.current.CurrentUserDto;
import adhoc.user.state.UserStateEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.LockAcquisitionException;
import org.slf4j.event.Level;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final CoreProperties coreProperties;

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<UserDto> findUsers(Pageable pageable) {

        return userRepository.findAll(pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<UserDto> findUser(Long userId) {

        return userRepository.findById(userId).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<CurrentUserDto> findCurrentUser(Authentication authentication) {

        if (authentication == null || !(authentication.getPrincipal() instanceof AdhocUserDetails currentUserDetails)) {
            return Optional.empty();
        }

        return userRepository.findById(currentUserDetails.getUserId()).map(this::toCurrentUserDto);
    }

    /**
     * Called by {@link AdhocAuthenticationSuccessHandler}. Sets a new "token" every time a user logs in.
     * The "token" will need to be provided to the Unreal server so we can make sure the user is who they say they are.
     */
    @Retryable(includes = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxRetries = 3, delay = 100, jitter = 10, multiplier = 1, maxDelay = 1000)
    public void authenticationSuccess(Long userId) {

        UserEntity user = userRepository.getReferenceById(userId);

        UUID newToken = RandomUUIDUtils.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        user.getState().setToken(newToken);
        user.setLastLogin(now);

        log.atLevel(Level.INFO)
                .addKeyValue("name", user.getName())
                .log("Authentication success: userId={} human={}", user.getId(), user.isHuman());

        log.debug("authenticationSuccess: token={}", user.getState().getToken());
    }

    UserDto toDto(UserEntity user) {
        return new UserDto(
                user.getId(),
                user.getVersion(),
                user.getName(),
                user.isHuman(),
                user.getFaction().getId(),
                user.getScore(),
                Optional.ofNullable(user.getState()).map(UserStateEntity::getRegion).map(RegionEntity::getId).orElse(null),
                Optional.ofNullable(user.getState()).map(UserStateEntity::getSeen).orElse(null),
                user.getUserRoles().stream().map(UserRole::name).collect(Collectors.toList()),
                Optional.ofNullable(user.getState()).map(UserStateEntity::getDestinationServer).map(ServerEntity::getId).orElse(null),
                Optional.ofNullable(user.getState()).map(UserStateEntity::getServer).map(ServerEntity::getId).orElse(null));
    }

    CurrentUserDto toCurrentUserDto(UserEntity user) {

        String quickLoginCode = user.getName() + "-" + user.getQuickLoginPassword(coreProperties.getQuickLoginPasswordEncryptionKey());

        return new CurrentUserDto(
                user.getId(),
                user.getVersion(),
                user.getName(),
                quickLoginCode,
                user.isHuman(),
                user.getFaction().getId(),
                user.getScore(),
                Optional.ofNullable(user.getState()).map(UserStateEntity::getRegion).map(RegionEntity::getId).orElse(null),
                user.getUserRoles().stream().map(UserRole::name).collect(Collectors.toList()),
                Optional.ofNullable(user.getState()).map(UserStateEntity::getDestinationServer).map(ServerEntity::getId).orElse(null),
                Optional.ofNullable(user.getState()).map(UserStateEntity::getServer).map(ServerEntity::getId).orElse(null));
    }
}
