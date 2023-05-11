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

package adhoc.region;

import adhoc.area.Area;
import adhoc.region.dto.RegionDto;
import adhoc.server.Server;
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
public class RegionService {

    private final RegionRepository regionRepository;

    public List<RegionDto> getRegions() {
        return regionRepository.findAll(PageRequest.of(0, 100, Sort.Direction.ASC, "id"))
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public RegionDto getRegion(Long regionId) {
        return toDto(regionRepository.getReferenceById(regionId));
    }

    private RegionDto toDto(Region region) {
        return new RegionDto(
                region.getId(),
                region.getVersion(),
                region.getName(),
                region.getMapName(),
                region.getX(), region.getY(), region.getZ(),
                region.getAreas().stream().map(Area::getId).collect(Collectors.toList()),
                region.getServers().stream().map(Server::getId).collect(Collectors.toList()));
    }
}
