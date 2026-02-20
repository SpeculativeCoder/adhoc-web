/*
 * Copyright (c) 2022-2026 SpeculativeCoder (https://github.com/SpeculativeCoder)
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
import adhoc.faction.FactionRepository;
import adhoc.region.RegionEntity;
import adhoc.region.RegionRepository;
import adhoc.server.ServerEntity;
import adhoc.server.ServerRepository;
import com.google.common.base.Preconditions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.AbstractMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class ObjectiveManagerService {

    private final ObjectiveRepository objectiveRepository;
    private final RegionRepository regionRepository;
    private final AreaRepository areaRepository;
    private final FactionRepository factionRepository;
    private final ServerRepository serverRepository;

    private final ObjectiveService objectiveService;

    public ObjectiveDto updateObjective(ObjectiveDto objectiveDto) {
        // NOTE: this is a two stage save to allow linked objectives to be set too
        ObjectiveEntity objective = toEntityStage1(objectiveDto, objectiveRepository.getReferenceById(objectiveDto.getId()));

        objective = toEntityStage2(objectiveDto, objective);

        return objectiveService.toDto(objective);
    }

    @Retryable(includes = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxRetries = 3, delay = 100, jitter = 10, multiplier = 1, maxDelay = 1000)
    public List<ObjectiveDto> updateServerObjectives(Long serverId, List<ObjectiveDto> objectiveDtos) {
        ServerEntity server = serverRepository.getReferenceById(serverId);
        RegionEntity serverRegion = server.getRegion();

        Set<Integer> objectiveIndexes = new TreeSet<>();
        for (ObjectiveDto objectiveDto : objectiveDtos) {
            Preconditions.checkArgument(Objects.equals(serverRegion.getId(), objectiveDto.getRegionId()));

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

        try (Stream<ObjectiveEntity> objectivesToDelete = objectiveRepository.streamByRegionAndIndexNotIn(serverRegion, objectiveIndexes)) {
            objectivesToDelete.forEach(objectiveToDelete -> {
                log.info("Deleting unused objective: {}", objectiveToDelete);
                objectiveRepository.delete(objectiveToDelete);
            });
        }

        // NOTE: this is a two stage save to allow linked objectives to be set too
        return objectiveDtos.stream().map(objectiveDto -> {
            ObjectiveEntity objective = toEntityStage1(objectiveDto,
                    objectiveRepository.findByRegionAndIndex(serverRegion, objectiveDto.getIndex()).orElseGet(ObjectiveEntity::new));

            // set faction to be initial faction if this is a new entity
            if (objective.getId() == null) {
                objective.setFaction(objective.getInitialFaction());
            }

            return new AbstractMap.SimpleEntry<>(objectiveDto, objectiveRepository.save(objective));
        }).toList().stream().map(entry -> {
            ObjectiveEntity objective = toEntityStage2(entry.getKey(), entry.getValue());

            return objectiveService.toDto(objective);
        }).toList();
    }

    ObjectiveEntity toEntityStage1(ObjectiveDto objectiveDto, ObjectiveEntity objective) {
        RegionEntity region = regionRepository.getReferenceById(objectiveDto.getRegionId());

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

    ObjectiveEntity toEntityStage2(ObjectiveDto objectiveDto, ObjectiveEntity objective) {
        RegionEntity region = regionRepository.getReferenceById(objectiveDto.getRegionId());

        Set<ObjectiveEntity> linkedObjectives = objectiveDto.getLinkedObjectiveIndexes().stream()
                .map(linkedObjectiveIndex ->
                        objectiveRepository.getByRegionAndIndex(region, linkedObjectiveIndex))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        objective.getLinkedObjectives().retainAll(linkedObjectives);
        objective.getLinkedObjectives().addAll(linkedObjectives);

        return objective;
    }
}
