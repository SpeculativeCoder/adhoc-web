/*
 * Copyright (c) 2022-2024 SpeculativeCoder (https://github.com/SpeculativeCoder)
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

import adhoc.faction.FactionRepository;
import adhoc.region.RegionRepository;
import adhoc.server.ServerRepository;
import adhoc.user.request_response.UserRegisterRequest;
import com.google.common.collect.Sets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final FactionRepository factionRepository;
    private final ServerRepository serverRepository;
    private final RegionRepository regionRepository;

    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<UserDto> getUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public UserDto getUser(Long userId) {
        return toDto(userRepository.getReferenceById(userId));
    }

    @Transactional(readOnly = true)
    public UserFullDto getUserFull(Long userId) {
        return toFullDto(userRepository.getReferenceById(userId));
    }

    /**
     * Called by {@link UserAuthenticationSuccessHandler}. Sets a new "token" every time a user logs in.
     * The "token" is used when logging into an Unreal server to make sure the user is who they say they are.
     */
    @Retryable(retryFor = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 100, maxDelay = 1000))
    public void onAuthenticationSuccess(Long userId) {
        User user = userRepository.getReferenceById(userId);

        UUID newToken = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        user.setToken(newToken);
        user.setLastLogin(now);

        log.debug("onAuthenticationSuccess: id={} name={} human={} token={}", user.getId(), user.getName(), user.isHuman(), user.getToken());
    }

    UserDto toDto(User user) {
        return new UserDto(
                user.getId(),
                user.getVersion(),
                user.getName(),
                user.isHuman(),
                user.getFaction().getId(),
                user.getScore(),
                user.getRegion() == null ? null : user.getRegion().getId(),
                user.getSeen(),
                user.getDestinationServer() == null ? null : user.getDestinationServer().getId(),
                user.getServer() == null ? null : user.getServer().getId());
    }

    UserFullDto toFullDto(User user) {
        return new UserFullDto(
                user.getId(),
                user.getVersion(),
                user.getName(),
                user.isHuman(),
                user.getFaction().getId(),
                user.getScore(),
                user.getRegion().getId(),
                user.getX(),
                user.getY(),
                user.getZ(),
                user.getPitch(),
                user.getYaw(),
                user.getCreated(),
                user.getUpdated(),
                user.getLastLogin(),
                user.getNavigated(),
                user.getLastJoin(),
                user.getSeen(),
                user.getRoles().stream().map(UserRole::name).collect(Collectors.toList()),
                user.getToken().toString(),
                user.getDestinationServer() == null ? null : user.getDestinationServer().getId(),
                user.getServer() == null ? null : user.getServer().getId());
    }

    User toEntity(UserRegisterRequest userRegisterRequest) {
        User user = new User();

        user.setName(userRegisterRequest.getName());
        user.setEmail(userRegisterRequest.getEmail());
        user.setPassword(userRegisterRequest.getPassword() == null ? null
                : passwordEncoder.encode(userRegisterRequest.getPassword()));
        user.setHuman(userRegisterRequest.getHuman());
        user.setFaction(factionRepository.getReferenceById(userRegisterRequest.getFactionId()));
        user.setScore(BigDecimal.valueOf(0.0));
        user.setRegion(regionRepository.getReferenceById(userRegisterRequest.getRegionId()));
        user.setRoles(Sets.newHashSet(UserRole.USER));
        user.setToken(UUID.randomUUID());
        user.setDestinationServer(userRegisterRequest.getDestinationServerId() == null ? null
                : serverRepository.getReferenceById(userRegisterRequest.getDestinationServerId()));
        user.setNavigated(LocalDateTime.now());

        return user;
    }
}
