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

import adhoc.user.event.ServerUserDefeatedUserEvent;
import adhoc.user.event.UserDefeatedBotEvent;
import adhoc.user.event.UserDefeatedUserEvent;
import adhoc.user.request.UserJoinRequest;
import adhoc.user.request.UserNavigateRequest;
import adhoc.user.response.UserNavigateResponse;
import com.google.common.base.Verify;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

    @PostMapping("/servers/{serverId}/userNavigate")
    @PreAuthorize("hasRole('SERVER')")
    public ResponseEntity<UserNavigateResponse> postServerUserNavigate(
            @PathVariable("serverId") Long serverId,
            @Valid @RequestBody UserNavigateRequest userNavigateRequest) {
        Verify.verify(Objects.equals(serverId, userNavigateRequest.getSourceServerId()));

        return managerUserService.serverUserNavigate(userNavigateRequest);
    }

    @PostMapping("/servers/{serverId}/userJoin")
    @PreAuthorize("hasRole('SERVER')")
    public ResponseEntity<UserDetailDto> postServerUserJoin(
            @PathVariable("serverId") Long serverId,
            @Valid @RequestBody UserJoinRequest userJoinRequest,
            Authentication authentication,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) {
        Verify.verify(Objects.equals(serverId, userJoinRequest.getServerId()));

        return managerUserService.serverUserJoin(userJoinRequest, authentication, httpServletRequest, httpServletResponse);
    }

    @MessageMapping("UserDefeatedUser")
    @SendTo("/topic/events")
    @PreAuthorize("hasRole('SERVER') or hasRole('ADMIN')")
    public UserDefeatedUserEvent handleServerUserDefeatedUser(
            @Valid @RequestBody ServerUserDefeatedUserEvent event) {
        log.debug("Handling: {}", event);

        UserDefeatedUserEvent userDefeatedUserEvent = managerUserService.handleUserDefeatedUser(event);

        log.debug("Sending: {}", userDefeatedUserEvent);
        return userDefeatedUserEvent;
    }

    // TODO
    @MessageMapping("UserDefeatedBot")
    @PreAuthorize("hasRole('SERVER') or hasRole('ADMIN')")
    public void handleUserDefeatedBot(
            @Valid @RequestBody UserDefeatedBotEvent event) {
        log.debug("Handling: {}", event);

        managerUserService.handleUserDefeatedBot(event);
    }
}
