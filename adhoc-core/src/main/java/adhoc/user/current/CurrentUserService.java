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

package adhoc.user.current;

import adhoc.system.auth.AdhocUserDetails;
import adhoc.system.properties.CoreProperties;
import adhoc.user.UserEntity;
import adhoc.user.UserRepository;
import adhoc.user.UserRole;
import adhoc.user.state.UserStateEntity;
import com.google.common.base.Verify;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class CurrentUserService {

    private final CoreProperties coreProperties;

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Optional<CurrentUserDto> findCurrentUser(Authentication authentication) {

        if (authentication == null || !(authentication.getPrincipal() instanceof AdhocUserDetails currentUserDetails)) {
            return Optional.empty();
        }

        return userRepository.findById(currentUserDetails.getUserId()).map(this::toCurrentUserDto);
    }

    public CurrentUserDto toCurrentUserDto(UserEntity user) {

        String quickLoginCode = user.getId() + "-" + user.getQuickLoginPassword(coreProperties.getQuickLoginPasswordEncryptionKey());

        UserStateEntity userState = Verify.verifyNotNull(user.getState());

        return new CurrentUserDto(
                user.getId(),
                user.getVersion(),
                user.getName(),
                quickLoginCode,
                user.isHuman(),
                user.getFaction().getId(),
                user.getScore(),
                userState.getRegion() == null ? null : userState.getRegion().getId(),
                user.getUserRoles().stream().map(UserRole::name).collect(Collectors.toList()),
                userState.getDestinationServer() == null ? null : userState.getDestinationServer().getId(),
                userState.getServer() == null ? null : userState.getServer().getId());
    }
}
