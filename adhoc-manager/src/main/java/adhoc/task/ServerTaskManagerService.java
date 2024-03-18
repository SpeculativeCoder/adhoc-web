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

import adhoc.hosting.HostedServerTask;
import adhoc.hosting.HostedTask;
import adhoc.hosting.HostingService;
import adhoc.server.Server;
import adhoc.server.ServerManagerService;
import adhoc.server.ServerRepository;
import adhoc.server.ServerStatus;
import adhoc.server.event.ServerUpdatedEvent;
import adhoc.system.event.Event;
import com.google.common.base.Verify;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final ServerRepository serverRepository;
    private final ServerTaskRepository serverTaskRepository;

    private final ServerManagerService serverManagerService;
    private final HostingService hostingService;

    /**
     * For each server, manage a server task in the hosting service,
     * creating new server tasks and/or tearing down old server tasks as required by the current servers.
     */
    public List<? extends Event> manageServerTasks() {
        log.trace("Managing server tasks...");
        List<Event> events = new ArrayList<>();

        List<String> taskIdentifiers = new ArrayList<>();

        try (Stream<Server> servers = serverRepository.streamBy()) {
            servers.forEach(server -> {
                Optional<ServerTask> optionalServerTask = serverTaskRepository.findByServerId(server.getId());

                manageServerTask(server, optionalServerTask).ifPresent(events::add);

                optionalServerTask.map(Task::getTaskIdentifier).ifPresent(taskIdentifiers::add);
            });
        }

        // any tasks for servers which don't exist should be stopped (typically this is cleanup from a previous run)
        try (Stream<ServerTask> orphanedServerTasks = serverTaskRepository.streamByTaskIdentifierNotIn(taskIdentifiers)) {
            orphanedServerTasks.forEach(orphanedServerTask -> {
                try {
                    log.info("Stopping orphaned server task {}", orphanedServerTask.getTaskIdentifier());
                    hostingService.stopServerTask(orphanedServerTask.getTaskIdentifier());
                } catch (Exception e) {
                    log.warn("Failed to stop server task {}!", orphanedServerTask.getTaskIdentifier(), e);
                }
            });
        }

        return events;
    }

    private Optional<ServerUpdatedEvent> manageServerTask(Server server, Optional<ServerTask> optionalServerTask) {
        Optional<ServerUpdatedEvent> optionalEvent = Optional.empty();

        // state transitions
        switch (server.getStatus()) {

        case INACTIVE:
            if (optionalServerTask.isEmpty() && !server.getAreas().isEmpty()) {
                log.info("Server {} has assigned areas - need to start server task", server.getId());
                optionalEvent = serverManagerService.updateServerStateInNewTransaction(server.getId(), ServerStatus.STARTING);
                try {
                    hostingService.startServerTask(server);
                } catch (Exception e) {
                    log.warn("Failed to start server task for server {}!", server.getId(), e);
                    optionalEvent = serverManagerService.updateServerStateInNewTransaction(server.getId(), ServerStatus.ERROR);
                }
            }
            break;

        case ServerStatus.STARTING:
            if (optionalServerTask.isPresent()) {
                log.info("Server task {} for server {} has started successfully", optionalServerTask.get().getTaskIdentifier(), server.getId());
                optionalEvent = serverManagerService.updateServerStateInNewTransaction(server.getId(), ServerStatus.ACTIVE);

            } else if (server.getInitiated().plusMinutes(5).isBefore(LocalDateTime.now())) {
                log.warn("Server task for server {} failed or took too long to start!", server.getId());
                optionalEvent = serverManagerService.updateServerStateInNewTransaction(server.getId(), ServerStatus.INACTIVE);
            }
            break;

        case ServerStatus.STOPPING:
            if (optionalServerTask.isEmpty()) {
                log.info("Server task server {} has stopped successfully", server.getId());
                optionalEvent = serverManagerService.updateServerStateInNewTransaction(server.getId(), ServerStatus.INACTIVE);
            }
            break;

        case ServerStatus.ACTIVE:
            if (optionalServerTask.isPresent() && server.getAreas().isEmpty()) {
                log.info("Server {} has no assigned areas - need to stop server task {}", server.getId(), optionalServerTask.get().getTaskIdentifier());
                optionalEvent = serverManagerService.updateServerStateInNewTransaction(server.getId(), ServerStatus.STOPPING);
                try {
                    hostingService.stopServerTask(optionalServerTask.get().getTaskIdentifier());
                } catch (Exception e) {
                    log.warn("Failed to stop server task {} for server {}!", optionalServerTask.get().getTaskIdentifier(), server.getId(), e);
                    optionalEvent = serverManagerService.updateServerStateInNewTransaction(server.getId(), ServerStatus.ERROR);
                }

            } else if (optionalServerTask.isEmpty()) {
                log.info("Server task for server {} has stopped unexpectedly!", server.getId());
                optionalEvent = serverManagerService.updateServerStateInNewTransaction(server.getId(), ServerStatus.INACTIVE);
            }
            break;
        }

        return optionalEvent;
    }

    private void stopAllServerTasks() {
        // get state of running containers
        List<HostedTask> hostedTasks = hostingService.poll();
        log.debug("Stopping all server tasks: hostedTasks={}", hostedTasks);
        Verify.verifyNotNull(hostedTasks, "hostedTasks is null after polling hosting service");

        hostedTasks.stream()
                .filter(task -> task instanceof HostedServerTask)
                .forEach(hostedServerTask -> {
                    log.info("Stopping server task {}", hostedServerTask.getTaskIdentifier());
                    hostingService.stopServerTask(hostedServerTask.getTaskIdentifier());
                });
    }
}
