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

import adhoc.region.Region;
import adhoc.server.Server;
import adhoc.server.ServerRepository;
import com.google.common.base.Preconditions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Stream;

@Transactional
@Service
@Slf4j
@RequiredArgsConstructor
public class ObjectiveReconcileService {

    private final ObjectiveRepository objectiveRepository;
    private final ServerRepository serverRepository;

    private final ObjectiveService objectiveService;
    private final ManagerObjectiveService managerObjectiveService;

    @Retryable(retryFor = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 100, maxDelay = 1000))
    public List<ObjectiveDto> reconcileServerObjectives(Long serverId, List<ObjectiveDto> objectiveDtos) {
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
            Objective objective = managerObjectiveService.toEntityStage1(objectiveDto,
                    objectiveRepository.findByRegionAndIndex(region, objectiveDto.getIndex()).orElseGet(Objective::new));

            // set faction to be initial faction if this is a new entity
            if (objective.getId() == null) {
                objective.setFaction(objective.getInitialFaction());
            }

            return new AbstractMap.SimpleEntry<>(objectiveDto, objectiveRepository.save(objective));
        }).toList().stream().map(entry -> {
            Objective objective = managerObjectiveService.toEntityStage2(entry.getKey(), entry.getValue());

            return objectiveService.toDto(objective);
        }).toList();
    }
}
