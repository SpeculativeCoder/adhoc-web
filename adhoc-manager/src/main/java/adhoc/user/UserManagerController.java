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

import adhoc.user.defeated.ServerUserDefeatedEvent;
import adhoc.user.defeated.UserDefeatedEvent;
import adhoc.user.defeated.UserDefeatedService;
import adhoc.user.join.UserJoinRequest;
import adhoc.user.join.UserJoinService;
import adhoc.user.navigate.UserNavigateRequest;
import adhoc.user.navigate.UserNavigateResponse;
import adhoc.user.navigate.UserNavigateService;
import com.google.common.base.Preconditions;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class UserManagerController {

    private final UserManagerService userManagerService;
    private final UserJoinService userJoinService;
    private final UserNavigateService userNavigateService;
    private final UserDefeatedService userDefeatedService;

    @PutMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto putUser(
            @PathVariable("userId") Long userId,
            @Valid @RequestBody UserDto userDto) {

        Preconditions.checkArgument(Objects.equals(userId, userDto.getId()),
                "User ID mismatch: %s != %s", userId, userDto.getId());

        return userManagerService.updateUser(userDto);
    }

    @PostMapping("/servers/{serverId}/userJoin")
    @PreAuthorize("hasRole('SERVER')")
    public ResponseEntity<UserFullDto> postServerUserJoin(
            @PathVariable("serverId") Long serverId,
            @Valid @RequestBody UserJoinRequest userJoinRequest) {

        Preconditions.checkArgument(Objects.equals(serverId, userJoinRequest.getServerId()),
                "Server ID mismatch: %s != %s", serverId, userJoinRequest.getServerId());

        return ResponseEntity.ok(userJoinService.userJoin(userJoinRequest));
    }

    @PostMapping("/servers/{serverId}/userNavigate")
    @PreAuthorize("hasRole('SERVER')")
    public ResponseEntity<UserNavigateResponse> postServerUserNavigate(
            @PathVariable("serverId") Long serverId,
            @Valid @RequestBody UserNavigateRequest userNavigateRequest) {

        return ResponseEntity.ok(userNavigateService.userNavigate(userNavigateRequest));
    }

    @MessageMapping("ServerUserDefeated")
    @SendTo("/topic/events")
    @PreAuthorize("hasRole('SERVER') or hasRole('ADMIN')")
    public UserDefeatedEvent handleUserDefeated(
            @Valid @RequestBody ServerUserDefeatedEvent serverUserDefeatedEvent) {

        log.debug("Handling: {}", serverUserDefeatedEvent);
        UserDefeatedEvent userDefeatedEvent = userDefeatedService.userDefeated(serverUserDefeatedEvent);

        log.debug("Sending: {}", userDefeatedEvent);
        return userDefeatedEvent;
    }
}
