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
import adhoc.area.groups.AreaGroupsFactory;
import adhoc.region.Region;
import adhoc.region.RegionRepository;
import adhoc.server.event.ServerStartedEvent;
import adhoc.server.event.ServerUpdatedEvent;
import adhoc.system.event.Event;
import adhoc.task.ServerTask;
import adhoc.task.ServerTaskRepository;
import com.google.common.base.Verify;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
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
public class ServerManagerService {

    private final ServerProperties serverProperties;

    private final ServerRepository serverRepository;
    private final RegionRepository regionRepository;
    private final AreaRepository areaRepository;
    private final ServerTaskRepository serverTaskRepository;

    private final ServerService serverService;

    private final AreaGroupsFactory areaGroupsFactory;

    @Setter(onMethod_ = {@Autowired}, onParam_ = {@Lazy})
    private ServerManagerService self;

    public List<ServerDto> getServerServers(Long serverId) {
        return serverRepository.findAll().stream().map(serverService::toDto).toList();
    }

    public ServerDto updateServer(ServerDto serverDto) {
        Server server = toEntity(serverDto, serverRepository.getReferenceById(serverDto.getId()));

        return serverService.toDto(server);
    }

    Server toEntity(ServerDto serverDto, Server server) {
        server.setRegion(regionRepository.getReferenceById(serverDto.getRegionId()));
        server.setAreas(serverDto.getAreaIds().stream().map(areaRepository::getReferenceById).collect(Collectors.toList())); // TODO

        server.setMapName(serverDto.getMapName());

        server.setX(server.getX());
        server.setY(server.getY());
        server.setZ(server.getZ());

        //server.setStatus(ServerStatus.INACTIVE); // TODO

        server.setPublicIp(server.getPublicIp());

        server.setWebSocketUrl(server.getWebSocketUrl());

        return server;
    }

