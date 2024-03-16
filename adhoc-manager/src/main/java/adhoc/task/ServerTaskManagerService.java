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

import adhoc.dns.DnsService;
import adhoc.hosting.HostedServerTask;
import adhoc.hosting.HostedTask;
import adhoc.hosting.HostingService;
import adhoc.properties.ManagerProperties;
import adhoc.server.Server;
import adhoc.server.ServerManagerService;
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
import java.util.Optional;
import java.util.stream.Stream;

@Transactional
@Service
@Slf4j
@RequiredArgsConstructor
public class ServerTaskManagerService {

    private final ManagerProperties managerProperties;
    private final ServerProperties serverProperties;

    private final ServerRepository serverRepository;
    private final ServerTaskRepository serverTaskRepository;

    private final ServerManagerService serverManagerService;
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
            servers.forEach(server ->
                    serverTaskRepository.findByServerId(server.getId())
                            .map(existingServerTask ->
                                    manageExistingServerTask(existingServerTask, server))
                            .orElseGet(() ->
                                    manageMissingServerTask(server))
                            .ifPresent(events::add));
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

        Optional<ServerUpdatedEvent> optionalEvent = Optional.empty();

        // state transition(s)

        if (server.getStatus() == ServerStatus.STARTING) {
            log.info("Server {} task has started successfully", server.getName());
            optionalEvent = serverManagerService.updateServerStateInNewTransaction(server.getId(), ServerStatus.ACTIVE);


        } else if (server.getStatus() == ServerStatus.ACTIVE) {
            if (server.getAreas().isEmpty()) {
                log.info("Server {} has no assigned areas - need to stop task {}", server.getName(), task.getName());
                optionalEvent = serverManagerService.updateServerStateInNewTransaction(server.getId(), ServerStatus.STOPPING);
                try {
                    hostingService.stopServerTask(task.getTaskIdentifier());
                } catch (Exception e) {
                    log.warn("Failed to stop server {}!", server.getName(), e);
                    optionalEvent = serverManagerService.updateServerStateInNewTransaction(server.getId(), ServerStatus.ERROR);
                }
            }
        }

        return optionalEvent;
    }

    private Optional<ServerUpdatedEvent> manageMissingServerTask(Server server) {
        log.trace("Managing missing server task for server {}", server.getName());

        if (server.getStatus() == ServerStatus.ERROR) {
            return Optional.empty();
        }

        Optional<ServerUpdatedEvent> optionalEvent = Optional.empty();

        // state transition(s)

        if (server.getStatus() == ServerStatus.INACTIVE) {
            if (!server.getAreas().isEmpty()) {
                log.info("Server {} has assigned areas - need to start task", server.getName());
                optionalEvent = serverManagerService.updateServerStateInNewTransaction(server.getId(), ServerStatus.STARTING);
                try {
                    hostingService.startServerTask(server);
                } catch (Exception e) {
                    log.warn("Failed to start server {}!", server.getName(), e);
                    optionalEvent = serverManagerService.updateServerStateInNewTransaction(server.getId(), ServerStatus.ERROR);
                }
            }

        } else if (server.getStatus() == ServerStatus.STARTING) {
            if (server.getInitiated().plusMinutes(5).isBefore(LocalDateTime.now())) {
                log.warn("Server task for {} failed or took too long to start!", server.getName());
                optionalEvent = serverManagerService.updateServerStateInNewTransaction(server.getId(), ServerStatus.INACTIVE);
            }

        } else if (server.getStatus() == ServerStatus.STOPPING) {
            log.info("Server {} task has stopped successfully", server.getName());
            optionalEvent = serverManagerService.updateServerStateInNewTransaction(server.getId(), ServerStatus.INACTIVE);

        } else if (server.getStatus() == ServerStatus.ACTIVE) {
            log.info("Server {} task has stopped unexpectedly!", server.getName());
            optionalEvent = serverManagerService.updateServerStateInNewTransaction(server.getId(), ServerStatus.INACTIVE);
        }

        return optionalEvent;
    }

    private void manageOrphanedServerTask(ServerTask serverTask) {
        log.info("Server {} does not exist - need to stop server task {}", serverTask.getServerId(), serverTask.getName());
        hostingService.stopServerTask(serverTask.getTaskIdentifier());
    }

    private void stopAllServerTasks() {
        // get state of running containers
        List<HostedTask> hostedTasks = hostingService.poll();
        log.debug("Stopping all server tasks: hostedTasks={}", hostedTasks);
        Verify.verifyNotNull(hostedTasks, "hostedTasks is null after polling hosting service");

        hostedTasks.stream()
                .filter(task -> task instanceof HostedServerTask)
                .forEach(hostedServerTask -> {
                    log.info("Stopping task {}", hostedServerTask.getTaskIdentifier());
                    hostingService.stopServerTask(hostedServerTask.getTaskIdentifier());
                });
    }
}
