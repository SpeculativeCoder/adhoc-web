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
import adhoc.server.ServerDto;
import adhoc.server.ServerManagerService;
import com.google.common.base.Verify;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ServerTaskManagerOrchestrator {

    private final ServerTaskManagerService serverTaskManagerService;
    private final ServerManagerService serverManagerService;

    private final HostingService hostingService;

    /**
     * For each enabled server, ensure there is a server task in the hosting service. Stop any other server tasks.
     */
    public void manageServerTasks() {
        List<ServerDto> enabledTasklessServers = serverManagerService.findEnabledTasklessServers();

        for (ServerDto enabledTasklessServer : enabledTasklessServers) {
            TaskDto serverTask = startHostedServerTask(enabledTasklessServer);
            serverTaskManagerService.createServerTask(serverTask);
        }

        List<String> seenUnusedServerTasks = serverTaskManagerService.findSeenUnusedServerTasks();
        for (String seenUnusedServerTask : seenUnusedServerTasks) {
            stopHostedServerTask(seenUnusedServerTask);
            serverTaskManagerService.deleteServerTask(seenUnusedServerTask);
        }
    }

    private TaskDto startHostedServerTask(ServerDto server) {
        try {
            log.debug("Starting server task for server {}", server.getId());
            TaskDto serverTask = hostingService.startServerTask(server);

            Verify.verifyNotNull(serverTask.getTaskIdentifier());
            Verify.verifyNotNull(serverTask.getServerId());

            return serverTask;

        } catch (Exception e) {
            log.warn("Failed to start server task for server {}!", server.getId(), e);
            throw e;
        }
    }

    private void stopHostedServerTask(String serverTaskIdentifier) {
        try {
            log.debug("Stopping server task for server with identifier {}", serverTaskIdentifier);
            hostingService.stopServerTask(serverTaskIdentifier);

        } catch (Exception e) {
            log.warn("Failed to stop server task for server with identifier {}!", serverTaskIdentifier, e);
            throw e;
        }
    }
}
