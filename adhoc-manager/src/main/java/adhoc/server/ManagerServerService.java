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
import adhoc.hosting.ServerTask;
import adhoc.properties.ManagerProperties;
import adhoc.region.Region;
import adhoc.region.RegionRepository;
import adhoc.server.event.ServerStartedEvent;
import adhoc.server.event.ServerUpdatedEvent;
import adhoc.world.ManagerWorldService;
import com.google.common.base.Verify;
import com.google.common.collect.Sets;
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

    private final ManagerWorldService managerWorldService;
    private final HostingService hostingService;
    private final DnsService dnsService;
    private final ServerService serverService;

    private final AreaGroupsFactory areaGroupsFactory;

    private final SimpMessageSendingOperations stomp;
    private final EntityManager entityManager;

    public ServerDto updateServer(ServerDto serverDto) {
        return serverService.toDto(
                toEntity(serverDto, serverRepository.getReferenceById(serverDto.getId())));
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
    public void manageServers() {
        log.trace("Managing servers...");

        List<Region> regions = regionRepository.findAll();

        for (Region region : regions) {
            log.trace("Managing servers for region {}", region.getId());

            List<Set<Area>> areaGroups = areaGroupsFactory.determineAreaGroups(region);
            log.trace("areaGroups: {}", areaGroups);

            for (Set<Area> areaGroup : areaGroups) {
                Optional<ServerUpdatedEvent> optionalEvent = manageServer(region, areaGroup);
                optionalEvent.ifPresent(event -> {
                    // TODO
                    log.info("Sending: {}", event);
                    stomp.convertAndSend("/topic/events", event);
                });
            }
        }

        try (Stream<Server> unusedServers = serverRepository.streamByAreasEmptyAndUsersEmptyAndPawnsEmpty()) {
            unusedServers.forEach(unusedServer -> {
                log.info("Deleting unused server {}", unusedServer);
                serverRepository.delete(unusedServer);
            });
        }
    }

    private Optional<ServerUpdatedEvent> manageServer(Region region, Set<Area> areaGroup) {
        log.trace("Managing server for region {} and area group {}", region.getId(), areaGroup);

        boolean sendEvent = false;

        // TODO: prefer areas that have humans in them
        Area firstArea = areaGroup.iterator().next();
        Server server = serverRepository.findFirstByAreasContains(firstArea).orElseGet(Server::new);

        if (server.getRegion() == null || !region.getId().equals(server.getRegion().getId())) {
            server.setRegion(region);
            sendEvent = true;
        }
        if (!region.getMapName().equals(server.getMapName())) {
            server.setMapName(region.getMapName());
            sendEvent = true;
        }

        // TODO: average across all the areas
        if (!firstArea.getX().equals(server.getX())
                || !firstArea.getY().equals(server.getY())
                || !firstArea.getZ().equals(server.getZ())) {
            server.setX(firstArea.getX());
            server.setY(firstArea.getY());
            server.setZ(firstArea.getZ());
            sendEvent = true;
        }


        if (server.getAreas() == null) {
            server.setAreas(new ArrayList<>());
            sendEvent = true;
        }
        // remove areas no longer represented by this server
        for (Iterator<Area> iter = server.getAreas().iterator(); iter.hasNext(); ) {
            Area existingArea = iter.next();
            if (!areaGroup.contains(existingArea)) {
                log.info("Server {} no longer contains area {}", server.getId(), existingArea.getId());
                iter.remove();
                existingArea.setServer(null);
                sendEvent = true;
            }
        }
        // link new areas represented by this server
        for (Area area : areaGroup) {
            if (!server.getAreas().contains(area)) {
                log.info("Server {} now contains area {}", server.getId(), area.getId());
                server.getAreas().add(area);
                area.setServer(server);
                sendEvent = true;
            }
        }

        if (server.getId() == null) {
            server.setName(""); // the id will be added after insert (see below)
            server.setStatus(ServerStatus.INACTIVE);

            server = serverRepository.save(server);

            server.setName(server.getId().toString());

            log.info("New server {} assigned to region {} areas {}", server.getId(), server.getRegion().getId(),
                    server.getAreas().stream().map(Area::getId).toList());
            sendEvent = true;
        }

        return sendEvent ? Optional.of(toServerUpdatedEvent(server)) : Optional.empty();
    }

    /**
     * Manage the server tasks in the hosting service, creating new ones and/or tearing down old ones as required
     * by the current servers.
     */
    public void manageServerTasks() {
        log.trace("Managing server tasks...");

        // get state of running containers
        HostingState hostingState = hostingService.poll();
        log.debug("manageHostingTasks: hostingState={}", hostingState);
        Verify.verifyNotNull(hostingState, "hostingState must not be null");

        managerWorldService.updateManagerAndKioskHosts(hostingState.getManagerHosts(), hostingState.getKioskHosts());

        Set<ServerTask> tasksToKeep = Sets.newLinkedHashSet();
        try (Stream<Server> servers = serverRepository.streamByAreasNotEmpty()) {
            servers.forEach(server -> {
                ServerTask task = hostingState.getServerTasks().get(server.getId());
                if (task != null) {
                    tasksToKeep.add(task);
                }
                manageServerTask(server, Optional.ofNullable(task));
            });
        }

        for (ServerTask task : hostingState.getServerTasks().values()) {
            if (!tasksToKeep.contains(task)) {
                log.debug("Need to stop task {}", task);
                hostingService.stopServerTask(task);
            }
        }
    }

    private void manageServerTask(Server server, Optional<ServerTask> existingTask) {
        boolean sendEvent = false;

        if (existingTask.isPresent()) {
            ServerTask task = existingTask.get();

            server.setSeen(LocalDateTime.now());

            if (!Objects.equals(server.getPrivateIp(), task.getPrivateIp())) {
                server.setPrivateIp(task.getPrivateIp());
                sendEvent = true;
            }

            if (!Objects.equals(server.getPublicIp(), task.getPublicIp())) {
                if (task.getPublicIp() != null) {
                    server.setStatus(ServerStatus.ACTIVE);

                    String serverHost = server.getId() + "-" + managerProperties.getServerDomain();

                    int webSocketPort = Objects.requireNonNull(task.getPublicWebSocketPort(),
                            "web socket port not available from task when constructing url");

                    String webSocketUrl = (serverProperties.getSsl().isEnabled() ?
                            "wss://" + serverHost : "ws://" + task.getPublicIp()) +
                            ":" + webSocketPort;

                    server.setWebSocketUrl(webSocketUrl);

                    dnsService.createOrUpdateDnsRecord(serverHost, Collections.singleton(task.getPublicIp()));
                }
                server.setPublicIp(task.getPublicIp());
                sendEvent = true;
            }

            if (!Objects.equals(server.getPublicWebSocketPort(), task.getPublicWebSocketPort())) {
                server.setPublicWebSocketPort(task.getPublicWebSocketPort());
                sendEvent = true;
            }

        } else {

            if (server.getStatus() == ServerStatus.ACTIVE) {
                log.warn("Server {} seems to have stopped running!", server.getId());
                server.setStatus(ServerStatus.INACTIVE);

                server.setPrivateIp(null);
                server.setPublicIp(null);
                server.setPublicWebSocketPort(null);

                server.setInitiated(null);
                server.setSeen(null);
                sendEvent = true;

            } else if (server.getStatus() == ServerStatus.INACTIVE) {
                // don't start a server unless it will be able to connect to a manager
                //if (hostingState.getManagerHosts().isEmpty()) {
                //    log.warn("Need to start server {} but no manager(s) available!", server.getId());
                //} else {
                log.info("Need to start server {}", server.getId());
                try {
                    hostingService.startServerTask(server); //, hostingState.getManagerHosts());
                    server.setStatus(ServerStatus.STARTING);
                    server.setInitiated(LocalDateTime.now());
                    //server.setManagerHost(hostingState.getManagerHosts().iterator().next());
                    sendEvent = true;
                } catch (Exception e) {
                    log.warn("Failed to start server {}!", server.getId(), e);
                    server.setStatus(ServerStatus.ERROR);
                    //throw new RuntimeException("Failed to start server "+server.getId(), e);
                }
                //}
            }
        }

        if (sendEvent) {
            ServerUpdatedEvent event = toServerUpdatedEvent(server);
            log.info("Sending: {}", event);
            stomp.convertAndSend("/topic/events", event);
        }
    }

    private void stopAllServerTasks() {
        // get state of running containers
        HostingState hostingState = hostingService.poll();
        log.debug("stopAllTasks: hostingState={}", hostingState);
        if (hostingState == null) {
            return;
        }
        for (ServerTask task : hostingState.getServerTasks().values()) {
            log.debug("Stopping task {}", task);
            hostingService.stopServerTask(task);
        }
    }
}
