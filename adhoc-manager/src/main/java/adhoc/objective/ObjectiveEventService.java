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

package adhoc.objective;

import adhoc.faction.Faction;
import adhoc.faction.FactionRepository;
import adhoc.objective.event.ObjectiveTakenEvent;
import adhoc.objective.event.ServerObjectiveTakenEvent;
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

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class ObjectiveEventService {

    private final ObjectiveRepository objectiveRepository;
    private final FactionRepository factionRepository;
    private final UserRepository userRepository;

    @Retryable(retryFor = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 100, maxDelay = 1000))
    public ObjectiveTakenEvent handleObjectiveTaken(ServerObjectiveTakenEvent event) {
        Objective objective = objectiveRepository.getReferenceById(event.getObjectiveId());
        Faction faction = factionRepository.getReferenceById(event.getFactionId());

        log.debug("Objective {} has been taken by {}", objective.getName(), faction.getName());

        objective.setFaction(faction);

        factionRepository.updateScoreAddById(BigDecimal.valueOf(1.0), faction.getId());

        BigDecimal humanScoreAdd = BigDecimal.valueOf(1.0);
        BigDecimal notHumanScoreAdd = BigDecimal.valueOf(0.1);
        LocalDateTime seenAfter = LocalDateTime.now().minusMinutes(15);

        userRepository.updateScoreAddByFactionIdAndSeenAfter(humanScoreAdd, notHumanScoreAdd, faction.getId(), seenAfter);

        // TODO
        return new ObjectiveTakenEvent(objective.getId(), objective.getVersion(), faction.getId(), faction.getVersion() + 1);
    }
}
