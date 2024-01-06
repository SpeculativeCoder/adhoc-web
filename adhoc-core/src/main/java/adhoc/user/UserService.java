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

import adhoc.faction.FactionRepository;
import adhoc.properties.CoreProperties;
import adhoc.server.ServerRepository;
import adhoc.user.request.RegisterUserRequest;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Transactional
@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    
    private final CoreProperties coreProperties;

    private final UserRepository userRepository;
    private final FactionRepository factionRepository;
    private final ServerRepository serverRepository;

    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final RememberMeServices rememberMeServices;
    private final AdhocAuthenticationSuccessHandler adhocAuthenticationSuccessHandler;

    private final HttpServletRequest httpServletRequest;
    private final HttpServletResponse httpServletResponse;

    private final WebAuthenticationDetailsSource authenticationDetailsSource = new WebAuthenticationDetailsSource();
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();

    @Transactional(readOnly = true)
    public List<UserDto> getUsers() {
        return userRepository.findAll(PageRequest.of(0, 100, Sort.Direction.DESC, "score"))
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserDto getUser(Long userId) {
        return toDto(userRepository.getReferenceById(userId));
    }

    @Transactional(readOnly = true)
    public Optional<User> findUserByNameOrEmail(String usernameOrEmail) {
        return userRepository.findByNameOrEmail(usernameOrEmail, usernameOrEmail);
    }

    @Transactional(readOnly = true)
    public UserDetailDto getUserDetail(Long userId) {
        return toDetailDto(userRepository.getReferenceById(userId));
    }

    public ResponseEntity<UserDetailDto> registerUser(RegisterUserRequest registerUserRequest) {
        if (!coreProperties.getFeatureFlags().contains("development")) {
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

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean authenticationIsServer = authentication != null
                && authentication.getAuthorities().stream().anyMatch(authority -> "ROLE_SERVER".equals(authority.getAuthority()));

        Preconditions.checkNotNull(registerUserRequest.getHuman());
        // human can only register user as human, but server may register users as human or bot
        Preconditions.checkArgument(registerUserRequest.getHuman() || authenticationIsServer);

        // TODO: think about email check before allowing email input
        Optional<User> optionalUser =
                registerUserRequest.getEmail() == null
                        ? userRepository.findByName(registerUserRequest.getName())
                        : userRepository.findByNameOrEmail(registerUserRequest.getName(), registerUserRequest.getEmail());
        if (optionalUser.isPresent()) {
            log.warn("Can't register user as name or email already in use: name={} email={}", registerUserRequest.getName(), registerUserRequest.getEmail());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        User user = createUser(registerUserRequest);

        // if not an auto-register from server - log them in too
        if (!authenticationIsServer) {
            autoLogin(registerUserRequest, user);
        }

        return ResponseEntity.ok(toDetailDto(user));
    }

    private void autoLogin(RegisterUserRequest registerUserRequest, User user) {
        String tempPassword = null;
        if (user.getPassword() == null) {
            tempPassword = UUID.randomUUID().toString();
        }

        UsernamePasswordAuthenticationToken authenticationToken =
                UsernamePasswordAuthenticationToken.unauthenticated(
                        registerUserRequest.getName(),
                        tempPassword != null ? tempPassword : registerUserRequest.getPassword());
        authenticationToken.setDetails(authenticationDetailsSource.buildDetails(httpServletRequest));

        if (tempPassword != null) {
            user.setPassword(passwordEncoder.encode(tempPassword));
        }

        Authentication authentication = authenticationManager.authenticate(authenticationToken);

        if (tempPassword != null) {
            user.setPassword(null);
        }

        sessionAuthenticationStrategy.onAuthentication(authentication, httpServletRequest, httpServletResponse);

        SecurityContext securityContext = securityContextHolderStrategy.createEmptyContext();
        securityContext.setAuthentication(authentication);
        securityContextHolderStrategy.setContext(securityContext);
        securityContextRepository.saveContext(securityContext, httpServletRequest, httpServletResponse);

        rememberMeServices.loginSuccess(httpServletRequest, httpServletResponse, authentication);

        adhocAuthenticationSuccessHandler.onAuthenticationSuccess(httpServletRequest, httpServletResponse, authentication);
    }

    User createUser(RegisterUserRequest registerUserRequest) {
        if (registerUserRequest.getName() == null) {
            String prefix = registerUserRequest.getHuman() ? "Anon" : "Bot";
            //if (registerUserRequest.getHuman()) {
            registerUserRequest.setName(prefix + (int) Math.floor(Math.random() * 1000000000)); // TODO
            //} else {
            //    registerUserRequest.setName("Bot"); // NOTE: name will be updated below once database id is assigned
            //}
        }

        if (registerUserRequest.getFactionId() == null) {
            registerUserRequest.setFactionId(1 + (long) Math.floor(Math.random() * factionRepository.count()));
        }

        User user = userRepository.save(toEntity(registerUserRequest));

        //if (!user.getHuman() && "Bot".equals(user.getName())) {
        //    user.setName("Bot" + user.getId());
        //}

        log.debug("createUser: user={} password*={} token={}", user, user.getPassword() == null ? null : "***", user.getToken());

        return user;
    }

    /**
     * Sets a new "token" every time a user logs in.
     * The "token" is used when logging into an Unreal server to make sure the user is who they say they are.
     */
    void onAuthenticationSuccess(Long userId) {
        User user = userRepository.getReferenceById(userId);

        UUID newToken = UUID.randomUUID();
        user.setToken(newToken);
        user.setLastLogin(LocalDateTime.now());

        log.debug("onAuthenticationSuccess: id={} name={} human={} token={}", user.getId(), user.getName(), user.getHuman(), user.getToken());
    }

    UserDto toDto(User user) {
        return new UserDto(
                user.getId(),
                user.getVersion(),
                user.getName(),
                user.getHuman(),
                user.getFaction().getId(),
                user.getScore(),
                user.getSeen());
    }

    UserDetailDto toDetailDto(User user) {
        return new UserDetailDto(
                user.getId(),
                user.getVersion(),
                user.getName(),
                user.getHuman(),
                user.getFaction().getId(),
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
        user.setHuman(registerUserRequest.getHuman());
        user.setFaction(factionRepository.getReferenceById(registerUserRequest.getFactionId()));
        user.setScore(0F);
        user.setRoles(Sets.newHashSet(UserRole.USER));
        user.setToken(UUID.randomUUID());
        user.setServer(registerUserRequest.getServerId() == null ? null : serverRepository.getReferenceById(registerUserRequest.getServerId()));

        return user;
    }
}
