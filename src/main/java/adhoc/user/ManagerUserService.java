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
import adhoc.faction.FactionRepository;
import adhoc.server.Server;
import adhoc.server.ServerRepository;
import adhoc.user.dto.*;
import adhoc.user.event.UserDefeatedBotEvent;
import adhoc.user.event.UserDefeatedUserEvent;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Transactional
@Service
@Profile("mode-manager")
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
                userRepository.save(userService.toEntity(userDto)));
    }

    public ResponseEntity<UserNavigateResponse> navigate(UserNavigateRequest userNavigateRequest) {
        User user = userRepository.getWithPessimisticWriteLockById(userNavigateRequest.getUserId());
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

    public ResponseEntity<UserDetailDto> userJoin(UserJoinRequest userJoinRequest) {
        User user = userRepository.getWithPessimisticWriteLockById(userJoinRequest.getUserId());
        Server server = serverRepository.getReferenceById(userJoinRequest.getServerId());

        // TODO: in addition to token - we should check validity of player login (e.g. are they meant to even be in the area?)
        if (user.getToken() == null || !user.getToken().toString().contentEquals(userJoinRequest.getToken())) {
            log.warn("Token {} mismatch {} for user {}", userJoinRequest.getToken(), user.getToken(), user);
            return ResponseEntity.unprocessableEntity().build();
        }

        user.setServer(server);
        user.setLastJoin(LocalDateTime.now());

        return ResponseEntity.ok(userService.toDetailDto(user));
    }

    public UserDefeatedUserEvent processUserDefeatedUser(UserDefeatedUserEvent userDefeatedUserEvent) {
        User user = userRepository.getWithPessimisticWriteLockById(userDefeatedUserEvent.getUserId());
        user.setScore(user.getScore() + 1);

//		FactionEntity faction = user.getFaction();
//		faction.setScore(faction.getScore() + 1);

        return userDefeatedUserEvent;
    }

    public UserDefeatedBotEvent processUserDefeatedBot(UserDefeatedBotEvent userDefeatedBotEvent) {
        User user = userRepository.getWithPessimisticWriteLockById(userDefeatedBotEvent.getUserId());
        user.setScore(user.getScore() + 1);

//		FactionEntity faction = user.getFaction();
//		faction.setScore(faction.getScore() + 1);

        return userDefeatedBotEvent;
    }

    public ResponseEntity<UserDetailDto> serverUserRegister(UserRegisterRequest userRegisterRequest, Authentication authentication) {
        return userService.register(userRegisterRequest, authentication);
    }
}
