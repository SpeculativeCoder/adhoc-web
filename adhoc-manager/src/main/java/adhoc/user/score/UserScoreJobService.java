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

package adhoc.user.score;

import adhoc.faction.Faction;
import adhoc.objective.ObjectiveRepository;
import adhoc.user.UserRepository;
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

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class UserScoreJobService {

    private final UserRepository userRepository;
    private final ObjectiveRepository objectiveRepository;

    /**
     * Award user score according to how many objectives the user's faction currently owns.
     * Also decay all user scores.
     */
    @Retryable(retryFor = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 100, maxDelay = 1000))
    public void awardAndDecayUserScores() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime forUsersSeenAfter = now.minusHours(48);
        log.trace("Awarding and decaying user scores... now={} forUsersSeenAfter={}", now, forUsersSeenAfter);

        List<ObjectiveRepository.FactionObjectiveCount> factionObjectiveCounts = objectiveRepository.getFactionObjectiveCounts();

        for (ObjectiveRepository.FactionObjectiveCount factionObjectiveCount : factionObjectiveCounts) {
            Faction faction = factionObjectiveCount.getFaction();
            Integer objectiveCount = factionObjectiveCount.getObjectiveCount();

            BigDecimal scoreToAddForHumans = BigDecimal.valueOf(0.01).multiply(BigDecimal.valueOf(objectiveCount));
            BigDecimal scoreToAddForNonHumans = BigDecimal.valueOf(0.001).multiply(BigDecimal.valueOf(objectiveCount));

            userRepository.updateScoreAddByFactionIdAndSeenAfter(scoreToAddForHumans, scoreToAddForNonHumans, faction.getId(), forUsersSeenAfter);
        }

        // TODO: multiplier property
        userRepository.updateScoreMultiply(BigDecimal.valueOf(0.999));
    }
}
