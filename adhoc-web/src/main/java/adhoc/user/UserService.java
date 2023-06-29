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

import com.google.common.collect.Sets;
import adhoc.faction.FactionRepository;
import adhoc.server.ServerRepository;
import adhoc.user.request.UserRegisterRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final PasswordEncoder passwordEncoder;
    private final HttpServletRequest httpServletRequest;

    public List<UserDto> getUsers() {
        return userRepository.findAll(PageRequest.of(0, 100, Sort.Direction.DESC, "score", "id"))
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public UserDto getUser(Long userId) {
        return toDto(userRepository.getReferenceById(userId));
    }

    public UserDetailDto getUserDetail(Long userId) {
        return toDetailDto(userRepository.getReferenceById(userId));
    }

    public ResponseEntity<UserDetailDto> register(UserRegisterRequest userRegisterRequest, Authentication authentication) {
        if (userRegisterRequest.getName() == null) {
            userRegisterRequest.setName("Anon" + (int) Math.floor(Math.random() * 1000000000)); // TODO
        }

        if (userRegisterRequest.getFactionId() == null) {
            userRegisterRequest.setFactionId(1 + (long) Math.floor(Math.random() * factionRepository.count()));
        }

        // TODO: think about email check before allowing email input (currently guarded at controller)
        Optional<User> optionalUser =
                userRegisterRequest.getEmail() == null
                        ? userRepository.findByName(userRegisterRequest.getName())
                        : userRepository.findByNameOrEmail(userRegisterRequest.getName(), userRegisterRequest.getEmail());
        if (optionalUser.isPresent()) {
            log.warn("Can't register user as name or email already in use: name={} email={}", userRegisterRequest.getName(), userRegisterRequest.getEmail());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        User user = userRepository.save(toEntity(userRegisterRequest));
        log.info("register: user={} password={} token={}", user, user.getPassword() == null ? null : "***", user.getToken());

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
        }

        return ResponseEntity.ok(toDetailDto(user));
    }

    UserDto toDto(User user) {
        return new UserDto(
                user.getId(),
                user.getVersion(),
                user.getName(),
                user.getFaction().getId(),
                user.getFaction().getIndex(),
                user.getScore(),
                user.getSeen());
    }

    UserDetailDto toDetailDto(User user) {
        return UserDetailDto.builder()
                .id(user.getId())
                .version(user.getVersion())
                .name(user.getName())
                .factionId(user.getFaction().getId())
                .factionIndex(user.getFaction().getIndex())
                .score(user.getScore())
                .x(user.getX())
                .y(user.getY())
                .z(user.getZ())
                .pitch(user.getPitch())
                .yaw(user.getYaw())
                .created(user.getCreated())
                .updated(user.getUpdated())
                .lastLogin(user.getLastLogin())
                .lastJoin(user.getLastJoin())
                .seen(user.getSeen())
                .roles(user.getRoles().stream().map(UserRole::name).collect(Collectors.toList()))
                .token(user.getToken().toString())
                .serverId(user.getServer() == null ? null : user.getServer().getId())
                .build();
    }

    User toEntity(UserDto userDto) {
        User user = userRepository.getReferenceById(userDto.getId());

        // TODO
        //user.setName(userDto.getName());
        //user.setFaction(user.getFaction());

        user.setUpdated(LocalDateTime.now());

        return user;
    }

    User toEntity(UserRegisterRequest userRegisterRequest) {
        User user = new User();

        user.setName(userRegisterRequest.getName());
        user.setEmail(userRegisterRequest.getEmail());
        user.setPassword(userRegisterRequest.getPassword() == null ? null : passwordEncoder.encode(userRegisterRequest.getPassword()));
        user.setFaction(factionRepository.getReferenceById(userRegisterRequest.getFactionId()));
        user.setScore(0F);
        user.setCreated(LocalDateTime.now());
        user.setUpdated(user.getCreated());
        user.setLastLogin(user.getCreated());
        user.setLastJoin(null);
        user.setRoles(Sets.newHashSet(UserRole.USER));
        user.setToken(UUID.randomUUID());
        user.setServer(userRegisterRequest.getServerId() == null ? null : serverRepository.getReferenceById(userRegisterRequest.getServerId()));

        return user;
    }


}
