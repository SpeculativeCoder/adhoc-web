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
import adhoc.hosting.HostingService;
import adhoc.server.Server;
import adhoc.server.ServerManagerService;
import adhoc.server.ServerRepository;
import adhoc.server.ServerStatus;
import adhoc.system.event.Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
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
    private final ManagerTaskRepository managerTaskRepository;

    private final ServerManagerService serverManagerService;

    private final HostingService hostingService;

    /**
     * For each enabled server, ensure there is a server task in the hosting service. Stop any other server tasks.
     */
    public List<? extends Event> manageServerTasks() {
        log.trace("Managing server tasks...");
        List<Event> events = new ArrayList<>();

        List<String> usedTaskIdentifiers = new ArrayList<>();

        // manager task must be seen before any servers tasks are started
        if (!managerTaskRepository.existsBy()) {
            return Collections.emptyList();
        }

        try (Stream<Server> servers = serverRepository.streamByEnabledTrue()) {
            servers.forEach(server -> {
                Optional<ServerTask> optionalServerTask = serverTaskRepository.findFirstByServerId(server.getId());

                if (optionalServerTask.isPresent()) {
                    optionalServerTask
                            .map(ServerTask::getTaskIdentifier)
                            .ifPresent(usedTaskIdentifiers::add);

                } else if (server.getInitiated() == null || server.getInitiated().isBefore(LocalDateTime.now().minusMinutes(1))) {

                    startServerTask(server);

                    serverManagerService.updateServerStateInNewTransaction(server.getId(), ServerStatus.STARTING)
                            .ifPresent(events::add);
                }
            });
        }

        // any other server tasks which are not in use should be stopped
        try (Stream<ServerTask> unusedServerTasks = serverTaskRepository.streamByTaskIdentifierNotIn(usedTaskIdentifiers)) {
            unusedServerTasks.forEach(unusedServerTask -> {

                Optional<Server> optionalServer = serverRepository.findById(unusedServerTask.getServerId());

                // TODO: timestamp check
                if (optionalServer.isEmpty() || optionalServer.get().getStatus() != ServerStatus.INACTIVE) {
                    stopServerTask(unusedServerTask);

                    optionalServer.flatMap(server ->
                                    serverManagerService.updateServerStateInNewTransaction(server.getId(), ServerStatus.INACTIVE))
                            .ifPresent(events::add);
                }
            });
        }

        return events;
    }

    private void startServerTask(Server server) {
        try {
            log.info("Starting server task for server {}", server.getId());
            HostedServerTask hostedServerTask = hostingService.startServerTask(server);

        } catch (Exception e) {
            log.warn("Failed to start server task for server {}!", server.getId(), e);
        }
    }

    private void stopServerTask(ServerTask serverTask) {
        try {
            log.info("Stopping server task for server task {}", serverTask.getServerId());
            hostingService.stopServerTask(serverTask.getTaskIdentifier());

        } catch (Exception e) {
            log.warn("Failed to stop server task for server task {}!", serverTask.getServerId(), e);
        }
    }
}
