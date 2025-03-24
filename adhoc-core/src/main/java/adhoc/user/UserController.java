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
import adhoc.user.navigate.UserNavigateRequest;
import adhoc.user.navigate.UserNavigateResponse;
import adhoc.user.navigate.UserNavigateService;
import adhoc.user.register.UserRegisterRequest;
import adhoc.user.register.UserRegisterService;
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
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRegisterService userRegisterService;
    private final UserNavigateService userNavigateService;

    @GetMapping("/users")
    public Page<UserDto> getUsers(
            @SortDefault(sort = "score", direction = Sort.Direction.DESC) Pageable pageable) {

        return userService.getUsers(pageable);
    }

    @GetMapping("/users/{userId}")
    public UserDto getUser(
            @PathVariable("userId") Long userId) {

        return userService.getUser(userId);
    }

    @GetMapping("/users/current")
    public ResponseEntity<UserFullDto> getCurrentUser(
            Authentication authentication) {

        if (authentication == null || !(authentication.getPrincipal() instanceof AdhocUserDetails userDetails)) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(userService.getUserFull(userDetails.getUserId()));
    }

    @PostMapping("/users/register")
    public ResponseEntity<UserFullDto> postUserRegister(
            @Valid @RequestBody UserRegisterRequest userRegisterRequest) {

        UserFullDto response = userRegisterService.userRegister(userRegisterRequest);

        return ResponseEntity.created(URI.create("/api/users/current")).body(response);
    }

    @PostMapping("/users/current/navigate")
    public ResponseEntity<UserNavigateResponse> postCurrentUserNavigate(
            @Valid @RequestBody UserNavigateRequest userNavigateRequest,
            Authentication authentication) {

        Preconditions.checkState(authentication != null);
        Preconditions.checkState(authentication.getPrincipal() instanceof AdhocUserDetails);

        AdhocUserDetails adhocUserDetails = (AdhocUserDetails) authentication.getPrincipal();

        userNavigateRequest = userNavigateRequest.toBuilder().userId(adhocUserDetails.getUserId()).build();

        return ResponseEntity.ok(userNavigateService.userNavigate(userNavigateRequest));
    }
}
