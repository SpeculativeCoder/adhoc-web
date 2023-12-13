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
import adhoc.region.Region;
import adhoc.region.RegionRepository;
import adhoc.server.Server;
import adhoc.server.ServerRepository;
import adhoc.user.UserRepository;
import com.google.common.base.Verify;
import com.google.common.collect.Sets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Transactional
@Service
@Slf4j
@RequiredArgsConstructor
public class ManagerObjectiveService {

    private final ServerRepository serverRepository;
    private final ObjectiveRepository objectiveRepository;
    private final RegionRepository regionRepository;
    private final AreaRepository areaRepository;
    private final FactionRepository factionRepository;
    private final UserRepository userRepository;

    private final ObjectiveService objectiveService;

    public ObjectiveDto updateObjective(ObjectiveDto objectiveDto) {
        // NOTE: this is a two stage save to allow linked objectives to be set too
        return objectiveService.toDto(
                toEntityStage2(objectiveDto,
                        toEntityStage1(objectiveDto, objectiveRepository.getReferenceById(objectiveDto.getId()))));
    }

    public List<ObjectiveDto> processServerObjectives(Long serverId, List<ObjectiveDto> objectiveDtos) {
        Server server = serverRepository.getReferenceById(serverId);
        Region region = server.getRegion();

        Set<Long> objectiveIds = new TreeSet<>();

        // NOTE: this is a two stage save to allow linked objectives to be set too (they need to be saved in the first pass to get ids)
        List<Pair<ObjectiveDto, Objective>> dtoEntityPairs = objectiveDtos.stream()
                .peek(objectiveDto -> Verify.verify(Objects.equals(region.getId(), objectiveDto.getRegionId())))
                .map(objectiveDto -> Pair.of(objectiveDto,
                        objectiveRepository.save(toEntityStage1(objectiveDto,
                                objectiveRepository.findForUpdateByRegionAndIndex(region, objectiveDto.getIndex()).orElseGet(Objective::new)))))
                .toList();

        objectiveDtos = dtoEntityPairs.stream()
                .map(dtoEntityPair ->
                        toEntityStage2(dtoEntityPair.getLeft(), dtoEntityPair.getRight()))
                .peek(objective -> objectiveIds.add(objective.getId()))
                .map(objectiveService::toDto)
                .collect(Collectors.toList());

        //if (objectiveIds.isEmpty()) {
        //    objectiveIds.add(-1L);
        //}

        try (Stream<Objective> objectivesToDelete = objectiveRepository.streamForUpdateByRegionAndIdNotIn(region, objectiveIds)) {
            objectivesToDelete.forEach(objectiveToDelete -> {
                log.info("Deleting objective: {}", objectiveToDelete);
                objectiveRepository.delete(objectiveToDelete);
            });
        }

        return objectiveDtos;
    }

    Objective toEntityStage1(ObjectiveDto objectiveDto, Objective objective) {
        Region region = regionRepository.getReferenceById(objectiveDto.getRegionId());

        objective.setRegion(region);
        objective.setIndex(objectiveDto.getIndex());

        objective.setName(objectiveDto.getName());

        objective.setX(objectiveDto.getX());
        objective.setY(objectiveDto.getY());
        objective.setZ(objectiveDto.getZ());

        objective.setInitialFaction(objectiveDto.getInitialFactionIndex() == null ? null :
                factionRepository.getByIndex(objectiveDto.getInitialFactionIndex()));

        // NOTE: we never change existing non-null faction (if admin wishes to do this - they can send an objective taken event)
        objective.setFaction(objective.getFaction() == null ? objective.getInitialFaction() : objective.getFaction());

        // NOTE: linked objectives will be set in stage 2
        //objective.setLinkedObjectives(new ArrayList<>());

        objective.setArea(areaRepository.getByRegionAndIndex(region, objectiveDto.getAreaIndex()));

        return objective;
    }

    Objective toEntityStage2(ObjectiveDto objectiveDto, Objective objective) {
        Region region = regionRepository.getReferenceById(objectiveDto.getRegionId());

        // update the linked objectives if not set yet or the objective indexes have changed
        if (objective.getLinkedObjectives() == null ||
                !objective.getLinkedObjectives().stream().map(Objective::getIndex).collect(Collectors.toSet())
                        .equals(Sets.newHashSet(objectiveDto.getLinkedObjectiveIndexes()))) {

            objective.setLinkedObjectives(
                    objectiveDto.getLinkedObjectiveIndexes().stream()
                            .map(linkedObjectiveIndex ->
                                    objectiveRepository.getByRegionAndIndex(region, linkedObjectiveIndex))
                            .collect(Collectors.toSet()));
        }

        return objective;
    }

    public void handleObjectiveTaken(ObjectiveTakenEvent objectiveTakenEvent) {

        Faction faction = factionRepository.getForUpdateById(objectiveTakenEvent.getFactionId());
        Objective objective = objectiveRepository.getForUpdateById(objectiveTakenEvent.getObjectiveId());

        faction.setScore(faction.getScore() + 1);
        objective.setFaction(faction);

        userRepository.updateUsersAddScoreByFactionAndSeenAfter(1, faction, LocalDateTime.now().minusMinutes(15));

        log.debug("Objective {} has been taken by {}", objective.getName(), faction.getName());
    }
}
