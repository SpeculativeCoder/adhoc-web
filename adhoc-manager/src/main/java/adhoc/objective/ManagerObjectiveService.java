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

package adhoc.objective;

import adhoc.area.AreaRepository;
import adhoc.faction.Faction;
import adhoc.faction.FactionRepository;
import adhoc.objective.event.ObjectiveTakenEvent;
import adhoc.objective.event.ServerObjectiveTakenEvent;
import adhoc.region.Region;
import adhoc.region.RegionRepository;
import adhoc.server.Server;
import adhoc.server.ServerRepository;
import adhoc.user.UserRepository;
import com.google.common.base.Preconditions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Transactional
@Service
@Slf4j
@RequiredArgsConstructor
public class ManagerObjectiveService {

    private final ObjectiveRepository objectiveRepository;
    private final ServerRepository serverRepository;
    private final RegionRepository regionRepository;
    private final AreaRepository areaRepository;
    private final FactionRepository factionRepository;
    private final UserRepository userRepository;

    private final ObjectiveService objectiveService;

    public ObjectiveDto updateObjective(ObjectiveDto objectiveDto) {
        // NOTE: this is a two stage save to allow linked objectives to be set too
        Objective objective = toEntityStage1(objectiveDto, objectiveRepository.getReferenceById(objectiveDto.getId()));

        objective = toEntityStage2(objectiveDto, objective);

        return objectiveService.toDto(objective);
    }

    Objective toEntityStage1(ObjectiveDto objectiveDto, Objective objective) {
        Region region = regionRepository.getReferenceById(objectiveDto.getRegionId());

        objective.setRegion(region);
        objective.setIndex(objectiveDto.getIndex());

        objective.setName(objectiveDto.getName());

        objective.setX(objectiveDto.getX());
        objective.setY(objectiveDto.getY());
        objective.setZ(objectiveDto.getZ());

        objective.setInitialFaction(objectiveDto.getInitialFactionIndex() == null ? null
                : factionRepository.getByIndex(objectiveDto.getInitialFactionIndex()));

        //noinspection OptionalAssignedToNull
        if (objectiveDto.getFactionIndex() != null) {
            objective.setFaction(objectiveDto.getFactionIndex().map(factionRepository::getByIndex).orElse(null));
        }

        // NOTE: linked objectives will be set in stage 2
        if (objective.getLinkedObjectives() == null) {
            objective.setLinkedObjectives(new LinkedHashSet<>());
        }

        objective.setArea(areaRepository.getByRegionAndIndex(region, objectiveDto.getAreaIndex()));

        return objective;
    }

    Objective toEntityStage2(ObjectiveDto objectiveDto, Objective objective) {
        Region region = regionRepository.getReferenceById(objectiveDto.getRegionId());

        Set<Objective> linkedObjectives = objectiveDto.getLinkedObjectiveIndexes().stream()
                .map(linkedObjectiveIndex ->
                        objectiveRepository.getByRegionAndIndex(region, linkedObjectiveIndex))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        objective.getLinkedObjectives().retainAll(linkedObjectives);
        objective.getLinkedObjectives().addAll(linkedObjectives);

        return objective;
    }

    @Retryable(retryFor = {ObjectOptimisticLockingFailureException.class, PessimisticLockingFailureException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 500, maxDelay = 2000))
    public List<ObjectiveDto> processServerObjectives(Long serverId, List<ObjectiveDto> objectiveDtos) {
        Server server = serverRepository.getReferenceById(serverId);
        Region region = server.getRegion();

        Set<Integer> objectiveIndexes = new TreeSet<>();
        for (ObjectiveDto objectiveDto : objectiveDtos) {
            Preconditions.checkArgument(Objects.equals(region.getId(), objectiveDto.getRegionId()));

            objectiveDto.getLinkedObjectiveIndexes().forEach(linkedObjectiveIndex -> {
                Preconditions.checkArgument(!Objects.equals(objectiveDto.getIndex(), linkedObjectiveIndex),
                        "Self loop not allowed: %s -> %s", objectiveDto.getIndex(), linkedObjectiveIndex);

                for (ObjectiveDto otherObjectiveDto : objectiveDtos) {
                    if (Objects.equals(otherObjectiveDto.getIndex(), linkedObjectiveIndex)) {
                        Preconditions.checkArgument(otherObjectiveDto.getLinkedObjectiveIndexes().contains(objectiveDto.getIndex()),
                                "Linked objectives must have matching backlink: %s -> %s", otherObjectiveDto.getIndex(), linkedObjectiveIndex);
                    }
                }
            });

            boolean unique = objectiveIndexes.add(objectiveDto.getIndex());
            Preconditions.checkArgument(unique, "Objective index not unique: %s", objectiveDto.getIndex());
        }

        try (Stream<Objective> objectivesToDelete = objectiveRepository.streamByRegionAndIndexNotIn(region, objectiveIndexes)) {
            objectivesToDelete.forEach(objectiveToDelete -> {
                log.info("Deleting unused objective: {}", objectiveToDelete);
                objectiveRepository.delete(objectiveToDelete);
            });
        }

        // NOTE: this is a two stage save to allow linked objectives to be set too
        return objectiveDtos.stream().map(objectiveDto -> {
            Objective objective = toEntityStage1(objectiveDto,
                    objectiveRepository.findByRegionAndIndex(region, objectiveDto.getIndex()).orElseGet(Objective::new));

            // set faction to be initial faction if this is a new entity
            if (objective.getId() == null) {
                objective.setFaction(objective.getInitialFaction());
            }

            return new AbstractMap.SimpleEntry<>(objectiveDto, objectiveRepository.save(objective));
        }).toList().stream().map(entry -> {
            Objective objective = toEntityStage2(entry.getKey(), entry.getValue());

            return objectiveService.toDto(objective);
        }).toList();
    }

    @Retryable(retryFor = {ObjectOptimisticLockingFailureException.class, PessimisticLockingFailureException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 100, maxDelay = 500))
    public ObjectiveTakenEvent handleObjectiveTaken(ServerObjectiveTakenEvent event) {
        Objective objective = objectiveRepository.getReferenceById(event.getObjectiveId());
        Faction faction = factionRepository.getReferenceById(event.getFactionId());

        log.debug("Objective {} has been taken by {}", objective.getName(), faction.getName());

        objective.setFaction(faction);

        factionRepository.updateScoreAddById(BigDecimal.valueOf(1.0), faction.getId());

        LocalDateTime seenAfter = LocalDateTime.now().minusMinutes(15);
        userRepository.updateScoreAddByHumanAndFactionIdAndSeenAfter(BigDecimal.valueOf(1.0), true, faction.getId(), seenAfter);
        userRepository.updateScoreAddByHumanAndFactionIdAndSeenAfter(BigDecimal.valueOf(0.1), false, faction.getId(), seenAfter);

        // TODO
        return new ObjectiveTakenEvent(objective.getId(), objective.getVersion(), faction.getId(), faction.getVersion() + 1);
    }
}
