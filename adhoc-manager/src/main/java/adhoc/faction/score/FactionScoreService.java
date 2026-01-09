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

package adhoc.faction.score;

import adhoc.faction.FactionEntity;
import adhoc.faction.FactionRepository;
import adhoc.objective.ObjectiveRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class FactionScoreService {

    private final FactionRepository factionRepository;
    private final ObjectiveRepository objectiveRepository;

    /**
     * Award faction score according to how many objectives the faction currently owns.
     * Also decay all faction scores.
     */
    @Retryable(includes = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxRetries = 3, delay = 100, jitter = 10, multiplier = 1, maxDelay = 1000)
    public void awardAndDecayFactionScores() {
        log.trace("Awarding and decaying faction scores...");

        List<ObjectiveRepository.FactionObjectiveCount> factionObjectiveCounts =
                objectiveRepository.getFactionObjectiveCounts();

        for (ObjectiveRepository.FactionObjectiveCount factionObjectiveCount : factionObjectiveCounts) {
            FactionEntity faction = factionObjectiveCount.getFaction();
            Integer objectiveCount = factionObjectiveCount.getObjectiveCount();

            BigDecimal scoreAdd = BigDecimal.valueOf(0.01).multiply(BigDecimal.valueOf(objectiveCount));

            factionRepository.updateScoreAddById(scoreAdd, faction.getId());
        }

        // TODO: multiplier property
        BigDecimal scoreMultiply = BigDecimal.valueOf(0.999);

        factionRepository.updateScoreMultiply(scoreMultiply);
    }
}
