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

package adhoc.area;

import adhoc.objective.ObjectiveRepository;
import adhoc.region.Region;
import adhoc.region.RegionRepository;
import adhoc.server.Server;
import adhoc.server.ServerRepository;
import com.google.common.base.Verify;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

@Transactional
@Service
@Slf4j
@RequiredArgsConstructor
public class ManagerAreaService {

    private final AreaService areaService;

    private final AreaRepository areaRepository;
    private final RegionRepository regionRepository;
    private final ServerRepository serverRepository;
    private final ObjectiveRepository objectiveRepository;

    public List<AreaDto> processServerAreas(Long serverId, List<AreaDto> areaDtos) {
        Server server = serverRepository.getReferenceById(serverId);
        Region region = server.getRegion();

        Set<Long> areaIds = new TreeSet<>();

        List<AreaDto> result = areaDtos.stream()
                .peek(areaDto -> Verify.verify(Objects.equals(region.getId(), areaDto.getRegionId())))
                .map(areaDto -> toEntity(areaDto,
                        areaRepository.findAreaByRegionAndIndex(region, areaDto.getIndex()).orElseGet(Area::new)))
                .map(areaRepository::save)
                .peek(area -> areaIds.add(area.getId()))
                .map(areaService::toDto)
                .toList();

        if (!areaIds.isEmpty() && areaRepository.existsByRegionAndIdNotIn(region, areaIds)) {

            // before deleting areas we must unlink any objectives that will become orphaned
            if (objectiveRepository.existsByRegionAndAreaIdNotIn(region, areaIds)) {
                objectiveRepository.updateObjectivesAreaNullByRegionAndAreaIdNotIn(region, areaIds);
            }

            areaRepository.deleteByRegionAndIdNotIn(region, areaIds);
        }

        return result;
    }

    Area toEntity(AreaDto areaDto, Area area) {
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
            area.setServer(areaDto.getServerId().isEmpty() ? null : serverRepository.getReferenceById(areaDto.getServerId().get()));
        }

        return area;
    }
}
