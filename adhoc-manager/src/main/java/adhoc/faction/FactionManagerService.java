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

package adhoc.faction;

import adhoc.objective.ObjectiveRepository;
import adhoc.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Transactional
@Service
@Slf4j
@RequiredArgsConstructor
public class FactionManagerService {

    private final FactionRepository factionRepository;
    private final ObjectiveRepository objectiveRepository;
    private final UserRepository userRepository;

    private final FactionService factionService;

    public FactionDto updateFaction(FactionDto factionDto) {
        Faction faction = toEntity(factionDto, factionRepository.getReferenceById(factionDto.getId()));

        return factionService.toDto(faction);
    }

    Faction toEntity(FactionDto factionDto, Faction faction) {
        faction.setId(faction.getId());
        faction.setIndex(faction.getIndex());
        faction.setName(factionDto.getName());
        faction.setColor(factionDto.getColor());
        faction.setScore(factionDto.getScore());

        return faction;
    }

    @Retryable(retryFor = {TransientDataAccessException.class}, maxAttempts = 3, backoff = @Backoff(delay = 100, maxDelay = 1000))
    public void awardFactionScores() {
        log.trace("Awarding faction scores...");

        List<ObjectiveRepository.FactionObjectiveCount> factionObjectiveCounts =
                objectiveRepository.getFactionObjectiveCounts();

        for (ObjectiveRepository.FactionObjectiveCount factionObjectiveCount : factionObjectiveCounts) {
            Faction faction = factionObjectiveCount.getFaction();
            Integer objectiveCount = factionObjectiveCount.getObjectiveCount();

            BigDecimal scoreAdd = BigDecimal.valueOf(0.01).multiply(BigDecimal.valueOf(objectiveCount));

            factionRepository.updateScoreAddById(scoreAdd, faction.getId());
        }

        LocalDateTime seenBefore = LocalDateTime.now().minusHours(48);

        // TODO: move to user
        for (ObjectiveRepository.FactionObjectiveCount factionObjectiveCount : factionObjectiveCounts) {
            Faction faction = factionObjectiveCount.getFaction();
            Integer objectiveCount = factionObjectiveCount.getObjectiveCount();

            BigDecimal humanScoreAdd = BigDecimal.valueOf(0.01).multiply(BigDecimal.valueOf(objectiveCount));
            BigDecimal nonHumanScoreAdd = BigDecimal.valueOf(0.001).multiply(BigDecimal.valueOf(objectiveCount));

            userRepository.updateScoreAddByHumanAndFactionIdAndSeenAfter(humanScoreAdd, true, faction.getId(), seenBefore);
            userRepository.updateScoreAddByHumanAndFactionIdAndSeenAfter(nonHumanScoreAdd, false, faction.getId(), seenBefore);
        }
    }

    @Retryable(retryFor = {TransientDataAccessException.class}, maxAttempts = 3, backoff = @Backoff(delay = 100, maxDelay = 1000))
    public void decayFactionScores() {
        log.trace("Decaying faction scores...");

        // TODO: multiplier property
        BigDecimal scoreMultiply = BigDecimal.valueOf(0.999);

        factionRepository.updateScoreMultiply(scoreMultiply);
    }
}
