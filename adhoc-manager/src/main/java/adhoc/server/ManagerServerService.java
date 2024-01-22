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
            log.trace("Region {} area groups: {}", region.getId(), areaGroups);

            for (Set<Area> areaGroup : areaGroups) {
                Optional<ServerUpdatedEvent> optionalEvent =
                        manageServer(region, areaGroup);

                // TODO
                optionalEvent.ifPresent(event -> {
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

        Area firstArea = areaGroup.iterator().next();
        // TODO: average across all the areas
        Float areaGroupX = firstArea.getX();
        Float areaGroupY = firstArea.getY();
        Float areaGroupZ = firstArea.getZ();

        // TODO: prefer searching by area(s) that have human(s) in them
        Server server = serverRepository.findFirstByRegionAndAreasContains(region, firstArea).orElseGet(Server::new);

        if (server.getRegion() != region) {
            server.setRegion(region);
            sendEvent = true;
        }
        if (!Objects.equals(server.getMapName(), region.getMapName())) {
            server.setMapName(region.getMapName());
            sendEvent = true;
        }

        if (!Objects.equals(server.getX(), areaGroupX)
                || !Objects.equals(server.getY(), areaGroupY)
                || !Objects.equals(server.getZ(), areaGroupZ)) {
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

        try (Stream<Server> servers = serverRepository.streamBy()) {
            servers.forEach(server -> {
                ServerTask task = hostingState.getServerTasks().get(server.getId());
                Optional<ServerUpdatedEvent> optionalEvent =
                        manageServerTask(server, Optional.ofNullable(task));

                // TODO
                optionalEvent.ifPresent(event -> {
                    log.info("Sending: {}", event);
                    stomp.convertAndSend("/topic/events", event);
                });
            });
        }

        // any tasks for servers which don't exist should be stopped (typically this is cleanup)
        for (Map.Entry<Long, ServerTask> entry : hostingState.getServerTasks().entrySet()) {
            Long serverId = entry.getKey();
            ServerTask task = entry.getValue();

            if (!serverRepository.existsById(serverId)) {
                log.debug("Server {} does not exist - need to stop task {}", serverId, task);
                hostingService.stopServerTask(task);
            }
        }
    }

    private Optional<ServerUpdatedEvent> manageServerTask(Server server, Optional<ServerTask> existingTask) {
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

            if (server.getAreas().isEmpty() && server.getStatus() == ServerStatus.ACTIVE) {
                log.info("Server {} has no assigned areas - need to stop task {}", server.getId(), task);
                hostingService.stopServerTask(task);
                server.setStatus(ServerStatus.STOPPING);
                sendEvent = true;
            }

        } else {

            if (server.getStatus() != ServerStatus.INACTIVE) {
                if (server.getStatus() != ServerStatus.STOPPING) {
                    log.warn("Server {} task has stopped unexpectedly!", server.getId());
                } else {
                    log.info("Server {} task has stopped successfully", server.getId());
                }
                server.setStatus(ServerStatus.INACTIVE);
            }

            if (server.getPrivateIp() != null) {
                server.setPrivateIp(null);
                sendEvent = true;
            }

            if (server.getPublicIp() != null) {
                server.setPublicIp(null);
                sendEvent = true;
            }

            if (server.getPublicWebSocketPort() != null) {
                server.setPublicWebSocketPort(null);
                sendEvent = true;
            }

            if (server.getInitiated() != null) {
                server.setInitiated(null);
                sendEvent = true;
            }

            if (server.getSeen() != null) {
                server.setSeen(null);
                sendEvent = true;
            }

            if (!server.getAreas().isEmpty()) {
                log.info("Server {} has assigned areas {} - need to start task", server.getId(),
                        server.getAreas().stream().map(Area::getId).toList());
                try {
                    hostingService.startServerTask(server);
                    server.setStatus(ServerStatus.STARTING);
                    server.setInitiated(LocalDateTime.now());
                    sendEvent = true;
                } catch (Exception e) {
                    log.warn("Failed to start server {}!", server.getId(), e);
                    server.setStatus(ServerStatus.ERROR);
                }
            }
        }

        return sendEvent ? Optional.of(toServerUpdatedEvent(server)) : Optional.empty();
    }

    private void stopAllServerTasks() {
        // get state of running containers
        HostingState hostingState = hostingService.poll();
        log.debug("stopAllTasks: hostingState={}", hostingState);
        Verify.verifyNotNull(hostingState, "hostingState must not be null");

        for (ServerTask task : hostingState.getServerTasks().values()) {
            log.debug("Stopping task {}", task);
            hostingService.stopServerTask(task);
        }
    }
}
