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
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Transactional
@Service
@Slf4j
@RequiredArgsConstructor
public class ManagerFactionService {

    private final FactionRepository factionRepository;
    private final ObjectiveRepository objectiveRepository;
    private final UserRepository userRepository;

    private final FactionService factionService;

    public FactionDto updateFaction(FactionDto factionDto) {
        return factionService.toDto(
                toEntity(factionDto, factionRepository.getReferenceById(factionDto.getId())));
    }

    @Retryable(retryFor = ObjectOptimisticLockingFailureException.class,
            maxAttempts = 3, backoff = @Backoff(delay = 500, maxDelay = 2000))
    public void awardFactionScores() {
        log.trace("Awarding faction scores...");

        Map<Faction, Integer> factionsNumObjectives = new LinkedHashMap<>();

        for (Faction faction : factionRepository.findAll()) {
            int numObjectives = objectiveRepository.countByFaction(faction);
            factionsNumObjectives.put(faction, numObjectives);

            faction.setScore(faction.getScore() + 0.1f * numObjectives);
        }

        LocalDateTime seenBefore = LocalDateTime.now().minusMinutes(15);
        for (Map.Entry<Faction, Integer> entry : factionsNumObjectives.entrySet()) {
            Faction faction = entry.getKey();
            int numObjectives = entry.getValue();

            userRepository.updateScoreAddByHumanAndFactionIdAndSeenAfter(
                    0.1f * numObjectives, true, faction.getId(), seenBefore);
            userRepository.updateScoreAddByHumanAndFactionIdAndSeenAfter(
                    0.01f * numObjectives, false, faction.getId(), seenBefore);
        }
    }

    @Retryable(retryFor = ObjectOptimisticLockingFailureException.class,
            maxAttempts = 3, backoff = @Backoff(delay = 500, maxDelay = 2000))
    public void decayFactionScores() {
        log.trace("Decaying faction scores...");

        // TODO: multiplier property
        factionRepository.updateScoreMultiply(0.98f);
    }

    Faction toEntity(FactionDto factionDto, Faction faction) {
        faction.setId(faction.getId());
        faction.setIndex(faction.getIndex());
        faction.setName(factionDto.getName());
        faction.setColor(factionDto.getColor());
        faction.setScore(factionDto.getScore());

        return faction;
    }
}
