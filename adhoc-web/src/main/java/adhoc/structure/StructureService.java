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

package adhoc.structure;

import adhoc.faction.FactionRepository;
import adhoc.region.Region;
import adhoc.region.RegionRepository;
import adhoc.server.Server;
import adhoc.server.ServerRepository;
import adhoc.structure.event.StructureCreatedEvent;
import adhoc.structure.dto.StructureDto;
import adhoc.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Transactional
@Service
@Slf4j
@RequiredArgsConstructor
public class StructureService {
    private final ServerRepository serverRepository;
    private final StructureRepository structureRepository;
    private final FactionRepository factionRepository;
    private final RegionRepository regionRepository;
    private final UserRepository userRepository;

    public List<StructureDto> getStructures() {
        return structureRepository.findAll(PageRequest.of(0, 100, Sort.Direction.ASC, "id"))
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public StructureDto getStructure(Long structureId) {
        return toDto(structureRepository.getReferenceById(structureId));
    }

    public StructureDto updateStructure(StructureDto structureDto) {
        return toDto(structureRepository.save(toEntity(structureDto)));
    }

    public List<StructureDto> updateServerStructures(Long serverId, List<StructureDto> structureDtos) {
        Server server = serverRepository.getReferenceById(serverId);
        // if there are ANY existing structures for this region - the submission is ignored (setup was done on a previous request).
        if (structureRepository.existsByRegionId(server.getRegion().getId())) {
            return structureRepository.findByRegionIdOrderById(server.getRegion().getId())
                    .map(this::toDto)
                    .collect(Collectors.toList());
        }
        return structureDtos.stream()
                .map(this::toEntity)
                .map(structureRepository::save)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public StructureCreatedEvent processStructureCreated(StructureCreatedEvent structureCreatedEvent) {
        Structure structure = toEntity(structureCreatedEvent);

        structure = structureRepository.save(structure);

        structureCreatedEvent.setId(structure.getId());
        structureCreatedEvent.setVersion(structure.getVersion());

        return structureCreatedEvent;
    }

    private StructureDto toDto(Structure structure) {
        return new StructureDto(
                structure.getId(),
                structure.getVersion(),
                structure.getUuid(),
                structure.getName(),
                structure.getType(),
                structure.getRegion().getId(),
                structure.getX(), structure.getY(), structure.getZ(),
                structure.getPitch(), structure.getYaw(), structure.getRoll(),
                structure.getScaleX(), structure.getScaleY(), structure.getScaleZ(),
                structure.getSizeX(), structure.getSizeY(), structure.getSizeZ(),
                structure.getFaction() == null ? null : structure.getFaction().getId(),
                structure.getFaction() == null ? null : structure.getFaction().getIndex(),
                structure.getUser() == null ? null : structure.getUser().getId());
    }

    private Structure toEntity(StructureDto structureDto) {
        Region region = regionRepository.getReferenceById(structureDto.getRegionId());
        Structure structure = structureRepository.findWithPessimisticWriteLockByRegionAndUuid(region, structureDto.getUuid()).orElseGet(Structure::new);

        structure.setRegion(region);
        structure.setUuid(structureDto.getUuid());
        structure.setName(structureDto.getName());
        structure.setType(structureDto.getType());
        structure.setRegion(region);
        structure.setX(structureDto.getX());
        structure.setY(structureDto.getY());
        structure.setZ(structureDto.getZ());
        structure.setPitch(structureDto.getPitch());
        structure.setYaw(structureDto.getYaw());
        structure.setRoll(structureDto.getRoll());
        structure.setScaleX(structureDto.getScaleX());
        structure.setScaleY(structureDto.getScaleY());
        structure.setScaleZ(structureDto.getScaleZ());
        structure.setSizeX(structureDto.getSizeX());
        structure.setSizeY(structureDto.getSizeY());
        structure.setSizeZ(structureDto.getSizeZ());
        structure.setFaction(structureDto.getFactionId() == null ? null : factionRepository.getReferenceById(structureDto.getFactionId()));
        structure.setUser(structureDto.getUserId() == null ? null : userRepository.getReferenceById(structureDto.getUserId()));

        return structure;
    }
}
