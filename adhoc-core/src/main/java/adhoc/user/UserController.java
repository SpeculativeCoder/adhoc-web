/*
 * Copyright (c) 2022-2026 SpeculativeCoder (https://github.com/SpeculativeCoder)
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
import adhoc.user.current.CurrentUserDto;
import adhoc.user.navigate.UserNavigateRequest;
import adhoc.user.navigate.UserNavigateResponse;
import adhoc.user.navigate.UserNavigateService;
import adhoc.user.register.UserRegisterRequest;
import adhoc.user.register.UserRegisterResponse;
import adhoc.user.register.UserRegisterService;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
            @PathVariable Long userId) {

        return ResponseEntity.of(userService.findUser(userId));
    }

    @GetMapping("/users/current")
    public ResponseEntity<CurrentUserDto> getCurrentUser(
            Authentication authentication) {

        return userService.findCurrentUser(authentication)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/users/register")
    public ResponseEntity<UserRegisterResponse> postUserRegister(
            @Valid @RequestBody UserRegisterRequest userRegisterRequest) {

        log.atInfo()
                .addKeyValue("name", userRegisterRequest.getName())
                .addKeyValue("password?", userRegisterRequest.getPassword() != null)
                .addKeyValue("factionId", userRegisterRequest.getFactionId())
                .log("postUserRegister:");

        UserRegisterResponse response = userRegisterService.userRegisterAndLogin(userRegisterRequest);

        return ResponseEntity.created(URI.create("/adhoc_api/users/current")).body(response);
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/users/navigate")
    public ResponseEntity<UserNavigateResponse> postUserNavigate(
            @Valid @RequestBody UserNavigateRequest request,
            Authentication authentication) {

        Verify.verifyNotNull(authentication, "authentication must be set");
        Verify.verify(authentication.getPrincipal() instanceof AdhocUserDetails, "principal must be user details");

        AdhocUserDetails adhocUserDetails = (AdhocUserDetails) authentication.getPrincipal();

        // TODO
        //Preconditions.checkArgument(Objects.equals(request.getUserId(), adhocUserDetails.getUserId()), "request user does not match current user: %s != %s", request.getUserId(), adhocUserDetails.getUserId());
        Preconditions.checkArgument(request.getUserId() == null, "userId must not be set: %s", request.getUserId());

        request = request.toBuilder().userId(adhocUserDetails.getUserId()).build();

        log.atInfo().addKeyValue("request", request).log("userNavigate:");

        // for now, only server navigation may specify location
        Preconditions.checkArgument(request.getX() == null, "x must not be set: %s", request.getX());
        Preconditions.checkArgument(request.getY() == null, "y must not be set: %s", request.getY());
        Preconditions.checkArgument(request.getZ() == null, "z must not be set: %s", request.getZ());
        Preconditions.checkArgument(request.getYaw() == null, "yaw must not be set: %s", request.getYaw());
        Preconditions.checkArgument(request.getPitch() == null, "pitch must not be set: %s", request.getPitch());

        UserNavigateResponse response = userNavigateService.userNavigate(request);

        log.atInfo().addKeyValue("response", request).log("userNavigate:");

        return ResponseEntity.ok(response);
    }
}
