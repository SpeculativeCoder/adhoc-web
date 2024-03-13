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

package adhoc.task.server;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static adhoc.server.ServerManagerService.toServerUpdatedEvent;

@Transactional
@Service
@Slf4j
@RequiredArgsConstructor
public class ServerTaskManagerService {

    private final ManagerProperties managerProperties;
    private final ServerProperties serverProperties;

    private final ServerRepository serverRepository;
    private final ServerTaskRepository serverTaskRepository;

    private final HostingService hostingService;
    private final DnsService dnsService;

    /**
     * For each server, manage a server task in the hosting service,
     * creating new server tasks and/or tearing down old server tasks as required by the current servers.
     */
    public List<? extends Event> manageServerTasks() {
        log.trace("Managing server tasks...");
        List<Event> events = new ArrayList<>();

        try (Stream<Server> servers = serverRepository.streamBy()) {
            servers.forEach(server -> {
                serverTaskRepository.findByServerId(server.getId())
                        .map(existingServerTask -> manageExistingServerTask(existingServerTask, server))
                        .orElseGet(() -> manageMissingServerTask(server))
                        .ifPresent(events::add);
            });
        }

        // any tasks for servers which don't exist should be stopped (typically this is cleanup from a previous run)
        try (Stream<ServerTask> serverTasks = serverTaskRepository.streamBy()) {
            serverTasks.forEach(serverTask -> {
                if (!serverRepository.existsById(serverTask.getServerId())) {
                    manageOrphanedServerTask(serverTask);
                }
            });
        }

        return events;
    }

    private Optional<ServerUpdatedEvent> manageExistingServerTask(ServerTask task, Server server) {
        log.trace("Managing existing server task {} for server {}", task.getName(), server.getName());

        if (server.getStatus() == ServerStatus.ERROR) {
            return Optional.empty();
        }

        boolean emitEvent = false;

        server.setSeen(LocalDateTime.now());

        if (!Objects.equals(server.getPublicIp(), task.getPublicIp())) {
            server.setPublicIp(task.getPublicIp());
            emitEvent = true;
        }

        if (!Objects.equals(server.getPublicWebSocketPort(), task.getPublicWebSocketPort())) {
            server.setPublicWebSocketPort(task.getPublicWebSocketPort());
            emitEvent = true;
        }

        if (!Objects.equals(server.getDomain(), task.getDomain())) {
            server.setDomain(task.getDomain());
            server.setWebSocketUrl(null); // need to calculate the web socket URL (see below)
            emitEvent = true;
        }

        if (server.getWebSocketUrl() == null
                && server.getDomain() != null && server.getPublicIp() != null && server.getPublicWebSocketPort() != null) {

            server.setWebSocketUrl(
                    (serverProperties.getSsl().isEnabled() ? "wss://" + server.getDomain() : "ws://" + task.getPublicIp()) +
                            ":" + server.getPublicWebSocketPort());

            emitEvent = true;
        }

        if (server.getStatus() == ServerStatus.ACTIVE && server.getAreas().isEmpty()) {
            log.debug("Server {} has no assigned areas - need to stop task {}", server.getName(), task.getName());
            hostingService.stopServerTask(task);
            server.setStatus(ServerStatus.STOPPING);
            emitEvent = true;
        }

        return emitEvent ? Optional.of(toServerUpdatedEvent(server)) : Optional.empty();
    }

    private Optional<ServerUpdatedEvent> manageMissingServerTask(Server server) {
        log.trace("Managing missing server task for server {}", server.getName());

        if (server.getStatus() == ServerStatus.ERROR) {
            return Optional.empty();
        }

        boolean emitEvent = false;

        if (server.getStatus() != ServerStatus.INACTIVE) {
            if (server.getStatus() != ServerStatus.STOPPING) {
                log.warn("Server {} task has stopped unexpectedly!", server.getName());
            } else {
                log.debug("Server {} task has stopped successfully", server.getName());
            }
            server.setStatus(ServerStatus.INACTIVE);
            emitEvent = true;
        }

        if (server.getPublicIp() != null) {
            server.setPublicIp(null);
            emitEvent = true;
        }

        if (server.getPublicWebSocketPort() != null) {
            server.setPublicWebSocketPort(null);
            emitEvent = true;
        }

        if (server.getDomain() != null) {
            server.setDomain(null);
            emitEvent = true;
        }

        if (server.getWebSocketUrl() != null) {
            server.setWebSocketUrl(null);
            emitEvent = true;
        }

        if (server.getStatus() == ServerStatus.INACTIVE && !server.getAreas().isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("Server {} has assigned areas {} - need to start task", server.getName(),
                        server.getAreas().stream().map(Area::getName).toList());
            }

            try {
                ServerTask hostedServerTask = hostingService.startServerTask(server);
                server.setStatus(ServerStatus.STARTING);
                server.setInitiated(LocalDateTime.now());

                ServerTask serverTask = new ServerTask();
                serverTask.setTaskIdentifier(hostedServerTask.getTaskIdentifier());
                serverTask.setName(hostedServerTask.getName());
                serverTask.setServerId(server.getId());
                serverTaskRepository.save(serverTask);

            } catch (Exception e) {
                log.warn("Failed to start server {}!", server.getName(), e);
                server.setStatus(ServerStatus.ERROR);
            }

            emitEvent = true;
        }

        return emitEvent ? Optional.of(toServerUpdatedEvent(server)) : Optional.empty();
    }

    private void manageOrphanedServerTask(ServerTask serverTask) {
        log.info("Server {} does not exist - need to stop server task {}", serverTask.getServerId(), serverTask.getName());
        hostingService.stopServerTask(serverTask);
    }

    private void stopAllServerTasks() {
        // get state of running containers
        HostingState hostingState = hostingService.poll();
        log.debug("Stopping all server tasks: hostingState={}", hostingState);
        Verify.verifyNotNull(hostingState, "hostingState is null after polling hosting service");

        for (ServerTask task : hostingState.getServerTasks()) {
            log.debug("Stopping task {}", task.getName());
            hostingService.stopServerTask(task);
        }
    }
}
