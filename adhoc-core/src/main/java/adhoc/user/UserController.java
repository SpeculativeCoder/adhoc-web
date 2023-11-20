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

import adhoc.user.request.RegisterUserRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Value("${adhoc.feature-flags}")
    private String featureFlags;

    @GetMapping("/users")
    public List<UserDto> getUsers() {

        // TODO: sorting
        return userService.getUsers();
    }

    @GetMapping("/users/{userId}")
    public UserDto getUser(
            @PathVariable("userId") Long userId) {

        return userService.getUser(userId);
    }

    @GetMapping("/users/current")
    public ResponseEntity<UserDetailDto> getCurrentUser() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication.getPrincipal() instanceof User user)) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(userService.getUserDetail(user.getId()));
    }

    @PostMapping("/users/register")
    public ResponseEntity<UserDetailDto> postRegisterUser(
            @Valid @RequestBody RegisterUserRequest registerUserRequest,
            HttpServletRequest httpServletRequest,
            Authentication authentication) {

        if (!featureFlags.contains("development")) {
            if (registerUserRequest.getEmail() != null) {
                throw new UnsupportedOperationException("register email not supported yet");
            }
            if (registerUserRequest.getPassword() != null) {
                throw new UnsupportedOperationException("register password not supported yet");
            }
            if (registerUserRequest.getName() != null) {
                throw new UnsupportedOperationException("register name not supported yet");
            }
        }

        log.info("register: name={} password*={} factionId={} remoteAddr={} userAgent={}",
                registerUserRequest.getName(),
                registerUserRequest.getPassword() == null ? null : "***",
                registerUserRequest.getFactionId(),
                httpServletRequest.getRemoteAddr(),
                httpServletRequest.getHeader("user-agent").replaceAll("[^A-Za-z0-9 _()/;:,.]", "?"));

        return userService.registerUser(registerUserRequest, authentication);
    }
}
