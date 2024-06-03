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

import adhoc.server.Server;
import adhoc.server.ServerRepository;
import adhoc.user.request_response.ServerUserJoinRequest;
import adhoc.user.request_response.UserRegisterRequest;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.LockAcquisitionException;
import org.slf4j.event.Level;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Transactional
@Service
@Slf4j
@RequiredArgsConstructor
public class ManagerUserJoinService {

    private final UserRepository userRepository;
    private final ServerRepository serverRepository;

    private final UserService userService;
    private final UserRegisterService userRegisterService;

    @Retryable(retryFor = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 100, maxDelay = 1000))
    public UserDetailDto serverUserJoin(ServerUserJoinRequest serverUserJoinRequest) {
        log.debug("userJoin: userId={} human={} factionId={} serverId={}",
                serverUserJoinRequest.getUserId(), serverUserJoinRequest.getHuman(), serverUserJoinRequest.getFactionId(), serverUserJoinRequest.getServerId());

        Server server = serverRepository.getReferenceById(serverUserJoinRequest.getServerId());

        User user;
        // existing user? verify token
        if (serverUserJoinRequest.getUserId() != null) {
            user = userRepository.getReferenceById(serverUserJoinRequest.getUserId());
            Preconditions.checkArgument(Objects.equals(user.getFaction().getId(), serverUserJoinRequest.getFactionId()));

            Preconditions.checkArgument(serverUserJoinRequest.getToken() != null);
            Verify.verifyNotNull(user.getToken());

            // TODO: in addition to token - we should check validity of player login (e.g. are they meant to even be in the area?)
            if (!Objects.equals(user.getToken().toString(), serverUserJoinRequest.getToken())) {
                log.warn("Token {} mismatch {} for user {}", serverUserJoinRequest.getToken(), user.getToken(), user);
                throw new IllegalArgumentException("Token mismatch");
            }

        } else {
            user = autoRegister(serverUserJoinRequest);
        }

        user.setServer(server);
        user.setLastJoin(LocalDateTime.now());
        user.setSeen(user.getLastJoin());

        log.atLevel(user.isHuman() ? Level.INFO : Level.DEBUG)
                .log("User joined: userId={} userName={} userHuman={} factionId={} serverId={} regionId={} x={} y={} z={} pitch={} yaw={}",
                        user.getId(), user.getName(), user.isHuman(), user.getFaction().getIndex(), server.getId(), server.getRegion().getId(), user.getX(), user.getY(), user.getZ(), user.getPitch(), user.getYaw());

        return userService.toDetailDto(user);
    }

    private User autoRegister(ServerUserJoinRequest serverUserJoinRequest) {
        User user = null;

        Verify.verifyNotNull(serverUserJoinRequest.getHuman());
        if (!serverUserJoinRequest.getHuman()) {
            // bots should try to use existing bot account
            // TODO: avoid using seen (should use serverId)
            user = userRepository.findFirstByHumanFalseAndFactionIdAndSeenBefore(
                    serverUserJoinRequest.getFactionId(), LocalDateTime.now().minusMinutes(1)).orElse(null);
        }

        if (user == null) {
            UserRegisterRequest userRegisterRequest = UserRegisterRequest.builder()
                    .factionId(serverUserJoinRequest.getFactionId())
                    .human(serverUserJoinRequest.getHuman())
                    .serverId(serverUserJoinRequest.getServerId())
                    .build();

            UserDetailDto userDetailDto = userRegisterService.registerUser(userRegisterRequest);

            user = userRepository.getReferenceById(userDetailDto.getId());
        }

        return user;
    }
}
