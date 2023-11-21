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

import adhoc.faction.FactionRepository;
import adhoc.server.ServerRepository;
import adhoc.user.request.RegisterUserRequest;
import com.google.common.collect.Sets;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

@Transactional
@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final FactionRepository factionRepository;
    private final ServerRepository serverRepository;
    private final HttpServletRequest httpServletRequest;

    // lazy for now due to cycle on authentication success handler
    @SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
    @Lazy
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserDto> getUsers() {
        return userRepository.findAll(PageRequest.of(0, 100, Sort.Direction.DESC, "score", "id"))
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserDto getUser(Long userId) {
        return toDto(userRepository.getReferenceById(userId));
    }

    @Transactional(readOnly = true)
    public Optional<User> findUser(String username) {
        return userRepository.findByNameOrEmailAndPasswordIsNotNull(username, username);
    }

    @Transactional(readOnly = true)
    public UserDetailDto getUserDetail(Long userId) {
        return toDetailDto(userRepository.getReferenceById(userId));
    }

    public ResponseEntity<UserDetailDto> registerUser(RegisterUserRequest registerUserRequest, Authentication authentication) {
        if (registerUserRequest.getName() == null) {
            registerUserRequest.setName("Anon" + (int) Math.floor(Math.random() * 1000000000)); // TODO
        }

        if (registerUserRequest.getFactionId() == null) {
            registerUserRequest.setFactionId(1 + (long) Math.floor(Math.random() * factionRepository.count()));
        }

        // TODO: think about email check before allowing email input (currently guarded at controller)
        Optional<User> optionalUser =
                registerUserRequest.getEmail() == null
                        ? userRepository.findByName(registerUserRequest.getName())
                        : userRepository.findByNameOrEmail(registerUserRequest.getName(), registerUserRequest.getEmail());
        if (optionalUser.isPresent()) {
            log.warn("Can't register user as name or email already in use: name={} email={}", registerUserRequest.getName(), registerUserRequest.getEmail());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        User user = userRepository.saveAndFlush(toEntity(registerUserRequest));
        log.debug("register: user={} password*={} token={}", user, user.getPassword() == null ? null : "***", user.getToken());

        // if not an auto-register from server - log them in too
        if (authentication == null
                || authentication.getAuthorities().stream().noneMatch(authority -> "ROLE_SERVER".equals(authority.getAuthority()))) {
            //UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            PreAuthenticatedAuthenticationToken authenticationToken = new PreAuthenticatedAuthenticationToken(user, null, user.getAuthorities());
            //SecurityContextHolder.clearContext();
            //SecurityContextHolder.createEmptyContext();
            // TODO
            SecurityContext securityContext = SecurityContextHolder.getContext();
            securityContext.setAuthentication(authenticationToken);
            httpServletRequest.getSession(true).setAttribute(SPRING_SECURITY_CONTEXT_KEY, securityContext);

            // TODO: put in a login listener
            user.setLastLogin(LocalDateTime.now());
        }

        return ResponseEntity.ok(toDetailDto(user));
    }

    public User regenerateUserToken(User user) {
        user = userRepository.getReferenceById(user.getId());

        UUID newToken = UUID.randomUUID();
        user.setToken(newToken);

        return user;
    }

    UserDto toDto(User user) {
        return new UserDto(
                user.getId(),
                user.getVersion(),
                user.getName(),
                user.getFaction().getId(),
                user.getFaction().getIndex(),
                user.getBot(),
                user.getScore(),
                user.getSeen());
    }

    UserDetailDto toDetailDto(User user) {
        return new UserDetailDto(
                user.getId(),
                user.getVersion(),
                user.getName(),
                user.getFaction().getId(),
                user.getFaction().getIndex(),
                user.getBot(),
                user.getScore(),
                user.getX(),
                user.getY(),
                user.getZ(),
                user.getPitch(),
                user.getYaw(),
                user.getCreated(),
                user.getUpdated(),
                user.getLastLogin(),
                user.getLastJoin(),
                user.getSeen(),
                user.getRoles().stream().map(UserRole::name).collect(Collectors.toList()),
                user.getToken().toString(),
                user.getServer() == null ? null : user.getServer().getId());
    }

    User toEntity(RegisterUserRequest registerUserRequest) {
        User user = new User();

        user.setName(registerUserRequest.getName());
        user.setEmail(registerUserRequest.getEmail());
        user.setPassword(registerUserRequest.getPassword() == null ? null : passwordEncoder.encode(registerUserRequest.getPassword()));
        user.setFaction(factionRepository.getReferenceById(registerUserRequest.getFactionId()));
        user.setBot(registerUserRequest.getBot() == null ? false : registerUserRequest.getBot());
        user.setScore(0F);
        user.setRoles(Sets.newHashSet(UserRole.USER));
        user.setToken(UUID.randomUUID());
        user.setServer(registerUserRequest.getServerId() == null ? null : serverRepository.getReferenceById(registerUserRequest.getServerId()));

        return user;
    }
}
