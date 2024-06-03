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

import adhoc.faction.Faction;
import adhoc.objective.ObjectiveRepository;
import adhoc.pawn.PawnRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

@Transactional
@Service
@Slf4j
@RequiredArgsConstructor
public class ManagerUserService {

    private final UserRepository userRepository;
    private final ObjectiveRepository objectiveRepository;
    private final PawnRepository pawnRepository;

    private final UserService userService;

    public UserDto updateUser(UserDto userDto) {
        return userService.toDto(
                toEntity(userDto, userRepository.getReferenceById(userDto.getId())));
    }

    private User toEntity(UserDto userDto, User user) {
        // TODO
        //user.setName(userDto.getName());
        //user.setFaction(user.getFaction());

        user.setUpdated(LocalDateTime.now());

        return user;
    }

    @Retryable(retryFor = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 100, maxDelay = 1000))
    public void manageUserScores() {
        log.trace("Managing user scores...");

        List<ObjectiveRepository.FactionObjectiveCount> factionObjectiveCounts =
                objectiveRepository.getFactionObjectiveCounts();

        LocalDateTime seenAfter = LocalDateTime.now().minusHours(48);

        // TODO: move to user
        for (ObjectiveRepository.FactionObjectiveCount factionObjectiveCount : factionObjectiveCounts) {
            Faction faction = factionObjectiveCount.getFaction();
            Integer objectiveCount = factionObjectiveCount.getObjectiveCount();

            BigDecimal humanScoreAdd = BigDecimal.valueOf(0.01).multiply(BigDecimal.valueOf(objectiveCount));
            BigDecimal notHumanScoreAdd = BigDecimal.valueOf(0.001).multiply(BigDecimal.valueOf(objectiveCount));

            userRepository.updateScoreAddByFactionIdAndSeenAfter(humanScoreAdd, notHumanScoreAdd, faction.getId(), seenAfter);
        }

        // TODO: multiplier property
        userRepository.updateScoreMultiply(BigDecimal.valueOf(0.999));
    }

    @Retryable(retryFor = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 100, maxDelay = 1000))
    public void manageUserLocations() {
        log.trace("Managing user locations...");

        LocalDateTime now = LocalDateTime.now();

        try (Stream<User> users = userRepository.streamByServerNotNull()) {
            users.forEach(user -> {
                // see if there is a pawn for the user
                pawnRepository.findFirstByServerAndUserOrderBySeenDescIdDesc(user.getServer(), user).ifPresent(pawn -> {

                    user.setX(pawn.getX());
                    user.setY(pawn.getY());
                    user.setZ(pawn.getZ());
                    user.setPitch(pawn.getPitch());
                    user.setYaw(pawn.getYaw());

                    user.setSeen(now);

                });
            });
        }
    }
}
