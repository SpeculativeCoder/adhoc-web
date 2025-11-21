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

package adhoc.area;

import adhoc.objective.ObjectiveEntity;
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

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class AreaManagerService {

    private final AreaRepository areaRepository;
    private final RegionRepository regionRepository;
    private final ServerRepository serverRepository;

    private final AreaService areaService;

    @Retryable(includes = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxRetries = 3, delay = 100, jitter = 10, multiplier = 1, maxDelay = 1000)
    public List<AreaDto> updateServerAreas(Long serverId, List<AreaDto> areaDtos) {
        ServerEntity server = serverRepository.getReferenceById(serverId);
        RegionEntity serverRegion = server.getRegion();

        Set<Integer> areaIndexes = new TreeSet<>();
        for (AreaDto areaDto : areaDtos) {
            Preconditions.checkArgument(Objects.equals(serverRegion.getId(), areaDto.getRegionId()),
                    "Region ID mismatch: %s != %s", serverRegion.getId(), areaDto.getRegionId());

            boolean unique = areaIndexes.add(areaDto.getIndex());
            Preconditions.checkArgument(unique, "Area index not unique: %s", areaDto.getIndex());
        }

        try (Stream<AreaEntity> areasToDelete = areaRepository.streamByRegionAndIndexNotIn(serverRegion, areaIndexes)) {
            areasToDelete.forEach(areaToDelete -> {
                log.info("Deleting unused area: {}", areaToDelete);
                // before deleting unused areas we must unlink any objectives that will become orphaned
                for (ObjectiveEntity orphanedObjective : areaToDelete.getObjectives()) {
                    orphanedObjective.setArea(null);
                }
                areaRepository.delete(areaToDelete);
            });
        }

        return areaDtos.stream()
                .map(areaDto -> toEntity(areaDto,
                        areaRepository.findByRegionAndIndex(serverRegion, areaDto.getIndex()).orElseGet(AreaEntity::new)))
                .map(areaRepository::save)
                .map(areaService::toDto)
                .toList();
    }

    AreaEntity toEntity(AreaDto areaDto, AreaEntity area) {
        area.setRegion(regionRepository.getReferenceById(areaDto.getRegionId()));
        area.setIndex(areaDto.getIndex());
        area.setName(areaDto.getName());
        area.setX(areaDto.getX());
        area.setY(areaDto.getY());
        area.setZ(areaDto.getZ());
        area.setSizeX(areaDto.getSizeX());
        area.setSizeY(areaDto.getSizeY());
        area.setSizeZ(areaDto.getSizeZ());
        //noinspection OptionalAssignedToNull
        if (areaDto.getServerId() != null) {
            area.setServer(areaDto.getServerId().map(serverRepository::getReferenceById).orElse(null));
        }

        return area;
    }
}
