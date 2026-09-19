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
import com.google.common.base.Verify;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.LockAcquisitionException;
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
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class ServerAllocateService {

    private final ServerRepository serverRepository;
    private final RegionRepository regionRepository;

    private final AreasAllocationStrategy areasAllocationStrategy;

    /**
     * Manage servers to ensure every area is assigned to a server, creating new servers as needed.
     * Some servers may represent more than one area - this will typically be based on the number of players in each area.
     */
    @Retryable(includes = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxRetries = 3, delay = 100, jitter = 10, multiplier = 1, maxDelay = 1000)
    public List<? extends Event> allocateServers() {
        log.trace("Allocating servers...");
        List<Event> events = new ArrayList<>();

        try (Stream<RegionEntity> regions = regionRepository.streamBy()) {
            regions.forEach(region -> {
                events.addAll(
                        allocateServersForRegion(region));
            });
        }

        return events;
    }

    private List<Event> allocateServersForRegion(RegionEntity region) {
        log.trace("Allocating servers for region {}...", region);
        List<Event> events = new ArrayList<>();
        List<Long> usedServerIds = new ArrayList<>();

        Map<List<AreaEntity>, ServerEntity> areasAllocation = areasAllocationStrategy.calculateAreasAllocation(region);
        log.trace("Region {} areas allocation: {}", region, areasAllocation);

        for (Map.Entry<List<AreaEntity>, ServerEntity> areasAllocationEntry : areasAllocation.entrySet()) {
            List<AreaEntity> areas = areasAllocationEntry.getKey();
            ServerEntity server = areasAllocationEntry.getValue();

            Verify.verifyNotNull(areasAllocationEntry.getKey());
            Verify.verify(!areas.isEmpty());
            Verify.verifyNotNull(areasAllocationEntry.getValue());

            allocateServerForRegionAndAreas(server, region, areas)
                    .ifPresent(events::add);

            usedServerIds.add(server.getId());
        }

        // any servers in this region that are no longer used should be adjusted to ensure they are not representing any areas
        try (Stream<ServerEntity> unusedServers = serverRepository.streamByRegionAndIdNotIn(region, usedServerIds)) {
            unusedServers.forEach(unusedServer -> {
                log.trace("Managing unused server {} for region {}", unusedServer, region);

                List<AreaEntity> areas = Collections.emptyList();

                allocateServerForRegionAndAreas(unusedServer, region, areas)
                        .ifPresent(events::add);
            });
        }

        return events;
    }

    private Optional<ServerAllocateEvent> allocateServerForRegionAndAreas(ServerEntity server, RegionEntity region, List<AreaEntity> areas) {
        log.trace("Allocating server {} for region {} and areas {}", server, region, areas);

        boolean emitEvent = false;

        emitEvent |= updateServerRegion(server, region);
        emitEvent |= updateServerAreas(server, areas);

        // server position is based on the areas it represents
        emitEvent |= updateServerPosition(server);

        // server is enabled if it has one or more areas assigned to it, disabled if no areas
        // when true, a server task will be started/maintained in the hosting service
        // when false, any server task will be torn down
        server.setEnabled(!areas.isEmpty());

        if (server.getId() == null) {
            server = serverRepository.save(server);
        }

        ServerAllocateEvent event = null;
        if (emitEvent) {
            event = serverAllocateEvent(server);
            log.debug("event={}", event);
        }
        return Optional.ofNullable(event);
    }

    private boolean updateServerRegion(ServerEntity server, RegionEntity region) {
        boolean emitEvent = false;

        if (server.getRegion() != region) {
            log.debug("Server {} now has region {}", server, region);
            server.setRegion(region);
            emitEvent = true;
        }

        return emitEvent;
    }

    private boolean updateServerAreas(ServerEntity server, List<AreaEntity> areas) {
        boolean emitEvent = false;

        if (server.getAreas() == null) {
            server.setAreas(new ArrayList<>());
        }
        // remove areas no longer represented by this server
        for (Iterator<AreaEntity> iter = server.getAreas().iterator(); iter.hasNext(); ) {
            AreaEntity existingArea = iter.next();
            if (!areas.contains(existingArea)) {
                log.debug("Server {} no longer contains area {}", server, existingArea);
                iter.remove();
                existingArea.setServer(null);
                emitEvent = true;
            }
        }
        // add new areas represented by this server
        for (AreaEntity area : areas) {
            if (!server.getAreas().contains(area)) {
                log.debug("Server {} now contains area {}", server, area);
                server.getAreas().add(area);
                area.setServer(server);
                emitEvent = true;
            }
        }

        return emitEvent;
    }

    private boolean updateServerPosition(ServerEntity server) {
        boolean emitEvent = false;

        OptionalDouble averageX = server.getAreas().stream().map(AreaEntity::getX).mapToDouble(BigDecimal::doubleValue).average();
        OptionalDouble averageY = server.getAreas().stream().map(AreaEntity::getY).mapToDouble(BigDecimal::doubleValue).average();
        OptionalDouble averageZ = server.getAreas().stream().map(AreaEntity::getZ).mapToDouble(BigDecimal::doubleValue).average();

        BigDecimal serverX = averageX.isPresent() ? BigDecimal.valueOf(averageX.getAsDouble()).setScale(0, RoundingMode.HALF_UP) : null;
        BigDecimal serverY = averageY.isPresent() ? BigDecimal.valueOf(averageY.getAsDouble()).setScale(0, RoundingMode.HALF_UP) : null;
        BigDecimal serverZ = averageZ.isPresent() ? BigDecimal.valueOf(averageZ.getAsDouble()).setScale(0, RoundingMode.HALF_UP) : null;

        if (!Objects.equals(server.getX(), serverX)
                || !Objects.equals(server.getY(), serverY)
                || !Objects.equals(server.getZ(), serverZ)) {
            //log.debug("Server {} now has position {} {} {}", server, serverX, serverY, serverZ);
            server.setX(serverX);
            server.setY(serverY);
            server.setZ(serverZ);
            //emitEvent = true;
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
