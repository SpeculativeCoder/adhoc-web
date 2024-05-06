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

import adhoc.area.Area;
import adhoc.area.AreaRepository;
import adhoc.server.Server;
import adhoc.server.ServerRepository;
import adhoc.user.event.ServerUserDefeatedUserEvent;
import adhoc.user.event.UserDefeatedUserEvent;
import adhoc.user.request_response.RegisterUserRequest;
import adhoc.user.request_response.UserJoinRequest;
import adhoc.user.request_response.UserNavigateRequest;
import adhoc.user.request_response.UserNavigateResponse;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.LockAcquisitionException;
import org.slf4j.event.Level;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

@Transactional
@Service
@Slf4j
@RequiredArgsConstructor
public class UserManagerService {

    private final UserRepository userRepository;
    private final ServerRepository serverRepository;
    private final AreaRepository areaRepository;

    private final UserService userService;

    public UserDto updateUser(UserDto userDto) {
        return userService.toDto(
                toEntity(userDto, userRepository.getReferenceById(userDto.getId())));
    }

    private User toEntity(UserDto userDto, User user) {
        // TODO
        //user.setName(userDto.getName());
        //user.setFaction(user.getFaction());

        user.setUpdated(LocalDateTime.now());

        return user;
    }

    @Retryable(retryFor = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 100, maxDelay = 1000))
    public UserDetailDto serverUserJoin(UserJoinRequest userJoinRequest) {
        log.debug("userJoin: userId={} human={} factionId={} serverId={}",
                userJoinRequest.getUserId(), userJoinRequest.getHuman(), userJoinRequest.getFactionId(), userJoinRequest.getServerId());

        Server server = serverRepository.getReferenceById(userJoinRequest.getServerId());

        User user;
        // existing user? verify token
        if (userJoinRequest.getUserId() != null) {
            user = userRepository.getReferenceById(userJoinRequest.getUserId());
            Preconditions.checkArgument(Objects.equals(user.getFaction().getId(), userJoinRequest.getFactionId()));

            Preconditions.checkArgument(userJoinRequest.getToken() != null);
            Verify.verifyNotNull(user.getToken());

            // TODO: in addition to token - we should check validity of player login (e.g. are they meant to even be in the area?)
            if (!Objects.equals(user.getToken().toString(), userJoinRequest.getToken())) {
                log.warn("Token {} mismatch {} for user {}", userJoinRequest.getToken(), user.getToken(), user);
                throw new IllegalArgumentException("Token mismatch");
            }

        } else {
            user = autoRegister(userJoinRequest);
        }

        user.setServer(server);
        user.setLastJoin(LocalDateTime.now());
        user.setSeen(user.getLastJoin());

        log.atLevel(user.isHuman() ? Level.INFO : Level.DEBUG)
                .log("User joined: userId={} userName={} userHuman={} factionId={} serverId={}",
                        user.getId(), user.getName(), user.isHuman(), user.getFaction().getIndex(), server.getId());

        return userService.toDetailDto(user);
    }

    private User autoRegister(UserJoinRequest userJoinRequest) {
        User user = null;

        Verify.verifyNotNull(userJoinRequest.getHuman());
        if (!userJoinRequest.getHuman()) {
            // bots should try to use existing bot account
            // TODO: avoid using seen (should use serverId)
            user = userRepository.findFirstByHumanFalseAndFactionIdAndSeenBefore(
                    userJoinRequest.getFactionId(), LocalDateTime.now().minusMinutes(1)).orElse(null);
        }

        if (user == null) {
            RegisterUserRequest registerUserRequest = RegisterUserRequest.builder()
                    .factionId(userJoinRequest.getFactionId())
                    .human(userJoinRequest.getHuman())
                    .serverId(userJoinRequest.getServerId())
                    .build();

            UserDetailDto userDetailDto = userService.registerUser(registerUserRequest);

            user = userRepository.getReferenceById(userDetailDto.getId());
        }

        return user;
    }

    @Retryable(retryFor = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 100, maxDelay = 1000))
    public ResponseEntity<UserNavigateResponse> serverUserNavigate(UserNavigateRequest userNavigateRequest) {
        User user = userRepository.getReferenceById(userNavigateRequest.getUserId());
        Server sourceServer = serverRepository.getReferenceById(userNavigateRequest.getSourceServerId());
        Area destinationArea = areaRepository.getReferenceById(userNavigateRequest.getDestinationAreaId());


        Preconditions.checkArgument(user.getServer() == sourceServer);

        Server destinationServer = destinationArea.getServer();
        if (destinationServer == null) {
            log.warn("User {} tried to navigate to area {} which does not have a server!", user.getId(), destinationArea.getId());
            return ResponseEntity.unprocessableEntity().build();
        }

        if (destinationServer.isEnabled()) {
            log.warn("User {} tried to navigate to server {} is not enabled!", user.getId(), destinationServer.getId());
            return ResponseEntity.unprocessableEntity().build();
        }

        if (destinationServer.isActive()) {
            log.warn("User {} tried to navigate to server {} is not active!", user.getId(), destinationServer.getId());
            return ResponseEntity.unprocessableEntity().build();
        }

        user.setX(userNavigateRequest.getX());
        user.setY(userNavigateRequest.getY());
        user.setZ(userNavigateRequest.getZ());

        user.setYaw(userNavigateRequest.getYaw());
        user.setPitch(userNavigateRequest.getPitch());

        // NOTE: when user joins new server this will be updated
        //user.setServer(destinationServer);

        return ResponseEntity.ok(new UserNavigateResponse(destinationServer.getId(),
                //adhocProperties.getServerDomain(),
                destinationServer.getPublicIp(),
                destinationServer.getPublicWebSocketPort(),
                destinationServer.getWebSocketUrl()));
    }

    @Retryable(retryFor = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 100, maxDelay = 1000))
    public UserDefeatedUserEvent handleUserDefeatedUser(ServerUserDefeatedUserEvent serverUserDefeatedUserEvent) {
        User user = userRepository.getReferenceById(serverUserDefeatedUserEvent.getUserId());
        User defeatedUser = userRepository.getReferenceById(serverUserDefeatedUserEvent.getDefeatedUserId());

        BigDecimal scoreAdd = BigDecimal.valueOf(user.isHuman() ? 1.0f : 0.1f);
        userRepository.updateScoreAddById(scoreAdd, user.getId());

        // TODO
        return new UserDefeatedUserEvent(
                user.getId(), user.getVersion() + 1, user.getName(), user.isHuman(),
                defeatedUser.getId(), defeatedUser.getVersion(), defeatedUser.getName(), defeatedUser.isHuman());
    }

    @Retryable(retryFor = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 100, maxDelay = 1000))
    public void decayUserScores() {
        log.trace("Decaying user scores...");

        // TODO: multiplier property
        userRepository.updateScoreMultiply(BigDecimal.valueOf(0.999));
    }

    @Retryable(retryFor = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 100, maxDelay = 1000))
    public void leaveUnseenUsers() {
        log.trace("Leaving unseen users...");
        LocalDateTime seenBefore = LocalDateTime.now().minusMinutes(1);

        try (Stream<User> users = userRepository.streamByServerNotNullAndSeenBefore(seenBefore)) {
            users.forEach(unseenUser -> {
                log.atLevel(unseenUser.isHuman() ? Level.INFO : Level.DEBUG)
                        .log("Leaving unseen user: id={} name={} password?={} human={} factionIndex={} serverId={}",
                                unseenUser.getId(),
                                unseenUser.getName(),
                                unseenUser.getPassword() != null,
                                unseenUser.isHuman(),
                                unseenUser.getFaction().getIndex(),
                                unseenUser.getServer().getId());

                // TODO: common path?
                unseenUser.setServer(null);
            });
        }
    }

    @Retryable(retryFor = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 100, maxDelay = 1000))
    public void purgeOldUsers() {
        log.trace("Purging old users...");

        Set<Long> oldUserIds = new TreeSet<>();

        // regular cleanup of anon users who had a temp account created but never were seen in a server
        oldUserIds.addAll(userRepository.findIdsByCreatedBeforeAndSeenIsNullAndPasswordIsNullAndPawnsIsEmpty(LocalDateTime.now().minusHours(6)));
        // regular cleanup of anon users who were last seen in a server a long time ago
        oldUserIds.addAll(userRepository.findIdsBySeenBeforeAndPasswordIsNullAndPawnsIsEmpty(LocalDateTime.now().minusDays(7)));

        if (!oldUserIds.isEmpty()) {
            log.debug("Purging old users: {}", oldUserIds);
        }

        userRepository.deleteAllByIdInBatch(oldUserIds);
    }
}
