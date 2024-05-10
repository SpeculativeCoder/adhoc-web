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
import adhoc.region.Region;
import adhoc.region.RegionRepository;
import adhoc.server.Server;
import adhoc.server.ServerRepository;
import adhoc.system.authentication.AdhocAuthenticationSuccessHandler;
import adhoc.user.request_response.UserNavigateRequest;
import adhoc.user.request_response.UserRegisterRequest;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.LockAcquisitionException;
import org.slf4j.event.Level;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
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
    private final RegionRepository regionRepository;

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
    public Page<UserDto> getUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toDto);
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

    @Retryable(retryFor = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 100, maxDelay = 1000))
    public UserDetailDto registerUser(UserRegisterRequest userRegisterRequest) {
        String userAgent = determineUserAgent();
        String remoteAddr = determineRemoteAddr();

        log.debug("registerUser: name={} password?={} human={} factionId={} remoteAddr={} userAgent={}",
                userRegisterRequest.getName(),
                userRegisterRequest.getPassword() != null,
                userRegisterRequest.getHuman(),
                userRegisterRequest.getFactionId(),
                remoteAddr,
                userAgent);

        if (!coreProperties.getFeatureFlags().contains("development")) {
            Preconditions.checkArgument(userRegisterRequest.getEmail() == null, "Registering with email not allowed yet");
            Preconditions.checkArgument(userRegisterRequest.getPassword() == null, "Registering with password not allowed yet");
            Preconditions.checkArgument(userRegisterRequest.getName() == null, "Registering with name not allowed yet");
        }

        boolean authenticatedAsServer = isAuthenticatedAsServer();

        Preconditions.checkArgument(userRegisterRequest.getHuman() != null);
        // human can only register user as human, but server may register users as human or bot
        Preconditions.checkArgument(userRegisterRequest.getHuman() || authenticatedAsServer);

        // TODO: think about existing name/email check before allowing name/email input
        Optional<User> existingUser;
        if (userRegisterRequest.getName() != null && userRegisterRequest.getEmail() != null) {
            existingUser = userRepository.findByNameOrEmail(userRegisterRequest.getName(), userRegisterRequest.getEmail());
        } else if (userRegisterRequest.getName() != null) {
            existingUser = userRepository.findByName(userRegisterRequest.getName());
        } else {
            existingUser = Optional.empty();
        }

        if (existingUser.isPresent()) {
            log.warn("User name or email already in use: name={} email={}", userRegisterRequest.getName(), userRegisterRequest.getEmail());
            throw new IllegalArgumentException("User name or email already in use");
        }

        UserRegisterRequest.UserRegisterRequestBuilder builder = userRegisterRequest.toBuilder();

        if (userRegisterRequest.getName() == null) {
            String prefix = userRegisterRequest.getHuman() ? "Anon" : "Bot";
            builder.name(prefix + (int) Math.floor(Math.random() * 1000000000)); // TODO
        }

        if (userRegisterRequest.getFactionId() == null) {
            builder.factionId(1 + (long) Math.floor(Math.random() * factionRepository.count()));
        }

        Region region;
        if (userRegisterRequest.getRegionId() == null) {
            List<Region> regions = regionRepository.findAll();
            // TODO
            region = regions.get(ThreadLocalRandom.current().nextInt(regions.size()));
            builder.regionId(region.getId());
        } else {
            region = regionRepository.getReferenceById(userRegisterRequest.getRegionId());
        }

        if (userRegisterRequest.getServerId() == null) {
            // TODO
            Server server = region.getServers().get(ThreadLocalRandom.current().nextInt(region.getServers().size()));
            builder.serverId(server.getId());
        }

        userRegisterRequest = builder.build();

        User user = userRepository.save(toEntity(userRegisterRequest));

        // if not an auto-register from server - log them in too
        if (!authenticatedAsServer) {
            autoLogin(userRegisterRequest, user);
        }

        log.atLevel(Optional.ofNullable(userRegisterRequest.getHuman()).orElse(false) ? Level.INFO : Level.DEBUG)
                .log("User registered: id={} name={} password?={} human={} factionIndex={} remoteAddr={} userAgent={}",
                        user.getId(),
                        user.getName(),
                        user.getPassword() != null,
                        user.isHuman(),
                        user.getFaction().getIndex(),
                        remoteAddr,
                        userAgent);

        return toDetailDto(user);
    }

    private String determineRemoteAddr() {
        return httpServletRequest.getRemoteAddr();
    }

    private String determineUserAgent() {
        return httpServletRequest.getHeader("user-agent").replaceAll("[^A-Za-z0-9 _()/;:,.+\\-]", "?");
    }

    private static boolean isAuthenticatedAsServer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.getAuthorities().stream().anyMatch(authority -> "ROLE_SERVER".equals(authority.getAuthority()));
    }

    private void autoLogin(UserRegisterRequest userRegisterRequest, User user) {
        String tempPassword = null;
        if (user.getPassword() == null) {
            tempPassword = UUID.randomUUID().toString();
        }

        UsernamePasswordAuthenticationToken authenticationToken =
                UsernamePasswordAuthenticationToken.unauthenticated(
                        userRegisterRequest.getName(),
                        tempPassword != null ? tempPassword : userRegisterRequest.getPassword());
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

    /**
     * Sets a new "token" every time a user logs in.
     * The "token" is used when logging into an Unreal server to make sure the user is who they say they are.
     */
    @Retryable(retryFor = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 100, maxDelay = 1000))
    public void onAuthenticationSuccess(Long userId) {
        User user = userRepository.getReferenceById(userId);

        UUID newToken = UUID.randomUUID();
        user.setToken(newToken);
        user.setLastLogin(LocalDateTime.now());

        log.debug("onAuthenticationSuccess: id={} name={} human={} token={}", user.getId(), user.getName(), user.isHuman(), user.getToken());
    }

    UserDto toDto(User user) {
        return new UserDto(
                user.getId(),
                user.getVersion(),
                user.getName(),
                user.isHuman(),
                user.getFaction().getId(),
                user.getScore(),
                user.getRegion() == null ? null : user.getRegion().getId(),
                user.getSeen(),
                user.getServer() == null ? null : user.getServer().getId());
    }

    UserDetailDto toDetailDto(User user) {
        return new UserDetailDto(
                user.getId(),
                user.getVersion(),
                user.getName(),
                user.isHuman(),
                user.getFaction().getId(),
                user.getScore(),
                user.getRegion().getId(),
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

    User toEntity(UserRegisterRequest userRegisterRequest) {
        User user = new User();

        user.setName(userRegisterRequest.getName());
        user.setEmail(userRegisterRequest.getEmail());
        user.setPassword(userRegisterRequest.getPassword() == null ? null : passwordEncoder.encode(userRegisterRequest.getPassword()));
        user.setHuman(userRegisterRequest.getHuman());
        user.setFaction(factionRepository.getReferenceById(userRegisterRequest.getFactionId()));
        user.setScore(BigDecimal.valueOf(0.0));
        user.setRegion(regionRepository.getReferenceById(userRegisterRequest.getRegionId()));
        user.setRoles(Sets.newHashSet(UserRole.USER));
        user.setToken(UUID.randomUUID());
        user.setServer(serverRepository.getReferenceById(userRegisterRequest.getServerId()));

        return user;
    }

    public UserDetailDto navigateUser(Long userId, UserNavigateRequest userNavigateRequest) {
        Preconditions.checkArgument(userNavigateRequest.getRegionId() != null,
                "User navigation must specify a region");

        User user = userRepository.getReferenceById(userId);

        if (userNavigateRequest.getServerId() != null) {
            Server server = serverRepository.getReferenceById(userNavigateRequest.getServerId());
            user.setRegion(server.getRegion());
            user.setServer(server);
        } else {
            Region region = regionRepository.getReferenceById(userNavigateRequest.getRegionId());
            user.setRegion(region);
            // TODO
            user.setServer(region.getServers().get(ThreadLocalRandom.current().nextInt(region.getServers().size())));
        }

        // only internal navigation from unreal server to unreal server will preserve user location,
        // manual user navigation will force a new spawn
        user.setX(null);
        user.setY(null);
        user.setZ(null);
        user.setPitch(null);
        user.setYaw(null);

        return toDetailDto(user);
    }
}
