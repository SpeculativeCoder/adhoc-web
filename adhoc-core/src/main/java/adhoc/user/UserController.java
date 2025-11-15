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

import adhoc.system.auth.AdhocUserDetails;
import adhoc.user.requests.UserNavigateRequest;
import adhoc.user.requests.UserRegisterRequest;
import adhoc.user.responses.UserNavigateResponse;
import com.google.common.base.Preconditions;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/adhoc_api")
@Slf4j
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRegisterService userRegisterService;
    private final UserNavigateService userNavigateService;

    @GetMapping("/users")
    public Page<UserDto> getUsers(
            @SortDefault(sort = "score", direction = Sort.Direction.DESC) Pageable pageable) {

        return userService.findUsers(pageable);
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<UserDto> getUser(
            @PathVariable("userId") Long userId) {

        return ResponseEntity.of(userService.findUser(userId));
    }

    @GetMapping("/users/current")
    public ResponseEntity<UserDto> getCurrentUser(
            Authentication authentication) {

        if (authentication == null || !(authentication.getPrincipal() instanceof AdhocUserDetails userDetails)) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.of(userService.findUser(userDetails.getUserId()));
    }

    @PostMapping("/users/register")
    public ResponseEntity<UserDto> postUserRegister(
            @Valid @RequestBody UserRegisterRequest userRegisterRequest) {

        log.debug("postUserRegister: name={} password?={} factionId={}",
                userRegisterRequest.getName(),
                userRegisterRequest.getPassword() != null,
                userRegisterRequest.getFactionId());

        UserDto response = userRegisterService.userRegisterAndLogin(userRegisterRequest);

        return ResponseEntity.created(URI.create("/adhoc_api/users/current")).body(response);
    }

    @PostMapping("/users/current/navigate")
    public ResponseEntity<UserNavigateResponse> postCurrentUserNavigate(
            @Valid @RequestBody UserNavigateRequest request,
            Authentication authentication) {

        Preconditions.checkArgument(authentication != null);
        Preconditions.checkArgument(authentication.getPrincipal() instanceof AdhocUserDetails);

        AdhocUserDetails adhocUserDetails = (AdhocUserDetails) authentication.getPrincipal();

        request = request.toBuilder().userId(adhocUserDetails.getUserId()).build();

        log.debug("userNavigate: request={}", request);

        // for now, only server navigation may specify location
        Preconditions.checkArgument(request.getX() == null);
        Preconditions.checkArgument(request.getY() == null);
        Preconditions.checkArgument(request.getZ() == null);
        Preconditions.checkArgument(request.getYaw() == null);
        Preconditions.checkArgument(request.getPitch() == null);

        UserNavigateResponse response = userNavigateService.userNavigate(request);

        log.debug("userNavigate: response={}", response);

        return ResponseEntity.ok(response);
    }
}
