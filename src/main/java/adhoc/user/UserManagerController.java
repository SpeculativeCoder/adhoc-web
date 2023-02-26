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

import adhoc.user.dto.*;
import adhoc.user.event.UserDefeatedBotEvent;
import adhoc.user.event.UserDefeatedUserEvent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Profile("mode-manager")
@Slf4j
@RequiredArgsConstructor
public class UserManagerController {

    private final UserManagerService userManagerService;

    @PutMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto putUser(@PathVariable("userId") Long userId, @Valid @RequestBody UserDto userDto) {
        userDto.setId(userId);

        return userManagerService.updateUser(userDto);
    }

    /**
     * When a user connects to a server but has not yet registered, the server will auto register them.
     */
    @PostMapping("/servers/{serverId}/users/register")
    @PreAuthorize("hasRole('SERVER')")
    public ResponseEntity<UserDetailDto> postServerUserRegister(
            @PathVariable("serverId") Long serverId, @Valid @RequestBody UserRegisterRequest userRegisterRequest, Authentication authentication) {
        userRegisterRequest.setServerId(serverId);

        return userManagerService.serverUserRegister(userRegisterRequest, authentication);
    }

    @PostMapping("/servers/{serverId}/users/{userId}/navigate")
    @PreAuthorize("hasRole('SERVER')")
    public ResponseEntity<UserNavigateResponse> postNavigate(
            @PathVariable("serverId") Long serverId, @PathVariable("userId") Long userId, @Valid @RequestBody UserNavigateRequest userNavigateRequest) {
        userNavigateRequest.setUserId(userId);

        return userManagerService.navigate(userNavigateRequest);
    }

    @PostMapping("/servers/{serverId}/users/{userId}/join")
    @PreAuthorize("hasRole('SERVER')")
    public ResponseEntity<UserDetailDto> postUserJoin(
            @PathVariable("serverId") Long serverId, @PathVariable("userId") Long userId, @Valid @RequestBody UserJoinRequest userJoinRequest) {
        userJoinRequest.setServerId(serverId);
        userJoinRequest.setUserId(userId);

        return userManagerService.userJoin(userJoinRequest);
    }

    @MessageMapping("UserDefeatedUser")
    @SendTo("/topic/events")
    @PreAuthorize("hasRole('SERVER') or hasRole('ADMIN')")
    public UserDefeatedUserEvent handleDefeatedUser(@Valid @RequestBody UserDefeatedUserEvent userDefeatedUserEvent) {
        log.info("Handling: {}", userDefeatedUserEvent);

        return userManagerService.processUserDefeatedUser(userDefeatedUserEvent);
    }

    @MessageMapping("UserDefeatedBot")
    @SendTo("/topic/events")
    @PreAuthorize("hasRole('SERVER') or hasRole('ADMIN')")
    public UserDefeatedBotEvent handleDefeatedBot(@Valid @RequestBody UserDefeatedBotEvent userDefeatedBotEvent) {
        log.info("Handling: {}", userDefeatedBotEvent);

        return userManagerService.processUserDefeatedBot(userDefeatedBotEvent);
    }
}
