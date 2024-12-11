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
import org.slf4j.event.Level;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.stream.Stream;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class UserSeenJobService {

    private final UserRepository userRepository;
    private final PawnRepository pawnRepository;

    /**
     * Update information about users which are seen in their desired server (we look for a pawn the user is controlling).
     * Also, if they have not been seen in the server for a while, they must have left so should be unlinked from the server.
     */
    @Retryable(retryFor = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 100, maxDelay = 1000))
    public void manageSeenUsers() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime leaveUsersSeenBefore = now.minusMinutes(1);
        log.trace("Managing seen users... now={} leaveUsersSeenBefore={}", now, leaveUsersSeenBefore);

        // manage users who we think are currently connected to a server
        try (Stream<User> users = userRepository.streamForWriteByServerNotNull()) {
            users.forEach(user -> {
                // users may be moving from one server to another so we are only interested after they have arrived at their desired server
                if (user.getServer() == user.getDestinationServer()) {

                    //user.getPawns().stream()
                    //.filter(pawn -> pawn.getServer() == user.getServer())
                    //.max(Comparator.comparing(Pawn::getSeen));

                    // see if there is a pawn for the user
                    pawnRepository.findFirstByServerAndUserOrderBySeenDescIdDesc(user.getServer(), user)
                            .ifPresent(pawn -> updateUserForPawn(user, pawn, now));
                }

                if (user.getSeen() != null && user.getSeen().isBefore(leaveUsersSeenBefore)) {
                    leaveUser(user);
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

    private static void leaveUser(User user) {
        log.atLevel(user.isHuman() ? Level.INFO : Level.DEBUG)
                .log("User left: id={} name={} password?={} human={} factionIndex={} serverId={}",
                        user.getId(),
                        user.getName(),
                        user.getPassword() != null,
                        user.isHuman(),
                        user.getFaction().getIndex(),
                        user.getServer().getId());

        // TODO: common path?
        user.setServer(null);
    }
}
