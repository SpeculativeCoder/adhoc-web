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
import adhoc.region.RegionRepository;
import adhoc.server.ServerEntity;
import adhoc.server.ServerRepository;
import adhoc.system.Event;
import adhoc.task.ServerTaskRepository;
import com.google.common.base.Verify;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class ServerAllocateService {

    private final ServerProperties serverProperties;

    private final ServerRepository serverRepository;
    private final RegionRepository regionRepository;
    private final ServerTaskRepository serverTaskRepository;

    private final AreasAllocationStrategy areasAllocationStrategy;

    /**
     * Manage servers to ensure every area is being represented by a server, creating new servers as needed.
     * Some servers may represent more than one area - this will typically be based on number of players in each area.
     */
    @Retryable(includes = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxRetries = 3, delay = 100, jitter = 10, multiplier = 1, maxDelay = 1000)
    public List<? extends Event> allocateServers() {
        log.trace("Managing servers...");
        List<Event> events = new ArrayList<>();

        try (Stream<RegionEntity> regions = regionRepository.streamBy()) {
            regions.forEach(region -> {
                log.trace("Managing servers for region {}", region);
                List<Long> usedServerIds = new ArrayList<>();

                Map<Set<AreaEntity>, ServerEntity> areasAllocation = areasAllocationStrategy.calculateAreasAllocation(region);
                log.trace("Region {} areas allocation: {}", region.getId(), areasAllocation);

                for (Map.Entry<Set<AreaEntity>, ServerEntity> areasAllocationEntry : areasAllocation.entrySet()) {
                    Set<AreaEntity> areas = areasAllocationEntry.getKey();
                    ServerEntity server = areasAllocationEntry.getValue();

                    Verify.verifyNotNull(areasAllocationEntry.getKey());
                    Verify.verify(!areas.isEmpty());
                    Verify.verifyNotNull(areasAllocationEntry.getValue());

                    // adjust server as necessary to ensure it is representing this region and area group
                    boolean emitEvent = updateServerWithRegionAndAreaGroup(areasAllocationEntry.getValue(), region, areas);

                    // server is now enabled as it has one or more areas assigned to it
                    // (this will trigger the starting of a server task via the hosting service)
                    server.setEnabled(true);

                    if (server.getId() == null) {
                        server = serverRepository.save(server);
                        emitEvent = true;
                    }

                    usedServerIds.add(server.getId());

                    if (emitEvent) {
                        ServerAllocateEvent event = serverAllocateEvent(server);
                        //log.info("{}", event);
                        events.add(event);
                    }
                }

                // any servers in this region that are no longer used should be adjusted to ensure they are not representing any areas
                try (Stream<ServerEntity> unusedServers = serverRepository.streamByRegionAndIdNotIn(region, usedServerIds)) {
                    unusedServers.forEach(unusedServer -> {
                        log.trace("Managing unused server {} for region {}", unusedServer, region);

                        boolean emitEvent = updateServerWithRegionAndAreaGroup(unusedServer, region, Collections.emptySet());

                        // setting server to disabled will cause its server task to be shut down
                        unusedServer.setEnabled(false);

                        if (emitEvent) {
                            ServerAllocateEvent event = serverAllocateEvent(unusedServer);
                            //log.info("{}", event);
                            events.add(event);
                        }
                    });
                }
            });
        }

        return events;
    }

    private boolean updateServerWithRegionAndAreaGroup(ServerEntity server, RegionEntity region, Set<AreaEntity> areaGroup) {
        log.trace("Updating server {} with region {} and area group {}", server, region, areaGroup);

        OptionalDouble averageX = areaGroup.stream().map(AreaEntity::getX).mapToDouble(BigDecimal::doubleValue).average();
        OptionalDouble averageY = areaGroup.stream().map(AreaEntity::getY).mapToDouble(BigDecimal::doubleValue).average();
        OptionalDouble averageZ = areaGroup.stream().map(AreaEntity::getZ).mapToDouble(BigDecimal::doubleValue).average();

        BigDecimal areaGroupX = averageX.isPresent() ? BigDecimal.valueOf(averageX.getAsDouble()).setScale(32, RoundingMode.HALF_UP) : null;
        BigDecimal areaGroupY = averageY.isPresent() ? BigDecimal.valueOf(averageY.getAsDouble()).setScale(32, RoundingMode.HALF_UP) : null;
        BigDecimal areaGroupZ = averageZ.isPresent() ? BigDecimal.valueOf(averageZ.getAsDouble()).setScale(32, RoundingMode.HALF_UP) : null;

        boolean emitEvent = false;

        if (server.getRegion() != region) {
            server.setRegion(region);
            emitEvent = true;
        }

        if (server.getAreas() == null) {
            server.setAreas(new ArrayList<>());
            emitEvent = true;
        }
        // remove areas no longer represented by this server
        for (Iterator<AreaEntity> iter = server.getAreas().iterator(); iter.hasNext(); ) {
            AreaEntity existingArea = iter.next();
            if (!areaGroup.contains(existingArea)) {
                log.debug("Server {} no longer contains area {}", server, existingArea);
                iter.remove();
                existingArea.setServer(null);
                emitEvent = true;
            }
        }
        // link new areas represented by this server
        for (AreaEntity area : areaGroup) {
            if (!server.getAreas().contains(area)) {
                log.debug("Server {} now contains area {}", server, area);
                server.getAreas().add(area);
                area.setServer(server);
                emitEvent = true;
            }
        }

        if (!Objects.equals(server.getX(), areaGroupX)
                || !Objects.equals(server.getY(), areaGroupY)
                || !Objects.equals(server.getZ(), areaGroupZ)) {
            server.setX(areaGroupX);
            server.setY(areaGroupY);
            server.setZ(areaGroupZ);
            emitEvent = true;
        }

        return emitEvent;
    }

    public ServerAllocateEvent serverAllocateEvent(ServerEntity server) {
        return new ServerAllocateEvent(
                server.getId(),
                server.getRegion().getId(),
                server.getAreas().stream().map(AreaEntity::getIndex).collect(Collectors.toList()));
    }
}
