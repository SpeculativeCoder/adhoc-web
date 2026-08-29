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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class ObjectiveManagerMapper {

    private final ObjectiveRepository objectiveRepository;
    private final RegionRepository regionRepository;
    private final AreaRepository areaRepository;
    private final FactionRepository factionRepository;

    public ObjectiveEntity toEntityStage1(ObjectiveDto objectiveDto, ObjectiveEntity objective) {
        RegionEntity region = regionRepository.getReferenceById(objectiveDto.getRegionId());

        objective.setRegion(region);
        objective.setIndex(objectiveDto.getIndex());

        objective.setName(objectiveDto.getName());

        objective.setX(objectiveDto.getX());
        objective.setY(objectiveDto.getY());
        objective.setZ(objectiveDto.getZ());

        objective.setInitialFaction(objectiveDto.getInitialFactionIndex() == null ? null
                : factionRepository.findByIndex(objectiveDto.getInitialFactionIndex()).orElseThrow());

        // submitter can override whatever the current faction is (should only be for admin user flipping an objective)
        if (objectiveDto.getFactionIndex() != null) {
            objective.setFaction(factionRepository.findByIndex(objectiveDto.getFactionIndex()).orElseThrow());
        }
        // set faction to be initial faction for new entities
        if (objective.getId() == null && objective.getFaction() == null) {
            objective.setFaction(objective.getInitialFaction());
        }

        // NOTE: linked objectives will be set in stage 2
        if (objective.getLinkedObjectives() == null) {
            objective.setLinkedObjectives(new LinkedHashSet<>());
        }

        objective.setArea(objectiveDto.getAreaIndex() == null ? null :
                areaRepository.findByRegionAndIndex(region, objectiveDto.getAreaIndex()).orElseThrow());

        return objective;
    }

    public ObjectiveEntity toEntityStage2(ObjectiveDto objectiveDto, ObjectiveEntity objective) {
        RegionEntity region = regionRepository.getReferenceById(objectiveDto.getRegionId());

        Set<ObjectiveEntity> linkedObjectives = objectiveDto.getLinkedObjectiveIndexes().stream()
                .map(linkedObjectiveIndex -> objectiveRepository.findByRegionAndIndex(region, linkedObjectiveIndex)
                        .orElseThrow()) // TODO
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (!linkedObjectives.equals(objective.getLinkedObjectives())) {
            objective.setLinkedObjectives(linkedObjectives);
        }

        return objective;
    }
}
