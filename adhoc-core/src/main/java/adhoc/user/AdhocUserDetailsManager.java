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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Consults the {@link adhoc.user.User} table for user info.
 * Also has support for the "server" user (used by Unreal server when talking to the web server) which is set via properties.
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class AdhocUserDetailsManager implements UserDetailsManager {

    @Value("${adhoc.server.basic-auth.username:#{null}}")
    private Optional<String> serverBasicAuthUsername;
    @Value("${adhoc.server.basic-auth.password:#{null}}")
    private Optional<String> serverBasicAuthPassword;

    private final PasswordEncoder passwordEncoder;

    //@Setter(onMethod_ = {@Autowired}, onParam_ = {@Lazy})
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        if (serverBasicAuthUsername.isPresent()
                && serverBasicAuthPassword.isPresent()
                && serverBasicAuthUsername.get().equals(username)) {

            return new AdhocUserDetails(
                    serverBasicAuthUsername.get(),
                    passwordEncoder.encode(serverBasicAuthPassword.get()),
                    true, true, true, true,
                    Collections.singleton(new SimpleGrantedAuthority("ROLE_" + UserRole.SERVER.name())),
                    null);
        }

        log.debug("loadUserByUsername: username={}", username);

        User user = userRepository.findByNameOrEmail(username, username).orElseThrow(() ->
                new UsernameNotFoundException("Failed to find user with name or email: " + username));

        log.debug("loadUserByUsername: user={} token={}", user, user.getToken());

        Collection<GrantedAuthority> authorities = new LinkedHashSet<>(user.getAuthorities());
        //if (coreProperties.getFeatureFlags().contains("development")) {
        //    authorities.add(new SimpleGrantedAuthority("ROLE_" + UserRole.DEBUG.name()));
        //}

        AdhocUserDetails userDetails = new AdhocUserDetails(
                user.getName(),
                // TODO
                user.getPassword() == null ? UUID.randomUUID().toString() : user.getPassword(),
                // NOTE: password null means user is not to be logged in to (i.e. temporary users) so we mark as not "enabled"
                user.getPassword() != null, true, true, true,
                authorities,
                user.getId());

        return userDetails;
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
