/*
 * Copyright (c) 2022-2025 SpeculativeCoder (https://github.com/SpeculativeCoder)
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

import adhoc.hosting.HostingService;
import adhoc.server.ServerEntity;
import adhoc.server.ServerRepository;
import com.google.common.base.Verify;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class ServerTaskAllocateService {

    private final ServerRepository serverRepository;
    private final ServerTaskRepository serverTaskRepository;

    private final ServerTaskManagerService serverTaskManagerService;

    private final HostingService hostingService;

    /**
     * For each enabled server, ensure there is a server task in the hosting service. Stop any other server tasks.
     */
    public void allocateServerTasks() {
        List<String> taskIdentifiers = new ArrayList<>();

        try (Stream<ServerEntity> servers = serverRepository.streamByEnabledTrue()) {
            servers.forEach(server -> {
                ServerTaskEntity serverTask = serverTaskRepository.findFirstByServerId(server.getId())
                        // no existing server task? start a new one
                        .orElseGet(() -> {
                            ServerTaskEntity serverHostingTask = startHostedServerTask(server);
                            serverTaskManagerService.createServerTaskInNewTransaction(serverHostingTask);
                            return serverHostingTask;
                        });

                taskIdentifiers.add(serverTask.getTaskIdentifier());
            });
        }

        LocalDateTime initiatedBefore = LocalDateTime.now().minusMinutes(1);
        // any other server tasks that are no longer in use by a server should be stopped and their entry deleted
        try (Stream<ServerTaskEntity> unusedServerTasks = serverTaskRepository.streamByTaskIdentifierNotInAndInitiatedBefore(taskIdentifiers, initiatedBefore)) {
            unusedServerTasks.forEach(unusedServerTask -> {
                if (unusedServerTask.getSeen() != null) {
                    stopHostedServerTask(unusedServerTask);
                }
                serverTaskManagerService.deleteServerTaskInNewTransaction(unusedServerTask.getId());
            });
        }
    }

    private ServerTaskEntity startHostedServerTask(ServerEntity server) {
        try {
            log.debug("Starting server task for server {}", server.getId());
            ServerTaskEntity serverTask = hostingService.startServerTask(server);

            Verify.verifyNotNull(serverTask.getTaskIdentifier());
            Verify.verifyNotNull(serverTask.getServerId());

            return serverTask;

        } catch (Exception e) {
            log.warn("Failed to start server task for server {}!", server.getId(), e);
            throw e;
        }
    }

    private void stopHostedServerTask(ServerTaskEntity serverTask) {
        try {
            log.debug("Stopping server task for server {}", serverTask.getServerId());
            hostingService.stopServerTask(serverTask.getTaskIdentifier());

        } catch (Exception e) {
            log.warn("Failed to stop server task for server {}!", serverTask.getServerId(), e);
            throw e;
        }
    }
}
