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

import adhoc.user.event.UserDefeatedBotEvent;
import adhoc.user.event.UserDefeatedUserEvent;
import adhoc.user.request.RegisterUserRequest;
import adhoc.user.request.UserJoinRequest;
import adhoc.user.request.UserNavigateRequest;
import adhoc.user.response.UserNavigateResponse;
import com.google.common.base.Verify;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class ManagerUserController {

    private final ManagerUserService managerUserService;

    @PutMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto putUser(
            @PathVariable("userId") Long userId,
            @Valid @RequestBody UserDto userDto) {

        Verify.verify(Objects.equals(userId, userDto.getId()));

        return managerUserService.updateUser(userDto);
    }

    /**
     * When a user connects to a server but has not yet registered, the server will auto register them.
     */
    @PostMapping("/servers/{serverId}/users/register")
    @PreAuthorize("hasRole('SERVER')")
    public ResponseEntity<UserDetailDto> postServerUserRegister(
            @PathVariable("serverId") Long serverId,
            @Valid @RequestBody RegisterUserRequest registerUserRequest,
            Authentication authentication) {

        Verify.verify(Objects.equals(serverId, registerUserRequest.getServerId()));

        return managerUserService.serverUserRegister(registerUserRequest, authentication);
    }

    @PostMapping("/servers/{serverId}/users/{userId}/navigate")
    @PreAuthorize("hasRole('SERVER')")
    public ResponseEntity<UserNavigateResponse> postServerUserNavigate(
            @PathVariable("serverId") Long serverId,
            @PathVariable("userId") Long userId,
            @Valid @RequestBody UserNavigateRequest userNavigateRequest) {

        Verify.verify(Objects.equals(userId, userNavigateRequest.getUserId()));

        return managerUserService.serverUserNavigate(userNavigateRequest);
    }

    @PostMapping("/servers/{serverId}/users/{userId}/join")
    @PreAuthorize("hasRole('SERVER')")
    public ResponseEntity<UserDetailDto> postServerUserJoin(
            @PathVariable("serverId") Long serverId,
            @PathVariable("userId") Long userId,
            @Valid @RequestBody UserJoinRequest userJoinRequest) {

        Verify.verify(Objects.equals(serverId, userJoinRequest.getServerId()));
        Verify.verify(Objects.equals(userId, userJoinRequest.getUserId()));

        return managerUserService.serverUserJoin(userJoinRequest);
    }

    @MessageMapping("UserDefeatedUser")
    @SendTo("/topic/events")
    @PreAuthorize("hasRole('SERVER') or hasRole('ADMIN')")
    public UserDefeatedUserEvent handleDefeatedUser(
            @Valid @RequestBody UserDefeatedUserEvent userDefeatedUserEvent) {

        log.debug("Handling: {}", userDefeatedUserEvent);

        return managerUserService.handleUserDefeatedUser(userDefeatedUserEvent);
    }

    @MessageMapping("UserDefeatedBot")
    @SendTo("/topic/events")
    @PreAuthorize("hasRole('SERVER') or hasRole('ADMIN')")
    public UserDefeatedBotEvent handleDefeatedBot(
            @Valid @RequestBody UserDefeatedBotEvent userDefeatedBotEvent) {

        log.debug("Handling: {}", userDefeatedBotEvent);

        return managerUserService.handleUserDefeatedBot(userDefeatedBotEvent);
    }
}
