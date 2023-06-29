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

package adhoc.security.authentication;

import adhoc.user.User;
import adhoc.user.UserRepository;
import adhoc.user.UserRole;
import com.google.common.collect.Sets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Consults the ADHOC_USER table for user info.
 * Also has support for the "server" user (used by Unreal server when talking to the web server) which is set via properties.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AdhocUserDetailsManager implements UserDetailsManager {

    @Value("${adhoc.server.basic-auth.username:#{null}}")
    private Optional<String> serverBasicAuthUsername;
    @Value("${adhoc.server.basic-auth.password:#{null}}")
    private Optional<String> serverBasicAuthPassword;

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (serverBasicAuthUsername.isPresent()
                && serverBasicAuthPassword.isPresent()
                && serverBasicAuthUsername.get().equals(username)) {
            User user = new User();
            user.setName(serverBasicAuthUsername.get());
            user.setPassword(passwordEncoder.encode(serverBasicAuthPassword.get()));
            user.setRoles(Sets.newHashSet(UserRole.SERVER));
            return user;
        }

        log.debug("loadUserByUsername: username={}", username);

        User user = userRepository.findByNameOrEmailAndPasswordIsNotNull(username, username).orElseThrow(() ->
                new UsernameNotFoundException("Failed to enabled user with name or email: " + username));

        log.debug("loadUserByUsername: user={} token={}", user, user.getToken());

        return user;
    }

    @Override
    public void createUser(UserDetails user) {
        // userRepository.save((User) user);
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


    @Transactional(readOnly = true)
    @Override
    public boolean userExists(String username) {
        return userRepository.existsByName(username);
    }
}
