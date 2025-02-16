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

import adhoc.message.MessageService;
import adhoc.user.event.ServerUserDefeatedUserEvent;
import adhoc.user.event.UserDefeatedUserEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class UserEventService {

    private final UserRepository userRepository;

    private final MessageService messageService;

    @Retryable(retryFor = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 100, maxDelay = 1000))
    public UserDefeatedUserEvent userDefeatedUser(ServerUserDefeatedUserEvent serverUserDefeatedUserEvent) {
        User user = userRepository.getReferenceById(serverUserDefeatedUserEvent.getUserId());
        User defeatedUser = userRepository.getReferenceById(serverUserDefeatedUserEvent.getDefeatedUserId());

        BigDecimal scoreAdd = BigDecimal.valueOf(user.isHuman() ? 1.0f : 0.1f);
        userRepository.updateScoreAddById(scoreAdd, user.getId());

        if (user.isHuman() || defeatedUser.isHuman()) {
            messageService.addGlobalMessage(String.format("%s defeated %s", user.getName(), defeatedUser.getName()));
        }

        // TODO
        return new UserDefeatedUserEvent(
                user.getId(), user.getVersion() + 1, user.getName(), user.isHuman(),
                defeatedUser.getId(), defeatedUser.getVersion(), defeatedUser.getName(), defeatedUser.isHuman());
    }
}
