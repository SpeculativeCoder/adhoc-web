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

import adhoc.user.events.ServerUserDefeatEvent;
import adhoc.user.events.UserDefeatEvent;
import adhoc.user.requests.UserJoinRequest;
import adhoc.user.requests.UserNavigateRequest;
import adhoc.user.responses.UserJoinResponse;
import adhoc.user.responses.UserNavigateResponse;
import com.google.common.base.Preconditions;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/adhoc_api")
@Slf4j
@RequiredArgsConstructor
public class UserManagerController {

    private final UserManagerService userManagerService;
    private final UserNavigateService userNavigateService;
    private final UserJoinService userJoinService;
    private final UserDefeatService userDefeatService;

    @PutMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto putUser(
            @PathVariable("userId") Long userId,
            @Valid @RequestBody UserDto userDto) {

        Preconditions.checkArgument(Objects.equals(userId, userDto.getId()),
                "User ID mismatch: %s != %s", userId, userDto.getId());

        return userManagerService.updateUser(userDto);
    }

    @PostMapping("/servers/{serverId}/userNavigate")
    @PreAuthorize("hasRole('SERVER')")
    public ResponseEntity<UserNavigateResponse> postServerUserNavigate(
            @PathVariable("serverId") Long serverId,
            @Valid @RequestBody UserNavigateRequest request) {

        log.debug("serverUserNavigate: request={}", request);

        // for now, server must specify location so user can be properly placed on arrival
        Preconditions.checkArgument(request.getX() != null);
        Preconditions.checkArgument(request.getY() != null);
        Preconditions.checkArgument(request.getZ() != null);
        Preconditions.checkArgument(request.getYaw() != null);
        Preconditions.checkArgument(request.getPitch() != null);

        UserNavigateResponse response = userNavigateService.userNavigate(request);

        log.debug("serverUserNavigate: response={}", response);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/servers/{serverId}/userJoin")
    @PreAuthorize("hasRole('SERVER')")
    public UserJoinResponse postServerUserJoin(
            @PathVariable("serverId") Long serverId,
            @Valid @RequestBody UserJoinRequest userJoinRequest) {

        Preconditions.checkArgument(Objects.equals(serverId, userJoinRequest.getServerId()),
                "Server ID mismatch: %s != %s", serverId, userJoinRequest.getServerId());

        log.debug("postServerUserJoin: userId={} human={} factionId={} serverId={}",
                userJoinRequest.getUserId(), userJoinRequest.getHuman(), userJoinRequest.getFactionId(), userJoinRequest.getServerId());

        return userJoinService.userJoin(userJoinRequest);
    }

    @MessageMapping("ServerUserDefeat")
    @SendTo("/topic/events")
    @PreAuthorize("hasRole('SERVER') or hasRole('ADMIN')")
    public UserDefeatEvent handleUserDefeat(
            @Valid @RequestBody ServerUserDefeatEvent serverUserDefeatEvent) {

        log.debug("Handling: {}", serverUserDefeatEvent);

        UserDefeatEvent userDefeatEvent = userDefeatService.userDefeat(serverUserDefeatEvent);

        log.debug("Sending: {}", userDefeatEvent);

        return userDefeatEvent;
    }
}
