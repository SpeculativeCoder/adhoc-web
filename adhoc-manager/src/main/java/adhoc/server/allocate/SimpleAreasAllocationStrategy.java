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

package adhoc.server.allocate;

import adhoc.area.AreaEntity;
import adhoc.region.RegionEntity;
import adhoc.server.ServerEntity;
import adhoc.server.ServerRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple strategy allocates each area to its own server.
 * This is the default unless another strategy bean is made primary.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SimpleAreasAllocationStrategy implements AreasAllocationStrategy {

    private final ServerRepository serverRepository;

    @PostConstruct
    public void postConstruct() {
        //log.debug("Simple Area Groups Factory is available");
    }

    @Override
    public Map<List<AreaEntity>, ServerEntity> calculateAreasAllocation(RegionEntity region) {

        Map<List<AreaEntity>, ServerEntity> areasAllocation = new LinkedHashMap<>();
        for (AreaEntity area : region.getAreas()) {

            // try to use an existing server for this area, otherwise take an existing unused server, otherwise create a new server
            ServerEntity server = serverRepository.findFirstByRegionAndAreasContainsAndIdNotIn(region, area,
                            areasAllocation.values().stream().map(ServerEntity::getId).toList())
                    .orElseGet(() -> serverRepository.findFirstByRegionAndAreasEmpty(region)
                            .orElseGet(ServerEntity::new));

            areasAllocation.put(Collections.singletonList(area), server);
        }

        return areasAllocation;
    }
}
