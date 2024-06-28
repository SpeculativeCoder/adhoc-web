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

import adhoc.pawn.Pawn;
import adhoc.pawn.PawnRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Stream;

@Transactional
@Service
@Slf4j
@RequiredArgsConstructor
public class UserPawnService {

    private final UserRepository userRepository;
    private final PawnRepository pawnRepository;

    @Retryable(retryFor = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 100, maxDelay = 1000))
    public void manageUserPawns() {
        LocalDateTime now = LocalDateTime.now();
        log.trace("Managing user pawns... now={}", now);

        // manage users who we think are connected to a server
        try (Stream<User> users = userRepository.streamByServerNotNull()) {
            users.forEach(user -> {

                // see if there is a pawn for the user
                Optional<Pawn> optionalPawn = pawnRepository.findFirstByServerAndUserOrderBySeenDescIdDesc(user.getServer(), user);

                //user.getPawns().stream()
                //.filter(pawn -> pawn.getServer() == user.getServer())
                //.max(Comparator.comparing(Pawn::getSeen));

                if (optionalPawn.isPresent()) {
                    updateUserForPawn(user, optionalPawn.get(), now);
                }
            });
        }
    }

    private static void updateUserForPawn(User user, Pawn pawn, LocalDateTime now) {
        user.setX(pawn.getX());
        user.setY(pawn.getY());
        user.setZ(pawn.getZ());
        user.setPitch(pawn.getPitch());
        user.setYaw(pawn.getYaw());

        user.setSeen(now);
    }
}
