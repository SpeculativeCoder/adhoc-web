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
import adhoc.faction.Faction;
import adhoc.objective.ObjectiveRepository;
import adhoc.pawn.PawnRepository;
import adhoc.server.Server;
import adhoc.server.ServerRepository;
import adhoc.user.event.ServerUserDefeatedUserEvent;
import adhoc.user.event.UserDefeatedUserEvent;
import adhoc.user.request_response.ServerUserJoinRequest;
import adhoc.user.request_response.ServerUserNavigateRequest;
import adhoc.user.request_response.ServerUserNavigateResponse;
import adhoc.user.request_response.UserRegisterRequest;
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
import java.util.List;
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
    private final ObjectiveRepository objectiveRepository;
    private final PawnRepository pawnRepository;

    private final UserService userService;
    private final UserRegistrationService userRegistrationService;

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

            UserDetailDto userDetailDto = userRegistrationService.registerUser(userRegisterRequest);

            user = userRepository.getReferenceById(userDetailDto.getId());
        }

        return user;
    }

    @Retryable(retryFor = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 100, maxDelay = 1000))
    public ResponseEntity<ServerUserNavigateResponse> serverUserNavigate(ServerUserNavigateRequest serverUserNavigateRequest) {
        User user = userRepository.getReferenceById(serverUserNavigateRequest.getUserId());
        Server sourceServer = serverRepository.getReferenceById(serverUserNavigateRequest.getSourceServerId());
        Area destinationArea = areaRepository.getReferenceById(serverUserNavigateRequest.getDestinationAreaId());

        Preconditions.checkArgument(user.getServer() == sourceServer);

        Server destinationServer = destinationArea.getServer();
        if (destinationServer == null) {
            log.warn("User {} tried to navigate to area {} which does not have a server!", user.getId(), destinationArea.getId());
            return ResponseEntity.unprocessableEntity().build();
        }

        if (!destinationServer.isEnabled()) {
            log.warn("User {} tried to navigate to server {} which is not enabled!", user.getId(), destinationServer.getId());
            return ResponseEntity.unprocessableEntity().build();
        }

        if (!destinationServer.isActive()) {
            log.warn("User {} tried to navigate to server {} which is not active!", user.getId(), destinationServer.getId());
            return ResponseEntity.unprocessableEntity().build();
        }

        user.setX(serverUserNavigateRequest.getX());
        user.setY(serverUserNavigateRequest.getY());
        user.setZ(serverUserNavigateRequest.getZ());

        user.setYaw(serverUserNavigateRequest.getYaw());
        user.setPitch(serverUserNavigateRequest.getPitch());

        // NOTE: when user joins new server this will be updated
        //user.setServer(destinationServer);

        return ResponseEntity.ok(new ServerUserNavigateResponse(destinationServer.getId(),
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
    public void manageUserScores() {
        log.trace("Managing user scores...");

        List<ObjectiveRepository.FactionObjectiveCount> factionObjectiveCounts =
                objectiveRepository.getFactionObjectiveCounts();

        LocalDateTime seenAfter = LocalDateTime.now().minusHours(48);

        // TODO: move to user
        for (ObjectiveRepository.FactionObjectiveCount factionObjectiveCount : factionObjectiveCounts) {
            Faction faction = factionObjectiveCount.getFaction();
            Integer objectiveCount = factionObjectiveCount.getObjectiveCount();

            BigDecimal humanScoreAdd = BigDecimal.valueOf(0.01).multiply(BigDecimal.valueOf(objectiveCount));
            BigDecimal notHumanScoreAdd = BigDecimal.valueOf(0.001).multiply(BigDecimal.valueOf(objectiveCount));

            userRepository.updateScoreAddByFactionIdAndSeenAfter(humanScoreAdd, notHumanScoreAdd, faction.getId(), seenAfter);
        }

        // TODO: multiplier property
        userRepository.updateScoreMultiply(BigDecimal.valueOf(0.999));
    }

    @Retryable(retryFor = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 100, maxDelay = 1000))
    public void manageUserLocations() {
        log.trace("Managing user locations...");

        LocalDateTime now = LocalDateTime.now();

        try (Stream<User> users = userRepository.streamByServerNotNull()) {
            users.forEach(user -> {
                // see if there is a pawn for the user
                pawnRepository.findFirstByServerAndUserOrderBySeenDescIdDesc(user.getServer(), user).ifPresent(pawn -> {

                    user.setX(pawn.getX());
                    user.setY(pawn.getY());
                    user.setZ(pawn.getZ());
                    user.setPitch(pawn.getPitch());
                    user.setYaw(pawn.getYaw());

                    user.setSeen(now);

                });
            });
        }
    }

    @Retryable(retryFor = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 100, maxDelay = 1000))
    public void leaveUnseenUsers() {
        log.trace("Leaving unseen users...");
        LocalDateTime seenBefore = LocalDateTime.now().minusMinutes(1);

        try (Stream<User> users = userRepository.streamByServerNotNullAndSeenBefore(seenBefore)) {
            users.forEach(unseenUser -> {
                log.atLevel(unseenUser.isHuman() ? Level.INFO : Level.DEBUG)
                        .log("User left: id={} name={} password?={} human={} factionIndex={} serverId={}",
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
            userRepository.deleteAllByIdInBatch(oldUserIds);
        }
    }
}
