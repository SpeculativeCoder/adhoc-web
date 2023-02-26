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

package adhoc.faction;

import adhoc.objective.ObjectiveRepository;
import adhoc.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Transactional
@Service
@Profile("mode-manager")
@Slf4j
@RequiredArgsConstructor
public class FactionManagerJobService {

    private final ObjectiveRepository objectiveRepository;
    private final FactionRepository factionRepository;
    private final UserRepository userRepository;
    private final SimpMessageSendingOperations stomp;

    public void decayUserScores() {
        log.trace("Decaying user scores...");

        // TODO: properties
        userRepository.findAll().forEach(user -> user.setScore(user.getScore() * 0.99F));
    }

    public void decayFactionScores() {
        log.trace("Decaying faction scores...");

        // TODO: properties
        factionRepository.findAll().forEach(faction -> faction.setScore(faction.getScore() * 0.98F));
    }

    public void awardFactionScores() {
        log.trace("Awarding faction scores...");

        Map<Faction, Integer> factionAwardedScores = new LinkedHashMap<>();

        objectiveRepository.findAll().stream()
                .filter(objective -> objective.getFaction() != null)
                .forEach(objective -> factionAwardedScores.merge(objective.getFaction(), 1, (awardedScore, o) -> awardedScore + 1));

        factionRepository.findAll().stream()
                .filter(factionAwardedScores::containsKey)
                .forEach(faction -> faction.setScore(faction.getScore() + factionAwardedScores.get(faction)));

        log.debug("Awarded faction scores: factionAwardedScores={}", factionAwardedScores);
    }
}

//    @EventListener
//    public void contextStarted(ApplicationStartedEvent event) {
//        grantFactionScores();
//        decayFactionScores();
//        decayUserScores();
//    }

//@Scheduled(cron="0 */5 * * * *")
//@Scheduled(cron="0 */1 * * * *")

//        LocalDateTime now = LocalDateTime.now();
//        userRepository.findAll().stream()
//                .filter(user -> user.getLastLogin() != null && user.getLastLogin().until(now, ChronoUnit.MINUTES) < 15
//                        || user.getSeen() != null && user.getSeen().until(now, ChronoUnit.MINUTES) < 15)
//                .filter(user -> factionToAwardedScore.containsKey(user.getFaction()))
//                .forEach(user -> user.setScore(user.getScore() + factionToAwardedScore.get(user.getFaction())));

//        // spit out an event notification of the results
//        Map<Long, Integer> factionIdAwardedScores = factionAwardedScores.entrySet().stream()
//                .collect(Collectors.toMap(entry -> entry.getKey().getId(), Map.Entry::getValue));
//
//        FactionScoringEvent factionScoringEvent = new FactionScoringEvent();
//        factionScoringEvent.setFactionAwardedScores(factionIdAwardedScores);
//
//        log.info("Sending {}", factionScoringEvent);
//        stomp.convertAndSend("/topic/events", factionScoringEvent);