    @Retryable(retryFor = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 100, maxDelay = 1000))
    public ServerUpdatedEvent handleServerStarted(ServerStartedEvent serverStartedEvent) {
        Server server = serverRepository.getReferenceById(serverStartedEvent.getServerId());

        server.setActive(true);

        return toServerUpdatedEvent(server);
    }

    public static ServerUpdatedEvent toServerUpdatedEvent(Server server) {
        ServerUpdatedEvent event = new ServerUpdatedEvent(
                server.getId(),
                server.getVersion(),
                server.getRegion().getId(),
                server.getAreas().stream().map(Area::getId).collect(Collectors.toList()),
                server.getAreas().stream().map(Area::getIndex).collect(Collectors.toList()),
                server.getEnabled(),
                server.getActive(),
                server.getPublicIp(),
                server.getPublicWebSocketPort(),
                server.getWebSocketUrl());
        return event;
    }

    /**
     * Manage the required servers to represent the areas within each region.
     * This will typically be based on number of players in each area.
     */
    @Retryable(retryFor = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 100, maxDelay = 1000))
    public List<? extends Event> manageServers() {
        log.trace("Managing servers...");
        List<Event> events = new ArrayList<>();

        try (Stream<Region> regions = regionRepository.streamBy()) {
            regions.forEach(region -> {
                log.trace("Managing servers for region {}", region);
                List<Long> usedServerIds = new ArrayList<>();

                List<Set<Area>> areaGroups = areaGroupsFactory.determineAreaGroups(region);
                log.trace("Region {} area groups: {}", region.getId(), areaGroups);

                for (Set<Area> areaGroup : areaGroups) {
                    // TODO: prefer searching by area(s) that have human(s) in them
                    Area firstArea = areaGroup.iterator().next();
                    Server server = serverRepository.findFirstByRegionAndAreasContains(region, firstArea).orElseGet(Server::new);

                    // adjust server for the required region and area group
                    boolean emitEvent = manageServer(server, region, areaGroup);

                    if (server.getId() == null) {
                        server = serverRepository.save(server);
                        emitEvent = true;
                    }

                    usedServerIds.add(server.getId());

                    if (emitEvent) {
                        events.add(toServerUpdatedEvent(server));
                    }
                }

                // any servers in this region that are no longer used...
                try (Stream<Server> unusedServers = serverRepository.streamByRegionAndIdNotIn(region, usedServerIds)) {
                    unusedServers.forEach(unusedServer -> {
                        boolean emitEvent = manageServer(unusedServer, region, Collections.emptySet());

                        if (emitEvent) {
                            events.add(toServerUpdatedEvent(unusedServer));
                        }
                    });
                }
            });
        }

        return events;
    }

    private boolean manageServer(Server server, Region region, Set<Area> areaGroup) {
        log.trace("Managing server {} for region {} area group {}", server, region, areaGroup);
        boolean emitEvent = false;

        Double areaGroupX = areaGroup.isEmpty() ? null : areaGroup.stream().mapToDouble(Area::getX).average().orElseThrow();
        Double areaGroupY = areaGroup.isEmpty() ? null : areaGroup.stream().mapToDouble(Area::getY).average().orElseThrow();
        Double areaGroupZ = areaGroup.isEmpty() ? null : areaGroup.stream().mapToDouble(Area::getZ).average().orElseThrow();

        if (server.getRegion() != region) {
            server.setRegion(region);
            emitEvent = true;
        }

        Verify.verifyNotNull(region, "region must be not null");
        String regionMapName = region.getMapName();
        if (!Objects.equals(server.getMapName(), regionMapName)) {
            server.setMapName(regionMapName);
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

        // a server should be enabled if it has one or more areas assigned to it
        // (this will trigger the starting of a server task via the hosting service)
        Boolean enabled = !server.getAreas().isEmpty();
        if (!Objects.equals(server.getEnabled(), enabled)) {
            server.setEnabled(enabled);
            emitEvent = true;
        }

        if (server.getActive() == null) {
            server.setActive(false);
            emitEvent = true;
        }

        return emitEvent;
    }

    /**
     * Update server information for a server task that exists (or does not exist)
     */
    @Retryable(retryFor = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 100, maxDelay = 1000))
    public List<? extends Event> updateServersForServerTasks() {
        log.trace("Updating servers for server tasks...");
        List<Event> events = new ArrayList<>();

        try (Stream<Server> servers = serverRepository.streamBy()) {
            servers.forEach(server -> {
                Optional<ServerTask> optionalServerTask = serverTaskRepository.findFirstByServerId(server.getId());

                log.trace("Updating server {} for server task {}", server, optionalServerTask);
                boolean emitEvent = updateServerForServerTask(server, optionalServerTask);

                if (emitEvent) {
                    events.add(toServerUpdatedEvent(server));
                }
            });
        }

        return events;
    }

    private boolean updateServerForServerTask(Server server, Optional<ServerTask> optionalServerTask) {
        log.trace("Managing server {} for server task {}", server, optionalServerTask);
        boolean emitEvent = false;

        Boolean active = server.getActive();
        // if server task has vanished, server is no longer active
        if (active == null || active && optionalServerTask.isEmpty()) {
            active = false;
        }
        if (!Objects.equals(server.getActive(), active)) {
            server.setActive(active);
            emitEvent = true;
        }

        String serverTaskPublicIp = optionalServerTask
                .map(ServerTask::getPublicIp)
                .orElse(null);

        if (!Objects.equals(server.getPublicIp(), serverTaskPublicIp)) {
            server.setPublicIp(serverTaskPublicIp);
            emitEvent = true;
        }

        Integer serverTaskPublicWebSocketPort = optionalServerTask
                .map(ServerTask::getPublicWebSocketPort)
                .orElse(null);

        if (!Objects.equals(server.getPublicWebSocketPort(), serverTaskPublicWebSocketPort)) {
            server.setPublicWebSocketPort(serverTaskPublicWebSocketPort);
            emitEvent = true;
        }

        String serverTaskDomain = optionalServerTask
                .map(ServerTask::getDomain)
                .orElse(null);

        if (!Objects.equals(server.getDomain(), serverTaskDomain)) {
            server.setDomain(serverTaskDomain);
            server.setWebSocketUrl(null); // need to calculate the web socket URL (see below)
            emitEvent = true;
        }

        String webSocketUrl = null;
        if (Boolean.TRUE == server.getEnabled() && Boolean.TRUE == server.getActive() &&
                server.getDomain() != null && server.getPublicIp() != null && server.getPublicWebSocketPort() != null) {
            webSocketUrl = (serverProperties.getSsl().isEnabled() ? "wss://" + server.getDomain() : "ws://" + server.getPublicIp()) +
                    ":" + server.getPublicWebSocketPort();
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

    public void purgeOldServers() {
        LocalDateTime seenBefore = LocalDateTime.now().minusMinutes(5);
        try (Stream<Server> oldServers = serverRepository.streamByAreasEmptyAndUsersEmptyAndPawnsEmptyAndSeenBefore(seenBefore)) {
            oldServers.forEach(oldServer -> {
                log.debug("Deleting old server {}", oldServer.getId());
                serverRepository.delete(oldServer);
            });
        }
    }
}
