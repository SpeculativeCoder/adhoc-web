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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class ObjectiveService {

    private final ObjectiveRepository objectiveRepository;

    @Transactional(readOnly = true)
    public Page<ObjectiveDto> getObjectives(Pageable pageable) {
        return objectiveRepository.findAll(pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public ObjectiveDto getObjective(Long objectiveId) {
        return toDto(objectiveRepository.getReferenceById(objectiveId));
    }

    ObjectiveDto toDto(ObjectiveEntity objective) {
        return new ObjectiveDto(
                objective.getId(),
                objective.getVersion(),
                objective.getRegion().getId(),
                objective.getIndex(),
                objective.getName(),
                objective.getX(), objective.getY(), objective.getZ(),
                objective.getInitialFaction() == null ? null : objective.getInitialFaction().getId(),
                objective.getInitialFaction() == null ? null : objective.getInitialFaction().getIndex(),
                objective.getFaction() == null ? null : objective.getFaction().getId(),
                objective.getFaction() == null ? Optional.empty() : Optional.of(objective.getFaction().getIndex()),
                objective.getLinkedObjectives().stream().map(ObjectiveEntity::getId).collect(Collectors.toList()),
                objective.getLinkedObjectives().stream().map(ObjectiveEntity::getIndex).collect(Collectors.toList()),
                objective.getArea() == null ? null : objective.getArea().getId(),
                objective.getArea() == null ? null : objective.getArea().getIndex());
    }
}
