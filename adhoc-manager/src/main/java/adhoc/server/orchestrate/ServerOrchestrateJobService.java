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

package adhoc.server.orchestrate;

import adhoc.area.Area;
import adhoc.area.groups.AreaGroupsFactory;
import adhoc.region.Region;
import adhoc.region.RegionRepository;
import adhoc.server.Server;
import adhoc.server.ServerManagerEventService;
import adhoc.server.ServerRepository;
import adhoc.server.ServerUpdatedEvent;
import adhoc.system.AdhocStompEvent;
import adhoc.task.ServerTask;
import adhoc.task.ServerTaskRepository;
import com.google.common.base.Verify;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class ServerOrchestrateJobService {

    private final ServerProperties serverProperties;

    private final ServerRepository serverRepository;
    private final RegionRepository regionRepository;
    private final ServerTaskRepository serverTaskRepository;

    private final ServerManagerEventService serverManagerEventService;

    private final AreaGroupsFactory areaGroupsFactory;

    /**
     * Manage servers to ensure every area is being represented by a server, creating new servers as needed.
     * Some servers may represent more than one area - this will typically be based on number of players in each area.
     */
    @Retryable(retryFor = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 100, maxDelay = 1000))
    public List<? extends AdhocStompEvent> manageServers() {
        log.trace("Managing servers...");
        List<AdhocStompEvent> events = new ArrayList<>();

        try (Stream<Region> regions = regionRepository.streamBy()) {
            regions.forEach(region -> {
                log.trace("Managing servers for region {}", region);
                List<Long> usedServerIds = new ArrayList<>();

                List<Set<Area>> areaGroups = areaGroupsFactory.determineAreaGroups(region);
                log.trace("Region {} area groups: {}", region.getId(), areaGroups);

                for (Set<Area> areaGroup : areaGroups) {
                    // TODO: prefer searching by area(s) that have human(s) in them
                    Area firstArea = areaGroup.iterator().next();

                    // try to use an existing server for this area group, otherwise take an existing unused server or create a new server
                    Server server = serverRepository.findFirstByRegionAndAreasContains(region, firstArea)
                            .orElseGet(() -> serverRepository.findFirstByRegionAndAreasEmpty(region).orElseGet(Server::new));

                    // adjust server as necessary to ensure it is representing this region and area group
                    boolean emitEvent = updateServerWithRegionAndAreaGroup(server, region, areaGroup);

                    if (server.getId() == null) {
                        server = serverRepository.save(server);
                        emitEvent = true;
                    }

                    usedServerIds.add(server.getId());

                    if (emitEvent) {
                        ServerUpdatedEvent event = serverManagerEventService.toServerUpdatedEvent(server);
                        //log.info("{}", event);
                        events.add(event);
                    }
                }

                // any servers in this region that are no longer used should be adjusted to ensure they are not representing any areas
                try (Stream<Server> unusedServers = serverRepository.streamByRegionAndIdNotIn(region, usedServerIds)) {
                    unusedServers.forEach(unusedServer -> {
                        log.trace("Managing unused server {} for region {}", unusedServer, region);

                        boolean emitEvent = updateServerWithRegionAndAreaGroup(unusedServer, region, Collections.emptySet());

                        if (emitEvent) {
                            ServerUpdatedEvent event = serverManagerEventService.toServerUpdatedEvent(unusedServer);
                            //log.info("{}", event);
                            events.add(event);
                        }
                    });
                }
            });
        }

        return events;
    }

    private boolean updateServerWithRegionAndAreaGroup(Server server, Region region, Set<Area> areaGroup) {
        log.trace("Updating server {} with region {} and area group {}", server, region, areaGroup);

        Optional<ServerTask> optionalServerTask = Optional.ofNullable(server.getId()).flatMap(serverTaskRepository::findFirstByServerId);

        Verify.verifyNotNull(region, "region must be not null");
        String mapName = region.getMapName();

        OptionalDouble averageX = areaGroup.stream().map(Area::getX).mapToDouble(BigDecimal::doubleValue).average();
        OptionalDouble averageY = areaGroup.stream().map(Area::getY).mapToDouble(BigDecimal::doubleValue).average();
        OptionalDouble averageZ = areaGroup.stream().map(Area::getZ).mapToDouble(BigDecimal::doubleValue).average();

        BigDecimal areaGroupX = averageX.isPresent() ? BigDecimal.valueOf(averageX.getAsDouble()).setScale(32, RoundingMode.HALF_UP) : null;
        BigDecimal areaGroupY = averageY.isPresent() ? BigDecimal.valueOf(averageY.getAsDouble()).setScale(32, RoundingMode.HALF_UP) : null;
        BigDecimal areaGroupZ = averageZ.isPresent() ? BigDecimal.valueOf(averageZ.getAsDouble()).setScale(32, RoundingMode.HALF_UP) : null;

        // a server should be enabled if it has one or more areas assigned to it
        // (this will trigger the starting of a server task via the hosting service)
        boolean enabled = !areaGroup.isEmpty();

        // only remain marked as active if there is still a server task
        boolean active = server.isActive() && optionalServerTask.isPresent();

        String publicIp = optionalServerTask
                .map(ServerTask::getPublicIp)
                .orElse(null);

        Integer publicWebSocketPort = optionalServerTask
                .map(ServerTask::getPublicWebSocketPort)
                .orElse(null);

        String domain = optionalServerTask
                .map(ServerTask::getDomain)
                .orElse(null);

        String webSocketUrl = null;
        if (server.isEnabled() && active &&
                publicIp != null && publicWebSocketPort != null &&
                (domain != null || !serverProperties.getSsl().isEnabled())) {

            webSocketUrl = (serverProperties.getSsl().isEnabled() ? "wss://" + domain : "ws://" + publicIp)
                    + ":" + publicWebSocketPort;
        }

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
        for (Iterator<Area> iter = server.getAreas().iterator(); iter.hasNext(); ) {
            Area existingArea = iter.next();
            if (!areaGroup.contains(existingArea)) {
                log.debug("Server {} no longer contains area {}", server, existingArea);
                iter.remove();
                existingArea.setServer(null);
                emitEvent = true;
            }
        }
        // link new areas represented by this server
        for (Area area : areaGroup) {
            if (!server.getAreas().contains(area)) {
                log.debug("Server {} now contains area {}", server, area);
                server.getAreas().add(area);
                area.setServer(server);
                emitEvent = true;
            }
        }

        if (!Objects.equals(server.getMapName(), mapName)) {
            server.setMapName(mapName);
            emitEvent = true;
        }

        if (!Objects.equals(server.getX(), areaGroupX)
                || !Objects.equals(server.getY(), areaGroupY)
                || !Objects.equals(server.getZ(), areaGroupZ)) {
            server.setX(areaGroupX);
            server.setY(areaGroupY);
            server.setZ(areaGroupZ);
            emitEvent = true;
        }

        if (server.isEnabled() != enabled) {
            server.setEnabled(enabled);
            emitEvent = true;
        }

        if (server.isActive() != active) {
            server.setActive(active);
            emitEvent = true;
        }

        if (!Objects.equals(server.getPublicIp(), publicIp)) {
            server.setPublicIp(publicIp);
            emitEvent = true;
        }

        if (!Objects.equals(server.getPublicWebSocketPort(), publicWebSocketPort)) {
            server.setPublicWebSocketPort(publicWebSocketPort);
            emitEvent = true;
        }

        if (!Objects.equals(server.getDomain(), domain)) {
            server.setDomain(domain);
            emitEvent = true;
        }

        if (!Objects.equals(server.getWebSocketUrl(), webSocketUrl)) {
            server.setWebSocketUrl(webSocketUrl);
            emitEvent = true;
        }

        if (optionalServerTask.isPresent()) {
            server.setSeen(LocalDateTime.now());
            // this is updated every time so don't emit an event
        }

        return emitEvent;
    }
}
