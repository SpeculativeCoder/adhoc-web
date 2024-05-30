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
import adhoc.message.MessageService;
import adhoc.server.ManagerServerService;
import adhoc.server.Server;
import adhoc.server.ServerRepository;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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
public class ManagerServerTaskService {

    private final ServerRepository serverRepository;
    private final ServerTaskRepository serverTaskRepository;
    private final ManagerTaskRepository managerTaskRepository;
    private final MessageService messageService;

    private final ManagerServerService managerServerService;

    private final HostingService hostingService;

    @Setter(onMethod_ = {@Autowired}, onParam_ = {@Lazy})
    private ManagerServerTaskService self;

    /**
     * For each enabled server, ensure there is a server task in the hosting service. Stop any other server tasks.
     */
    public void manageServerTasks() {
        log.trace("Managing server tasks...");
        List<String> taskIdentifiers = new ArrayList<>();

        try (Stream<Server> servers = serverRepository.streamByEnabledTrue()) {
            servers.forEach(server -> {
                Optional<ServerTask> optionalServerTask = serverTaskRepository.findFirstByServerId(server.getId());

                if (optionalServerTask.isPresent()) {
                    taskIdentifiers.add(optionalServerTask.get().getTaskIdentifier());

                } else {
                    HostedServerTask hostedServerTask = startHostedServerTask(server);
                    taskIdentifiers.add(hostedServerTask.getTaskIdentifier());

                    self.createServerTaskInNewTransaction(server, hostedServerTask);
                }
            });
        }

        // any other server tasks that are no longer in use should be stopped and their entry deleted
        try (Stream<ServerTask> unusedServerTasks = serverTaskRepository.streamByTaskIdentifierNotInAndInitiatedBefore(taskIdentifiers, LocalDateTime.now().minusMinutes(1))) {
            unusedServerTasks.forEach(unusedServerTask -> {
                if (unusedServerTask.getSeen() != null) {
                    stopHostedServerTask(unusedServerTask);
                }

                self.deleteServerTaskInNewTransaction(unusedServerTask.getId());
            });
        }
    }

    private HostedServerTask startHostedServerTask(Server server) {
        try {
            log.debug("Starting server task for server {}", server.getId());
            return hostingService.startServerTask(server);

        } catch (Exception e) {
            log.warn("Failed to start server task for server {}!", server.getId(), e);
            throw e;
        }
    }

    private void stopHostedServerTask(ServerTask serverTask) {
        try {
            log.debug("Stopping server task for server task {}", serverTask.getServerId());
            hostingService.stopServerTask(serverTask.getTaskIdentifier());

        } catch (Exception e) {
            log.warn("Failed to stop server task for server task {}!", serverTask.getServerId(), e);
            throw e;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(retryFor = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 100, maxDelay = 1000))
    void createServerTaskInNewTransaction(Server server, HostedServerTask hostedServerTask) {
        ServerTask serverTask = new ServerTask();

        serverTask.setTaskIdentifier(hostedServerTask.getTaskIdentifier());
        serverTask.setInitiated(LocalDateTime.now()); // TODO
        serverTask.setServerId(server.getId());

        serverTaskRepository.save(serverTask);

        messageService.addGlobalMessage(String.format("Server task %d created", server.getId()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(retryFor = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 100, maxDelay = 1000))
    void deleteServerTaskInNewTransaction(Long serverTaskId) {
        serverTaskRepository.deleteById(serverTaskId);

        messageService.addGlobalMessage(String.format("Server task %d deleted", serverTaskId));
    }
}
