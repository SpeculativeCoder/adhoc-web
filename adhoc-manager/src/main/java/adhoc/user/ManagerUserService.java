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
import adhoc.server.Server;
import adhoc.server.ServerRepository;
import adhoc.user.event.UserDefeatedBotEvent;
import adhoc.user.event.UserDefeatedUserEvent;
import adhoc.user.request.RegisterUserRequest;
import adhoc.user.request.UserJoinRequest;
import adhoc.user.request.UserNavigateRequest;
import adhoc.user.response.UserNavigateResponse;
import com.google.common.base.Verify;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.TreeSet;

@Transactional
@Service
@Slf4j
@RequiredArgsConstructor
public class ManagerUserService {

    private final UserService userService;

    private final UserRepository userRepository;
    private final ServerRepository serverRepository;
    private final AreaRepository areaRepository;

    public UserDto updateUser(UserDto userDto) {
        return userService.toDto(
                toEntity(userDto, userRepository.getReferenceById(userDto.getId())));
    }

    public ResponseEntity<UserNavigateResponse> serverUserNavigate(UserNavigateRequest userNavigateRequest) {
        User user = userRepository.getForUpdateById(userNavigateRequest.getUserId());
        Area area = areaRepository.getReferenceById(userNavigateRequest.getAreaId());
        Server server = area.getServer();
        if (server == null) {
            log.warn("User {} tried to navigate to area {} which does not have a server!", user.getId(), area.getId());
            return ResponseEntity.unprocessableEntity().build();
        }

        user.setX(userNavigateRequest.getX());
        user.setY(userNavigateRequest.getY());
        user.setZ(userNavigateRequest.getZ());

        user.setYaw(userNavigateRequest.getYaw());
        user.setPitch(userNavigateRequest.getPitch());

        user.setServer(server);

        return ResponseEntity.ok(new UserNavigateResponse(server.getId(),
                //adhocProperties.getServerDomain(),
                server.getPublicIp(),
                server.getPublicWebSocketPort(),
                server.getWebSocketUrl()));
    }

    public ResponseEntity<UserDetailDto> serverUserJoin(UserJoinRequest userJoinRequest, Authentication authentication) {
        Server server = serverRepository.getReferenceById(userJoinRequest.getServerId());

        Long userId = userJoinRequest.getUserId();
        String token = userJoinRequest.getToken();

        Verify.verify(userId == null || token != null);

        // if no user id provided, register them
        if (userId == null) {
            RegisterUserRequest registerUserRequest = RegisterUserRequest.builder()
                    .serverId(userJoinRequest.getServerId())
                    .build();
            ResponseEntity<UserDetailDto> registeredUserDetail = userService.registerUser(registerUserRequest, authentication);
            userId = registeredUserDetail.getBody().getId();
            token = registeredUserDetail.getBody().getToken();
        }

        User user = userRepository.getForUpdateById(userId);

        // TODO: in addition to token - we should check validity of player login (e.g. are they meant to even be in the area?)
        if (user.getToken() == null || !user.getToken().toString().contentEquals(token)) {
            log.warn("Token {} mismatch {} for user {}", token, user.getToken(), user);
            return ResponseEntity.unprocessableEntity().build();
        }

        user.setServer(server);
        user.setLastJoin(LocalDateTime.now());

        return ResponseEntity.ok(userService.toDetailDto(user));
    }

    public UserDefeatedUserEvent handleUserDefeatedUser(UserDefeatedUserEvent userDefeatedUserEvent) {
        User user = userRepository.getForUpdateById(userDefeatedUserEvent.getUserId());
        user.setScore(user.getScore() + 1);

        return userDefeatedUserEvent;
    }

    public UserDefeatedBotEvent handleUserDefeatedBot(UserDefeatedBotEvent userDefeatedBotEvent) {
        User user = userRepository.getForUpdateById(userDefeatedBotEvent.getUserId());
        user.setScore(user.getScore() + 1);

        return userDefeatedBotEvent;
    }

    public void decayUserScores() {
        log.trace("Decaying user scores...");

        // TODO: multiplier property
        userRepository.updateUsersMultiplyScore(0.999F);
    }

    public void purgeOldUsers() {
        log.trace("Purging old users...");

        Set<Long> oldUserIds = new TreeSet<>();

        // regular cleanup of anon users who had a temp account created but never were seen in a server
        oldUserIds.addAll(userRepository.findIdsByCreatedBeforeAndSeenIsNullAndPasswordIsNullAndPawnsIsEmpty(LocalDateTime.now().minusHours(6)));

        // regular cleanup of anon users who were last seen in a server a long time ago
        oldUserIds.addAll(userRepository.findIdsBySeenBeforeAndPasswordIsNullAndPawnsIsEmpty(LocalDateTime.now().minusDays(7)));

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
