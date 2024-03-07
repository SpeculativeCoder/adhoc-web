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
import adhoc.hosting.HostingState;
import adhoc.properties.ManagerProperties;
import adhoc.region.Region;
import adhoc.region.RegionRepository;
import adhoc.server.event.ServerStartedEvent;
import adhoc.server.event.ServerUpdatedEvent;
import adhoc.system.event.Event;
import adhoc.task.*;
import adhoc.world.ManagerWorldService;
import adhoc.world.event.WorldUpdatedEvent;
import com.google.common.base.Verify;
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

    private static ServerUpdatedEvent toServerUpdatedEvent(Server server) {
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

        Float areaGroupX = (float) areaGroup.stream().mapToDouble(Area::getX).average().orElseThrow();
        Float areaGroupY = (float) areaGroup.stream().mapToDouble(Area::getY).average().orElseThrow();
        Float areaGroupZ = (float) areaGroup.stream().mapToDouble(Area::getZ).average().orElseThrow();

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

    /**
     * Manage the server tasks in the hosting service, creating new ones and/or tearing down old ones as required
     * by the current servers.
     */
    public List<? extends Event> manageServerTasks() {
        log.trace("Managing server tasks...");
        List<Event> events = new ArrayList<>();

        // get state of running containers
        HostingState hostingState = hostingService.poll();
        log.debug("manageServerTasks: hostingState={}", hostingState);
        Verify.verifyNotNull(hostingState, "hostingState is null after polling hosting service");

        // TODO: WIP
        List<String> managerTaskIdentifiers = new ArrayList<>();
        for (ManagerTask task : hostingState.getManagerTasks()) {
            ManagerTask managerTask = managerTaskRepository.findByTaskIdentifier(task.getTaskIdentifier()).orElseGet(ManagerTask::new);
            managerTask.setTaskIdentifier(task.getTaskIdentifier());
            managerTask.setPrivateIp(task.getPrivateIp());
            managerTask.setPublicIp(task.getPublicIp());
            //managerTask.setPublicWebSocketPort(task.getPublicWebSocketPort());
            //managerTask.setManagerId(task.getManagerId());
            managerTaskRepository.save(managerTask);
            managerTaskIdentifiers.add(managerTask.getTaskIdentifier());
        }
        managerTaskRepository.deleteByTaskIdentifierNotIn(managerTaskIdentifiers);

        // TODO: WIP
        List<String> kioskTaskIdentifiers = new ArrayList<>();
        for (KioskTask task : hostingState.getKioskTasks()) {
            KioskTask kioskTask = kioskTaskRepository.findByTaskIdentifier(task.getTaskIdentifier()).orElseGet(KioskTask::new);
            kioskTask.setTaskIdentifier(task.getTaskIdentifier());
            kioskTask.setPrivateIp(task.getPrivateIp());
            kioskTask.setPublicIp(task.getPublicIp());
            //kioskTask.setPublicWebSocketPort(task.getPublicWebSocketPort());
            //kioskTask.setKioskId(task.getKioskId());
            kioskTaskRepository.save(kioskTask);
            kioskTaskIdentifiers.add(kioskTask.getTaskIdentifier());
        }
        kioskTaskRepository.deleteByTaskIdentifierNotIn(kioskTaskIdentifiers);

        // TODO: WIP
        List<String> serverTaskIdentifiers = new ArrayList<>();
        for (ServerTask task : hostingState.getServerTasks()) {
            ServerTask serverTask = serverTaskRepository.findByTaskIdentifier(task.getTaskIdentifier()).orElseGet(ServerTask::new);
            serverTask.setTaskIdentifier(task.getTaskIdentifier());
            serverTask.setPrivateIp(task.getPrivateIp());
            serverTask.setPublicIp(task.getPublicIp());
            serverTask.setPublicWebSocketPort(task.getPublicWebSocketPort());
            serverTask.setServerId(task.getServerId());
            serverTaskRepository.save(serverTask);
            serverTaskIdentifiers.add(serverTask.getTaskIdentifier());
        }
        serverTaskRepository.deleteByTaskIdentifierNotIn(serverTaskIdentifiers);

        // TODO
        Optional<WorldUpdatedEvent> optionalWorldUpdatedEvent =
                managerWorldService.updateManagerAndKioskHosts(
                        hostingState.getManagerTasks().stream().map(ManagerTask::getPublicIp).collect(Collectors.toSet()),
                        hostingState.getKioskTasks().stream().map(KioskTask::getPublicIp).collect(Collectors.toSet()));

        optionalWorldUpdatedEvent.ifPresent(events::add);

        try (Stream<Server> servers = serverRepository.streamBy()) {
            servers.forEach(server -> {
                // TODO
                ServerTask existingTask = serverTaskRepository.findByServerId(server.getId())
                        .orElse(null);

                Optional<ServerUpdatedEvent> optionalServerUpdatedEvent =
                        existingTask != null
                                ? manageExistingServerTask(existingTask, server)
                                : manageMissingServerTask(server);

                optionalServerUpdatedEvent.ifPresent(events::add);
            });
        }

        // any tasks for servers which don't exist should be stopped (typically this is cleanup)
        for (ServerTask serverTask : hostingState.getServerTasks()) {
            if (!serverRepository.existsById(serverTask.getServerId())) {
                log.debug("Server {} does not exist - need to stop task {}", serverTask.getServerId(), serverTask);
                hostingService.stopServerTask(serverTask);
            }
        }

        return events;
    }

    private Optional<ServerUpdatedEvent> manageExistingServerTask(ServerTask task, Server server) {
        log.trace("Managing existing server task {} for server {}", task, server);
        boolean changed = false;

        server.setSeen(LocalDateTime.now());

        if (!Objects.equals(server.getPrivateIp(), task.getPrivateIp())) {
            server.setPrivateIp(task.getPrivateIp());
            changed = true;
        }

        if (!Objects.equals(server.getPublicIp(), task.getPublicIp())) {
            if (task.getPublicIp() != null) {
                server.setStatus(ServerStatus.ACTIVE);

                String serverHost = server.getId() + "-" + managerProperties.getServerDomain();

                int webSocketPort = Verify.verifyNotNull(task.getPublicWebSocketPort(),
                        "public web socket port not available from task when constructing url");
                String webSocketUrl = (serverProperties.getSsl().isEnabled() ?
                        "wss://" + serverHost : "ws://" + task.getPublicIp()) +
                        ":" + webSocketPort;
                server.setWebSocketUrl(webSocketUrl);

                dnsService.createOrUpdateDnsRecord(serverHost, Collections.singleton(task.getPublicIp()));
            }
            server.setPublicIp(task.getPublicIp());
            changed = true;
        }

        if (!Objects.equals(server.getPublicWebSocketPort(), task.getPublicWebSocketPort())) {
            server.setPublicWebSocketPort(task.getPublicWebSocketPort());
            changed = true;
        }

        if (server.getAreas().isEmpty() && server.getStatus() == ServerStatus.ACTIVE) {
            log.debug("Server {} has no assigned areas - need to stop task {}", server.getId(), task);
            hostingService.stopServerTask(task);
            server.setStatus(ServerStatus.STOPPING);
            changed = true;
        }

        return changed ? Optional.of(toServerUpdatedEvent(server)) : Optional.empty();
    }


    private Optional<ServerUpdatedEvent> manageMissingServerTask(Server server) {
        log.trace("Managing missing server task for server {}", server);
        boolean changed = false;

        if (server.getStatus() != ServerStatus.INACTIVE) {
            if (server.getStatus() != ServerStatus.STOPPING) {
                log.warn("Server {} task has stopped unexpectedly!", server.getId());
            } else {
                log.debug("Server {} task has stopped successfully", server.getId());
            }
            server.setStatus(ServerStatus.INACTIVE);
        }

        if (server.getPrivateIp() != null) {
            server.setPrivateIp(null);
            changed = true;
        }

        if (server.getPublicIp() != null) {
            server.setPublicIp(null);
            changed = true;
        }

        if (server.getPublicWebSocketPort() != null) {
            server.setPublicWebSocketPort(null);
            changed = true;
        }

        //if (server.getInitiated() != null) {
        //    server.setInitiated(null);
        //    changed = true;
        //}

        //if (server.getSeen() != null) {
        //    server.setSeen(null);
        //    changed = true;
        //}

        if (!server.getAreas().isEmpty()) {
            log.debug("Server {} has assigned areas {} - need to start task", server.getId(),
                    server.getAreas().stream().map(Area::getId).toList());
            try {
                hostingService.startServerTask(server);
                server.setStatus(ServerStatus.STARTING);
                server.setInitiated(LocalDateTime.now());
                changed = true;
            } catch (Exception e) {
                log.warn("Failed to start server {}!", server.getId(), e);
                server.setStatus(ServerStatus.ERROR);
            }
        }

        return changed ? Optional.of(toServerUpdatedEvent(server)) : Optional.empty();
    }

    private void stopAllServerTasks() {
        // get state of running containers
        HostingState hostingState = hostingService.poll();
        log.debug("stopAllServerTasks: hostingState={}", hostingState);
        Verify.verifyNotNull(hostingState, "hostingState is null after polling hosting service");

        for (ServerTask task : hostingState.getServerTasks()) {
            log.debug("Stopping task {}", task);
            hostingService.stopServerTask(task);
        }
    }
}
