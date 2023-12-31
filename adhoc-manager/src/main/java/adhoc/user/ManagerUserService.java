/*
 * Copyright (c) 2022-2023 SpeculativeCoder (https://github.com/SpeculativeCoder)
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
import adhoc.faction.FactionRepository;
import adhoc.server.Server;
import adhoc.server.ServerRepository;
import adhoc.user.event.ServerUserDefeatedUserEvent;
import adhoc.user.event.UserDefeatedBotEvent;
import adhoc.user.event.UserDefeatedUserEvent;
import adhoc.user.request.RegisterUserRequest;
import adhoc.user.request.UserJoinRequest;
import adhoc.user.request.UserNavigateRequest;
import adhoc.user.response.UserNavigateResponse;
import com.google.common.base.Verify;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

@Transactional
@Service
@Slf4j
@RequiredArgsConstructor
public class ManagerUserService {

    private final UserRepository userRepository;
    private final FactionRepository factionRepository;
    private final ServerRepository serverRepository;
    private final AreaRepository areaRepository;

    private final UserService userService;

    public UserDto updateUser(UserDto userDto) {
        return userService.toDto(
                toEntity(userDto, userRepository.getReferenceById(userDto.getId())));
    }

    public ResponseEntity<UserDetailDto> serverUserJoin(UserJoinRequest userJoinRequest, Authentication authentication,
                                                        HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        log.debug("userJoin: human={} userId={} serverId={} factionId={}",
                userJoinRequest.getHuman(), userJoinRequest.getUserId(), userJoinRequest.getServerId(), userJoinRequest.getFactionId());

        Server server = serverRepository.getReferenceById(userJoinRequest.getServerId());

        User user;

        // existing user? verify token
        if (userJoinRequest.getUserId() != null) {
            user = userRepository.getForUpdateById(userJoinRequest.getUserId());
            Verify.verify(Objects.equals(user.getFaction().getId(), userJoinRequest.getFactionId()));

            Verify.verifyNotNull(userJoinRequest.getToken());
            Verify.verifyNotNull(user.getToken());

            // TODO: in addition to token - we should check validity of player login (e.g. are they meant to even be in the area?)
            if (!Objects.equals(user.getToken().toString(), userJoinRequest.getToken())) {
                log.warn("Token {} mismatch {} for user {}", userJoinRequest.getToken(), user.getToken(), user);
                return ResponseEntity.unprocessableEntity().build();
            }

        } else {
            user = autoRegister(userJoinRequest, authentication, httpServletRequest, httpServletResponse);
        }

        user.setServer(server);
        user.setLastJoin(LocalDateTime.now());
        user.setSeen(user.getLastJoin());

        log.info("userJoin: user={} faction={} server={}", user, user.getFaction(), server);

        return ResponseEntity.ok(userService.toDetailDto(user));
    }

    private User autoRegister(UserJoinRequest userJoinRequest, Authentication authentication, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        User user = null;

        Verify.verifyNotNull(userJoinRequest.getHuman());
        if (!userJoinRequest.getHuman()) {
            Faction faction = factionRepository.getReferenceById(userJoinRequest.getFactionId());
            // bots should try to use existing bot account
            // TODO: avoid using seen (should use serverId)
            user = userRepository.findForUpdateByHumanFalseAndFactionAndSeenBefore(
                    faction, LocalDateTime.now().minusMinutes(1)).orElse(null);
        }

        if (user == null) {
            RegisterUserRequest registerUserRequest = RegisterUserRequest.builder()
                    .factionId(userJoinRequest.getFactionId())
                    .human(userJoinRequest.getHuman())
                    .serverId(userJoinRequest.getServerId())
                    .build();

            // TODO: avoid web stuff
            ResponseEntity<UserDetailDto> registeredUserDetail =
                    userService.registerUser(registerUserRequest, authentication, httpServletRequest, httpServletResponse);

            Verify.verify(registeredUserDetail.getStatusCode().is2xxSuccessful());
            UserDetailDto userDetailDto = Verify.verifyNotNull(registeredUserDetail.getBody());

            user = userRepository.getForUpdateById(userDetailDto.getId());
        }

        return user;
    }

    public ResponseEntity<UserNavigateResponse> serverUserNavigate(UserNavigateRequest userNavigateRequest) {
        User user = userRepository.getForUpdateById(userNavigateRequest.getUserId());
        Server sourceServer = serverRepository.getReferenceById(userNavigateRequest.getSourceServerId());
        Area destinationArea = areaRepository.getReferenceById(userNavigateRequest.getDestinationAreaId());

        Verify.verify(user.getServer() == sourceServer);

        Server destinationServer = destinationArea.getServer();
        if (destinationServer == null) {
            log.warn("User {} tried to navigate to area {} which does not have a server!", user.getId(), destinationArea.getId());
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

    public UserDefeatedUserEvent handleUserDefeatedUser(ServerUserDefeatedUserEvent serverUserDefeatedUserEvent) {
        User user = userRepository.getForUpdateById(serverUserDefeatedUserEvent.getUserId());
        user.setScore(user.getScore() + 1);

        User defeatedUser = userRepository.getReferenceById(serverUserDefeatedUserEvent.getDefeatedUserId());
        return new UserDefeatedUserEvent(
                user.getId(), user.getVersion(), user.getName(), user.getHuman(),
                defeatedUser.getId(), defeatedUser.getVersion(), defeatedUser.getName(), defeatedUser.getHuman());
    }

    // TODO
    public void handleUserDefeatedBot(UserDefeatedBotEvent userDefeatedBotEvent) {
        User user = userRepository.getForUpdateById(userDefeatedBotEvent.getUserId());
        user.setScore(user.getScore() + 1);
    }

    public void decayUserScores() {
        log.trace("Decaying user scores...");

        // TODO: multiplier property
        userRepository.updateScoreMultiply(0.999F);
    }

    public void purgeOldUsers() {
        log.trace("Purging old users...");

        Set<Long> oldUserIds = new TreeSet<>();

        // regular cleanup of anon users who had a temp account created but never were seen in a server
        oldUserIds.addAll(userRepository.findIdsByCreatedBeforeAndSeenIsNullAndPasswordIsNullAndPawnsIsEmpty(LocalDateTime.now().minusHours(6)));

        // regular cleanup of anon users who were last seen in a server a long time ago
        oldUserIds.addAll(userRepository.findIdsBySeenBeforeAndPasswordIsNullAndPawnsIsEmpty(LocalDateTime.now().minusDays(7)));

        if (!oldUserIds.isEmpty()) {
            log.info("Purging old users: {}", oldUserIds);
        }

        userRepository.deleteAllByIdInBatch(oldUserIds);
    }

    private User toEntity(UserDto userDto, User user) {
        // TODO
        //user.setName(userDto.getName());
        //user.setFaction(user.getFaction());

        user.setUpdated(LocalDateTime.now());

        return user;
    }

}
