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

package adhoc.server;

import adhoc.ManagerProperties;
import adhoc.area.Area;
import adhoc.area.AreaRepository;
import adhoc.dns.DnsService;
import adhoc.hosting.HostingService;
import adhoc.hosting.HostingState;
import adhoc.hosting.ServerTask;
import adhoc.region.Region;
import adhoc.region.RegionRepository;
import adhoc.world.ManagerWorldService;
import com.google.common.collect.Sets;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Transactional
@Service
@Profile("mode-manager")
@Slf4j
@RequiredArgsConstructor
public class ManagerServerJobService {

    private final ManagerProperties managerProperties;

    private final ServerRepository serverRepository;

    private final RegionRepository regionRepository;

    private final AreaRepository areaRepository;

    private final HostingService hostingService;

    private final ManagerWorldService managerWorldService;

    private final ManagerServerService managerServerService;

    private final EntityManager entityManager;

    private final DnsService dnsService;

//    @EventListener
//    public void contextStarted(ApplicationStartedEvent event) {
//        manageNeededServers();
//        manageHostingTasks();
//    }

    public void manageServers() {
        manageNeededServers();
        manageHostingTasks();
    }

    /**
     * Manage the needed servers to represent areas within each region.
     * This will typically be based on number of players in each area.
     */
    //@Scheduled(cron="0 */1 * * * *")
    private void manageNeededServers() {
        log.trace("Managing needed servers...");

        List<Region> regions = regionRepository.findAll();

        for (Region region : regions) {
            log.trace("Managing needed servers for region {}", region.getId());

            // TODO: assign areas to servers based on player count etc. - for now we just do one server per area
            List<List<Area>> areaGroups = new ArrayList<>();
            for (Area area : region.getAreas()) {
                areaGroups.add(Collections.singletonList(area));
            }

            for (List<Area> areaGroup : areaGroups) {
                // TODO: other servers may already be hosting some of the other areas so can't just check first area (but we do for now)
                Area firstArea = areaGroup.get(0);
                Server server = serverRepository.findFirstWithPessimisticWriteLockByAreasContains(firstArea).orElseGet(Server::new);

                if (server.getRegion() == null || !region.getId().equals(server.getRegion().getId())) {
                    server.setRegion(region);
                }
                if (!region.getMapName().equals(server.getMapName())) {
                    server.setMapName(region.getMapName());
                }

                // TODO: average across all the areas
                if (!firstArea.getX().equals(server.getX())
                        || !firstArea.getY().equals(server.getY())
                        || !firstArea.getZ().equals(server.getZ())) {
                    server.setX(firstArea.getX());
                    server.setY(firstArea.getY());
                    server.setZ(firstArea.getZ());
                }

                if (server.getAreas() == null ||
                        !server.getAreas().stream().map(Area::getId).collect(Collectors.toSet()).equals(
                                areaGroup.stream().map(Area::getId).collect(Collectors.toSet()))) {
                    server.setAreas(new ArrayList<>(areaGroup));
                    for (Area area : server.getAreas()) {
                        entityManager.lock(area, LockModeType.PESSIMISTIC_WRITE);
                        area.setServer(server);
                    }
                }

                if (server.getId() == null) {
                    server.setName(""); // the id will be added after insert (see below)
                    server.setStatus(ServerStatus.INACTIVE);

                    server = serverRepository.save(server);

                    server.setName(server.getId().toString());

                    log.info("New server {} assigned to region {} areas {}", server.getId(), server.getRegion().getId(),
                            server.getAreas().stream().map(Area::getId).collect(Collectors.toList()));
                }
            }
        }
    }

    /**
     * Manage the tasks in the hosting service, creating new ones and/or tearing down old ones as required
     * by the current needed servers.
     */
    //@Scheduled(cron="0 */1 * * * *")
    private void manageHostingTasks() {
        log.trace("Managing hosting tasks...");

        // get state of running containers
        HostingState hostingState = hostingService.poll();
        log.debug("manageHostingTasks: hostingState={}", hostingState);
        if (hostingState == null) {
            throw new IllegalStateException("hostingState is null");
        }

        managerWorldService.updateManagerAndKioskHosts(hostingState.getManagerHosts(), hostingState.getKioskHosts());

        Set<ServerTask> tasksToKeep = Sets.newLinkedHashSet();
        List<Server> servers = serverRepository.findWithPessimisticWriteLockBy();

        for (Server server : servers) {
            boolean sendEvent = false;

            ServerTask task = hostingState.getServerTasks().get(server.getId());
            if (task != null) {
                tasksToKeep.add(task);
                server.setSeen(LocalDateTime.now());

                if (!Objects.equals(server.getPrivateIp(), task.getPrivateIp())) {
                    server.setPrivateIp(task.getPrivateIp());
                    sendEvent = true;
                }
                if (!Objects.equals(server.getPublicIp(), task.getPublicIp())) {
                    if (task.getPublicIp() != null) {
                        server.setStatus(ServerStatus.ACTIVE);
                        String webSocketHost = server.getId() + "-" + managerProperties.getServerDomain();
                        int webSocketPort = Objects.requireNonNull(task.getPublicWebSocketPort(),
                                "web socket port not available from task when constructing url");
                        server.setWebSocketUrl("wss://" + webSocketHost + ":" + webSocketPort);
                        dnsService.createOrUpdateDnsRecord(webSocketHost, Collections.singleton(task.getPublicIp()));
                    }
                    server.setPublicIp(task.getPublicIp());
                    sendEvent = true;
                }
                if (!Objects.equals(server.getPublicWebSocketPort(), task.getPublicWebSocketPort())) {
                    server.setPublicWebSocketPort(task.getPublicWebSocketPort());
                    sendEvent = true;
                }

            } else if (task == null) {
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
                managerServerService.sendServerUpdatedEvent(server);
            }
        }

        for (ServerTask task : hostingState.getServerTasks().values()) {
            if (!tasksToKeep.contains(task)) {
                log.debug("Need to stop task {}", task);
                hostingService.stopServerTask(task);
            }
        }
    }

    private void stopAllTasks() {
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
