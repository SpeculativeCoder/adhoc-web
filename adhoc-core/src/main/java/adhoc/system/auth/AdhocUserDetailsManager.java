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

package adhoc.system.auth;

import adhoc.system.properties.CoreProperties;
import adhoc.system.random_uuid.RandomUUIDUtils;
import adhoc.user.UserEntity;
import adhoc.user.UserRepository;
import adhoc.user.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

/**
 * Consults the {@link UserEntity} table for user info as needed by Spring Security.
 * Also has special support for the "server" user (used by Unreal server when talking to the web server) which is set via properties.
 * TODO: it would be nice to split the "server" user functionality into another user details service.
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class AdhocUserDetailsManager implements UserDetailsManager {

    private final CoreProperties coreProperties;

    private final PasswordEncoder passwordEncoder;

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        if (coreProperties.getServerBasicAuthUsername().isPresent()
                && coreProperties.getServerBasicAuthPassword().isPresent()
                && coreProperties.getServerBasicAuthUsername().get().equals(username)) {

            return new AdhocUserDetails(
                    coreProperties.getServerBasicAuthUsername().get(),
                    passwordEncoder.encode(coreProperties.getServerBasicAuthPassword().get()),
                    true, true, true, true,
                    Collections.singleton(new SimpleGrantedAuthority("ROLE_" + UserRole.SERVER.name())),
                    null);
        }

        log.debug("loadUserByUsername: username={}", username);

        UserEntity user = userRepository.findByNameOrEmail(username, username).orElseThrow(() ->
                new UsernameNotFoundException("Failed to find user with name or email: " + username));

        log.debug("loadUserByUsername: user={} user.token={}", user, user.getState().getToken());

        Collection<GrantedAuthority> authorities = new LinkedHashSet<>(user.getAuthorities());
        //if (coreProperties.getFeatureFlags().contains("development")) {
        //    authorities.add(new SimpleGrantedAuthority("ROLE_" + UserRole.DEBUG.name()));
        //}

        String name = user.getName();

        String password;
        boolean enabled;

        if (user.getPassword() != null) {
            password = user.getPassword();
            enabled = true;

        } else if (user.getLoginCode() != null) {
            // if no password set yet - allow user to use the login code instead
            password = user.getLoginCode();
            enabled = false;
            //enabled = true; // TODO: enable login code logins

        } else {
            // else account is not enabled
            password = RandomUUIDUtils.randomUUID().toString(); // TODO
            enabled = false;
        }

        return new AdhocUserDetails(
                name,
                password,
                enabled,
                true,
                true,
                true,
                authorities,
                user.getId());
    }

    @Override
    public void createUser(UserDetails user) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateUser(UserDetails user) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteUser(String username) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void changePassword(String oldPassword, String newPassword) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean userExists(String username) {
        throw new UnsupportedOperationException();
    }
}
