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

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Transactional
@Service
@Profile("mode-manager")
@Slf4j
@RequiredArgsConstructor
public class ManagerUserJobService {

    private final UserRepository userRepository;

    //@Scheduled(cron="0 */1 * * * *")
    public void purgeOldUsers() {
        log.trace("Purging old users...");

        userRepository
                .findWithPessimisticWriteLockBySeenBeforeAndPasswordIsNull(LocalDateTime.now().minusDays(7)) //.minusMinutes(30))
                .forEach(oldUser -> {
                    log.info("Purging old auto-registered user - oldUser={}", oldUser);
//                    for (Pawn pawn : oldUser.getPawns()) {
//                        pawn.setUser(null);
//                    }
                    userRepository.delete(oldUser);
                });
    }
}

//    @EventListener
//    public void contextStarted(ApplicationStartedEvent event) {
//        purgeOldAutoRegisteredUsers();
//    }
