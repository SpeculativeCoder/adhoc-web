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

import adhoc.server.ServerEntity;
import adhoc.server.ServerRepository;
import adhoc.user.requests.UserJoinRequest;
import adhoc.user.requests.UserRegisterRequest;
import adhoc.user.responses.UserJoinResponse;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.LockAcquisitionException;
import org.slf4j.event.Level;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class UserJoinService {

    private final UserRepository userRepository;
    private final ServerRepository serverRepository;

    private final UserService userService;
    private final UserRegisterService userRegisterService;

    @Retryable(includes = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxRetries = 3, delay = 100, jitter = 10, multiplier = 1, maxDelay = 1000)
    public UserJoinResponse userJoin(UserJoinRequest userJoinRequest) {

        ServerEntity server = serverRepository.getReferenceById(userJoinRequest.getServerId());

        UserEntity user;
        // existing user? verify token
        if (userJoinRequest.getUserId() != null) {
            user = userRepository.getReferenceById(userJoinRequest.getUserId());

            Preconditions.checkArgument(Objects.equals(user.getFaction().getId(), userJoinRequest.getFactionId()),
                    "Faction ID mismatch: %s != %s", user.getFaction().getId(), userJoinRequest.getFactionId());

            Preconditions.checkArgument(userJoinRequest.getToken() != null, "Token missing");
            Verify.verifyNotNull(user.getState().getToken(), "User has no token");

            // TODO: in addition to token - we should check validity of player login (e.g. are they meant to even be in the area?)
            if (!Objects.equals(user.getState().getToken().toString(), userJoinRequest.getToken())) {
                log.warn("Token {} mismatch {} for user {}", userJoinRequest.getToken(), user.getState().getToken(), user);
                throw new IllegalArgumentException("Token mismatch");
            }

        } else {
            user = autoRegister(userJoinRequest.getFactionId(), userJoinRequest.getHuman());
        }

        user.getState().setRegion(server.getRegion());
        user.getState().setServer(server);
        user.getState().setDestinationServer(server);
        user.setLastJoin(LocalDateTime.now());
        user.getState().setSeen(user.getLastJoin());

        log.atLevel(user.isHuman() ? Level.INFO : Level.DEBUG)
                .addKeyValue("id", user.getId())
                .addKeyValue("name", user.getName())
                .addKeyValue("password?", user.getPassword() != null)
                .addKeyValue("human", user.isHuman())
                .addKeyValue("factionIndex", user.getFaction().getIndex())
                .addKeyValue("factionId", user.getFaction().getId())
                .addKeyValue("serverId", user.getState().getServer().getId())
                .addKeyValue("regionId", server.getRegion().getId())
                .addKeyValue("x", user.getState().getX())
                .addKeyValue("y", user.getState().getY())
                .addKeyValue("z", user.getState().getZ())
                .addKeyValue("pitch", user.getState().getPitch())
                .addKeyValue("yaw", user.getState().getYaw())
                .log("User joined.");

        return UserJoinResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .factionId(user.getFaction().getId())
                .x(user.getState().getX())
                .y(user.getState().getY())
                .z(user.getState().getZ())
                .pitch(user.getState().getPitch())
                .yaw(user.getState().getYaw())
                .serverId(user.getState().getServer().getId())
                .build();
    }

    private UserEntity autoRegister(Long factionId, Boolean human) {
        Verify.verifyNotNull(human);

        UserEntity user = null;

        if (!human) {
            // bots should try to use existing bot account
            // TODO: avoid using seen (should use serverId)
            user = userRepository.findFirstByHumanFalseAndFactionIdAndStateSeenBefore(
                    factionId, LocalDateTime.now().minusMinutes(1)).orElse(null);
        }

        if (user == null) {

            UserRegisterRequest userRegisterRequest = UserRegisterRequest.builder()
                    .factionId(factionId)
                    .human(human)
                    .build();

            user = userRegisterService.userRegisterInternal(userRegisterRequest).user();
        }

        return user;
    }
}
