/*
 * Copyright (c) 2022-2024 SpeculativeCoder (https://github.com/SpeculativeCoder)
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

package adhoc.server;

import adhoc.area.Area;
import adhoc.area.AreaRepository;
import adhoc.dns.DnsService;
import adhoc.hosting.HostingService;
import adhoc.properties.ManagerProperties;
import adhoc.region.Region;
import adhoc.region.RegionRepository;
import adhoc.server.event.ServerStartedEvent;
import adhoc.server.event.ServerUpdatedEvent;
import adhoc.system.event.Event;
import adhoc.task.KioskTaskRepository;
import adhoc.task.ManagerTaskRepository;
import adhoc.task.ServerTaskRepository;
import adhoc.world.ManagerWorldService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Transactional
@Service
@Slf4j
@RequiredArgsConstructor
public class ManagerServerService {

    private final ManagerProperties managerProperties;
    private final ServerProperties serverProperties;

    private final ServerRepository serverRepository;
    private final RegionRepository regionRepository;
    private final AreaRepository areaRepository;
    private final ManagerTaskRepository managerTaskRepository;
    private final KioskTaskRepository kioskTaskRepository;
    private final ServerTaskRepository serverTaskRepository;

    private final ManagerWorldService managerWorldService;
    private final HostingService hostingService;
    private final DnsService dnsService;
    private final ServerService serverService;

    private final AreaGroupsFactory areaGroupsFactory;

    private final SimpMessageSendingOperations stomp;
    private final EntityManager entityManager;

    public ServerDto updateServer(ServerDto serverDto) {
        Server server = toEntity(serverDto, serverRepository.getReferenceById(serverDto.getId()));

        return serverService.toDto(server);
    }

    Server toEntity(ServerDto serverDto, Server server) {
        server.setName(serverDto.getName());

        server.setRegion(regionRepository.getReferenceById(serverDto.getRegionId()));
        server.setAreas(serverDto.getAreaIds().stream().map(areaRepository::getReferenceById).collect(Collectors.toList())); // TODO

        server.setMapName(serverDto.getMapName());

        server.setX(server.getX());
        server.setY(server.getY());
        server.setZ(server.getZ());

        server.setStatus(ServerStatus.INACTIVE);

        server.setManagerHost(server.getManagerHost());
        server.setPrivateIp(server.getPrivateIp());
        server.setPublicIp(server.getPublicIp());

        server.setWebSocketUrl(server.getWebSocketUrl());

        return server;
    }

    public ServerUpdatedEvent handleServerStarted(ServerStartedEvent serverStartedEvent) {
        Server server = serverRepository.getReferenceById(serverStartedEvent.getServerId());

        server.setStatus(ServerStatus.ACTIVE);
        //server.setPrivateIp(serverStartedEvent.getPrivateIp());
        //server.setManagerHost(server.getManagerHost());

        return toServerUpdatedEvent(server);
    }

    public static ServerUpdatedEvent toServerUpdatedEvent(Server server) {
        ServerUpdatedEvent event = new ServerUpdatedEvent(
                server.getId(),
                server.getVersion(),
                server.getName(),
                server.getRegion().getId(),
                server.getAreas().stream().map(Area::getId).collect(Collectors.toList()),
                server.getAreas().stream().map(Area::getIndex).collect(Collectors.toList()),
                server.getStatus().name(),
                server.getManagerHost(),
                server.getPrivateIp(),
                server.getPublicIp(),
                server.getPublicWebSocketPort(),
                server.getWebSocketUrl());
        return event;
    }

    /**
     * Manage the required servers to represent the areas within each region.
     * This will typically be based on number of players in each area.
     */
    public List<? extends Event> manageServers() {
        log.trace("Managing servers...");
        List<Event> events = new ArrayList<>();

        try (Stream<Region> regions = regionRepository.streamBy()) {
            regions.forEach(region -> {
                log.trace("Managing servers for region {}", region);

                List<Set<Area>> areaGroups = areaGroupsFactory.determineAreaGroups(region);
                log.trace("Region {} area groups: {}", region.getId(), areaGroups);

                areaGroups.forEach(areaGroup ->
                        manageServer(region, areaGroup).ifPresent(events::add));
            });
        }

        LocalDateTime seenBefore = LocalDateTime.now().minusMinutes(5);
        try (Stream<Server> unusedServers = serverRepository.streamByAreasEmptyAndUsersEmptyAndPawnsEmptyAndSeenBefore(seenBefore)) {
            unusedServers.forEach(unusedServer -> {
                log.debug("Deleting unused server {}", unusedServer);
                serverRepository.delete(unusedServer);
            });
        }

        return events;
    }

    private Optional<ServerUpdatedEvent> manageServer(Region region, Set<Area> areaGroup) {
        log.trace("Managing server for region {} area group {}", region.getId(), areaGroup);
        boolean changed = false;

        double areaGroupX = areaGroup.stream().mapToDouble(Area::getX).average().orElseThrow();
        double areaGroupY = areaGroup.stream().mapToDouble(Area::getY).average().orElseThrow();
        double areaGroupZ = areaGroup.stream().mapToDouble(Area::getZ).average().orElseThrow();

        // TODO: prefer searching by area(s) that have human(s) in them
        Area firstArea = areaGroup.iterator().next();
        Server server = serverRepository.findFirstByRegionAndAreasContains(region, firstArea).orElseGet(Server::new);

        if (server.getRegion() != region) {
            server.setRegion(region);
            changed = true;
        }
        if (!Objects.equals(server.getMapName(), region.getMapName())) {
            server.setMapName(region.getMapName());
            changed = true;
        }

        if (!Objects.equals(server.getX(), areaGroupX)
                || !Objects.equals(server.getY(), areaGroupY)
                || !Objects.equals(server.getZ(), areaGroupZ)) {
            server.setX(areaGroupX);
            server.setY(areaGroupY);
            server.setZ(areaGroupZ);
            changed = true;
        }

        if (server.getAreas() == null) {
            server.setAreas(new ArrayList<>());
            changed = true;
        }
        // remove areas no longer represented by this server
        for (Iterator<Area> iter = server.getAreas().iterator(); iter.hasNext(); ) {
            Area existingArea = iter.next();
            if (!areaGroup.contains(existingArea)) {
                log.debug("Server {} no longer contains area {}", server, existingArea);
                iter.remove();
                existingArea.setServer(null);
                changed = true;
            }
        }
        // link new areas represented by this server
        for (Area area : areaGroup) {
            if (!server.getAreas().contains(area)) {
                log.debug("Server {} now contains area {}", server, area);
                server.getAreas().add(area);
                area.setServer(server);
                changed = true;
            }
        }

        if (server.getId() == null) {
            server.setName(""); // the id will be added after insert (see below)
            server.setStatus(ServerStatus.INACTIVE);

            server = serverRepository.save(server);

            server.setName("S" + server.getId().toString());

            log.debug("New server {} assigned to region {} areas {}", server, server.getRegion(), server.getAreas());
            changed = true;
        }

        return changed ? Optional.of(toServerUpdatedEvent(server)) : Optional.empty();
    }
}
