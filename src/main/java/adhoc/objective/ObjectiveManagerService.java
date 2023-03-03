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

package adhoc.objective;

import adhoc.area.AreaRepository;
import adhoc.faction.Faction;
import adhoc.faction.FactionRepository;
import adhoc.objective.event.ObjectiveTakenEvent;
import adhoc.objective.dto.ObjectiveDto;
import adhoc.region.Region;
import adhoc.region.RegionRepository;
import adhoc.user.User;
import adhoc.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Transactional
@Service
@Slf4j
@RequiredArgsConstructor
public class ObjectiveManagerService {

    private final ObjectiveRepository objectiveRepository;
    private final RegionRepository regionRepository;
    private final AreaRepository areaRepository;
    private final FactionRepository factionRepository;
    private final UserRepository userRepository;
    private final ObjectiveService objectiveService;

    public ObjectiveDto updateObjective(ObjectiveDto objectiveDto) {
        // NOTE: this is a two stage save to allow linked objectives to be set too
        objectiveRepository.save(toEntityStage1(objectiveDto));

        return objectiveService.toDto(
                toEntityStage2(objectiveDto));
    }

    public List<ObjectiveDto> updateObjectives(List<ObjectiveDto> objectiveDtos) {
        // NOTE: this is a two stage save to allow linked objectives to be set too
        objectiveDtos.stream()
                .map(this::toEntityStage1)
                .forEach(objectiveRepository::save);

        return objectiveDtos.stream()
                .map(this::toEntityStage2)
                .map(objectiveService::toDto)
                .collect(Collectors.toList());
    }

    Objective toEntityStage1(ObjectiveDto objectiveDto) {
        Region region = regionRepository.getReferenceById(objectiveDto.getRegionId());
        Objective objective = objectiveRepository
                .findWithPessimisticWriteLockByRegionAndIndex(region, objectiveDto.getIndex())
                .orElseGet(Objective::new);

        Faction initialFaction = objectiveDto.getInitialFactionIndex() == null ? null :
                factionRepository.getByIndex(objectiveDto.getInitialFactionIndex());
        Faction faction = objectiveDto.getFactionIndex() == null ? null :
                factionRepository.getByIndex(objectiveDto.getFactionIndex());

        objective.setRegion(region);
        objective.setIndex(objectiveDto.getIndex());

        objective.setName(objectiveDto.getName());

        objective.setX(objectiveDto.getX());
        objective.setY(objectiveDto.getY());
        objective.setZ(objectiveDto.getZ());

        objective.setInitialFaction(initialFaction);
        // NOTE: we never change existing non-null faction (if admin wishes to do this - they can send an objective taken event)
        objective.setFaction(objective.getFaction() == null ? initialFaction : objective.getFaction());

        objective.setLinkedObjectives(Collections.emptyList()); // NOTE: will be set in stage 2

        objective.setArea(areaRepository.getByRegionAndIndex(region, objectiveDto.getAreaIndex()));

        return objective;
    }

    Objective toEntityStage2(ObjectiveDto objectiveDto) {
        Region region = regionRepository.getReferenceById(objectiveDto.getRegionId());
        Objective objective = objectiveRepository.getByRegionAndIndex(region, objectiveDto.getIndex());

        objective.setLinkedObjectives(
                objectiveDto.getLinkedObjectiveIndexes().stream().map(linkedObjectiveIndex ->
                        objectiveRepository.getByRegionAndIndex(region, linkedObjectiveIndex)).collect(Collectors.toList()));

        return objective;
    }

    public void processObjectiveTaken(ObjectiveTakenEvent objectiveTakenEvent) {
        Faction faction = factionRepository.getReferenceById(objectiveTakenEvent.getFactionId());
        Objective objective = objectiveRepository.getReferenceById(objectiveTakenEvent.getObjectiveId());

        faction.setScore(faction.getScore() + 1);
        objective.setFaction(faction);

        // TODO: do this without loading all users
        Stream<User> friendlies = userRepository.streamUserByFaction(faction);
        LocalDateTime now = LocalDateTime.now();
        friendlies
                .filter(friendly -> friendly.getLastLogin() != null && friendly.getLastLogin().until(now, ChronoUnit.MINUTES) < 15
                        || friendly.getSeen() != null && friendly.getSeen().until(now, ChronoUnit.MINUTES) < 15)
                .forEach(friendly -> {
                    friendly.setScore(friendly.getScore() + 1);
                });

        log.info("Objective {} has been taken by {}", objective.getName(), faction.getName());
    }
}
