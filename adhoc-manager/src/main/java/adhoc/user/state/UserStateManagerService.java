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

package adhoc.user.state;

import adhoc.pawn.PawnEntity;
import adhoc.pawn.PawnRepository;
import adhoc.user.UserEntity;
import adhoc.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.LockAcquisitionException;
import org.slf4j.event.Level;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.stream.Stream;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class UserStateManagerService {

    private final UserRepository userRepository;
    private final PawnRepository pawnRepository;

    /**
     * Update information about users which are seen in their desired server (we look for a pawn the user is controlling).
     * Also, if they have not been seen in the server for a while, they must have left so should be unlinked from the server.
     */
    @Retryable(includes = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxRetries = 3, delay = 100, jitter = 10, multiplier = 1, maxDelay = 1000)
    public void manageSeenUsers() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime leaveUsersSeenBefore = now.minusMinutes(1);
        log.trace("Managing seen users... now={} leaveUsersSeenBefore={}", now, leaveUsersSeenBefore);

        // manage users who we think are currently connected to a server
        try (Stream<UserEntity> users = userRepository.streamForWriteByStateServerNotNull()) {
            users.forEach(user -> {
                // users may be moving from one server to another so we are only interested after they have arrived at their desired server
                if (user.getState().getServer() == user.getState().getDestinationServer()) {

                    //user.getPawns().stream()
                    //.filter(pawn -> pawn.getServer() == user.getServer())
                    //.max(Comparator.comparing(Pawn::getSeen));

                    // see if there is a pawn for the user
                    pawnRepository.findFirstByServerAndUserOrderBySeenDescIdDesc(user.getState().getServer(), user)
                            .ifPresent(pawn -> updateUserForPawn(user, pawn, now));
                }

                if (user.getState().getSeen() != null && user.getState().getSeen().isBefore(leaveUsersSeenBefore)) {
                    leaveUser(user);
                }
            });
        }
    }

    private static void updateUserForPawn(UserEntity user, PawnEntity pawn, LocalDateTime now) {
        user.getState().setX(pawn.getX());
        user.getState().setY(pawn.getY());
        user.getState().setZ(pawn.getZ());
        user.getState().setPitch(pawn.getPitch());
        user.getState().setYaw(pawn.getYaw());

        user.getState().setSeen(now);
    }

    private static void leaveUser(UserEntity user) {
        log.atLevel(user.isHuman() ? Level.INFO : Level.DEBUG)
                .addKeyValue("id", user.getId())
                .addKeyValue("name", user.getName())
                .addKeyValue("password?", user.getPassword() != null)
                .addKeyValue("human", user.isHuman())
                .addKeyValue("factionIndex", user.getFaction().getIndex())
                .addKeyValue("factionId", user.getFaction().getId())
                .addKeyValue("serverId", user.getState().getServer().getId())
                .log("User left.");

        // TODO: common path?
        user.getState().setServer(null);
    }
}
