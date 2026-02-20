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

package adhoc.system.auth;

import adhoc.shared.properties.CoreProperties;
import adhoc.shared.random_uuid.RandomUUIDUtils;
import adhoc.user.UserEntity;
import adhoc.user.UserRepository;
import adhoc.user.UserRole;
import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
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
public class AdhocUserDetailsService implements UserDetailsService {

    private final CoreProperties coreProperties;

    private final PasswordEncoder passwordEncoder;

    private final UserRepository userRepository;

    @NonNull
    @Transactional(readOnly = true)
    @Override
    public UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {

        // TODO: move to server package?
        if (!Strings.isNullOrEmpty(coreProperties.getServerBasicAuthUsername())
                && !Strings.isNullOrEmpty(coreProperties.getServerBasicAuthPassword())
                && coreProperties.getServerBasicAuthUsername().equals(username)) {

            return new AdhocUserDetails(
                    coreProperties.getServerBasicAuthUsername(),
                    passwordEncoder.encode(coreProperties.getServerBasicAuthPassword()),
                    true, true, true, true,
                    Collections.singleton(new SimpleGrantedAuthority("ROLE_" + UserRole.SERVER.name())),
                    null);
        }

        log.atTrace()
                .addKeyValue("username", username)
                .log("loadUserByUsername:");

        boolean quickLogin;
        String nameOrEmail;

        if (username.endsWith("-")) {
            nameOrEmail = username.substring(0, username.length() - 1);
            quickLogin = true;
        } else {
            nameOrEmail = username;
            quickLogin = false;
        }

        UserEntity user = userRepository.findByNameOrEmail(nameOrEmail, nameOrEmail).orElseThrow(() ->
                UsernameNotFoundException.fromUsername(username));

        log.atTrace()
                .addKeyValue("user", user)
                //.addKeyValue("user.token", user.getState().getToken())
                .log("loadUserByUsername:");

        Collection<GrantedAuthority> authorities = new LinkedHashSet<>(user.getAuthorities());
        //if (coreProperties.getFeatureFlags().contains("development")) {
        //    authorities.add(new SimpleGrantedAuthority("ROLE_" + UserRole.DEBUG.name()));
        //}

        String password;
        boolean enabled;

        if (!quickLogin && user.getPassword() != null) {
            password = user.getPassword();
            enabled = true;

        } else if (quickLogin && user.getQuickLoginPassword() != null) {
            String quickLoginPassword = user.getQuickLoginPassword(coreProperties.getQuickLoginPasswordEncryptionKey());
            password = passwordEncoder.encode(quickLoginPassword);
            enabled = true;

        } else {
            // else account is not enabled
            password = RandomUUIDUtils.randomUUID().toString(); // TODO
            enabled = false;
        }

        AdhocUserDetails userDetails = new AdhocUserDetails(
                user.getName(),
                password,
                enabled,
                true,
                true,
                true,
                authorities,
                user.getId());

        log.atTrace()
                .addKeyValue("userDetails", userDetails)
                .log("loadUserByUsername:");

        return userDetails;
    }
}
