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

package adhoc.hosting.local;

import adhoc.hosting.*;
import adhoc.hosting.task.HostingTask;
import adhoc.hosting.task.KioskHostingTask;
import adhoc.hosting.task.ManagerHostingTask;
import adhoc.hosting.task.ServerHostingTask;
import adhoc.server.Server;
import adhoc.server.ServerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.artemis.utils.collections.ConcurrentHashSet;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Treats whatever server is running on 127.0.0.1 as the task for ALL servers.
 * Useful for testing i.e. if you are running the server locally in the editor.
 */
@Service
@Profile("hosting-local")
@Slf4j
@RequiredArgsConstructor
public class LocalHostingService implements HostingService {

    private final ServerRepository serverRepository;

    private final Set<Long> serverIds = new ConcurrentHashSet<>();

    @Override
    public List<HostingTask> poll() {

        List<HostingTask> tasks = new ArrayList<>();

        ManagerHostingTask managerTask = new ManagerHostingTask();
        managerTask.setTaskIdentifier("local manager task");
        managerTask.setPrivateIp("127.0.0.1");
        managerTask.setPublicIp("127.0.0.1");
        tasks.add(managerTask);

        KioskHostingTask kioskTask = new KioskHostingTask();
        kioskTask.setTaskIdentifier("local kiosk task");
        kioskTask.setPrivateIp("127.0.0.1");
        kioskTask.setPublicIp("127.0.0.1");
        tasks.add(kioskTask);

        List<Server> servers = serverRepository.findAll();

        for (Server server : servers) {
            if (serverIds.contains(server.getId())) {

                ServerHostingTask serverTask = new ServerHostingTask();
                serverTask.setTaskIdentifier(server.getId().toString());
                serverTask.setPrivateIp("127.0.0.1");
                serverTask.setPublicIp("127.0.0.1");
                serverTask.setPublicWebSocketPort(8889);
                serverTask.setServerId(server.getId());

                tasks.add(serverTask);
            }
        }

        return tasks;
    }

    @Override
    public ServerHostingTask startServerTask(Server server) {
        log.debug("Assuming local task for server {}", server);
        serverIds.add(server.getId());

        ServerHostingTask serverTask = new ServerHostingTask();
        serverTask.setTaskIdentifier(server.getId().toString());
        serverTask.setPublicWebSocketPort(8889);
        serverTask.setServerId(server.getId());

        return serverTask;
    }

    @Override
    public void stopServerTask(String taskIdentifier) {
        Long serverId = Long.valueOf(taskIdentifier);

        if (!serverIds.contains(serverId)) {
            log.warn("Tried to stop non existing assumed local server task {}", taskIdentifier);
            return;
        }

        log.debug("No longer assuming local server task {}", taskIdentifier);
        serverIds.remove(serverId);
    }
}
