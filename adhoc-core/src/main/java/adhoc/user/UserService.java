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

package adhoc.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<UserDto> getUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public UserDto getUser(Long userId) {
        return toDto(userRepository.getReferenceById(userId));
    }

    @Transactional(readOnly = true)
    public UserFullDto getUserFull(Long userId) {
        return toFullDto(userRepository.getReferenceById(userId));
    }

    UserDto toDto(UserEntity user) {
        return new UserDto(
                user.getId(),
                user.getVersion(),
                user.getName(),
                user.isHuman(),
                user.getFaction().getId(),
                user.getScore(),
                user.getState().getRegion() == null ? null : user.getState().getRegion().getId(),
                user.getState().getSeen(),
                user.getState().getDestinationServer() == null ? null : user.getState().getDestinationServer().getId(),
                user.getState().getServer() == null ? null : user.getState().getServer().getId());
    }

    public UserFullDto toFullDto(UserEntity user) {
        return new UserFullDto(
                user.getId(),
                user.getVersion(),
                user.getName(),
                user.isHuman(),
                user.getFaction().getId(),
                user.getScore(),
                user.getState().getRegion() == null ? null : user.getState().getRegion().getId(),
                user.getState().getX(),
                user.getState().getY(),
                user.getState().getZ(),
                user.getState().getPitch(),
                user.getState().getYaw(),
                user.getCreated(),
                user.getUpdated(),
                user.getLastLogin(),
                user.getNavigated(),
                user.getLastJoin(),
                user.getState().getSeen(),
                user.getRoles().stream().map(UserEntity.Role::name).collect(Collectors.toList()),
                user.getState().getToken().toString(),
                user.getState().getDestinationServer() == null ? null : user.getState().getDestinationServer().getId(),
                user.getState().getServer() == null ? null : user.getState().getServer().getId());
    }
}
