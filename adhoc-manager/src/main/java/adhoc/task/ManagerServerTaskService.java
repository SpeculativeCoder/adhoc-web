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

package adhoc.task;

import adhoc.area.Area;
import adhoc.dns.DnsService;
import adhoc.hosting.HostingService;
import adhoc.hosting.HostingState;
import adhoc.properties.ManagerProperties;
import adhoc.server.Server;
import adhoc.server.ServerRepository;
import adhoc.server.ServerStatus;
import adhoc.server.event.ServerUpdatedEvent;
import adhoc.system.event.Event;
import com.google.common.base.Verify;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

import static adhoc.server.ManagerServerService.toServerUpdatedEvent;

@Transactional
@Service
@Slf4j
@RequiredArgsConstructor
public class ManagerServerTaskService {

    private final ManagerProperties managerProperties;
    private final ServerProperties serverProperties;

    private final ServerRepository serverRepository;
    private final ServerTaskRepository serverTaskRepository;

    private final HostingService hostingService;
    private final DnsService dnsService;

    /**
     * Manage the server tasks in the hosting service, creating new ones and/or tearing down old ones as required by the current servers.
     */
    public List<? extends Event> manageServerTasks() {
        log.trace("Managing server tasks...");
        List<Event> events = new ArrayList<>();

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
        try (Stream<ServerTask> serverTasks = serverTaskRepository.streamBy()) {
            serverTasks.forEach(serverTask -> {
                if (!serverRepository.existsById(serverTask.getServerId())) {
                    log.debug("Server {} does not exist - need to stop task {}", serverTask.getName(), serverTask.getName());
                    hostingService.stopServerTask(serverTask);
                }
            });
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