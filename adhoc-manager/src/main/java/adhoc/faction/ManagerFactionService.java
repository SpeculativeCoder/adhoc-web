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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Stream;

@Transactional
@Service
@Slf4j
@RequiredArgsConstructor
public class ManagerFactionService {

    private final FactionService factionService;

    private final FactionRepository factionRepository;
    private final ObjectiveRepository objectiveRepository;

    public FactionDto updateFaction(FactionDto factionDto) {
        return factionService.toDto(
                toEntity(factionDto, factionRepository.getReferenceById(factionDto.getId())));
    }

    public void awardFactionScores() {
        log.trace("Awarding faction scores...");

        try (Stream<Faction> factions = factionRepository.streamFactionsBy()) {
            factions.forEach(faction ->
                    faction.setScore(faction.getScore() + objectiveRepository.countByFaction(faction)));
        }
    }

    public void decayFactionScores() {
        log.trace("Decaying faction scores...");

        // TODO: multiplier property
        factionRepository.updateFactionsMultiplyScore(0.98F);
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
