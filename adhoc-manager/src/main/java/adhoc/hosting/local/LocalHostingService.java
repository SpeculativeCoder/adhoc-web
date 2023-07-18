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

package adhoc.hosting.local;

import adhoc.hosting.HostingService;
import adhoc.hosting.HostingState;
import adhoc.server.Server;
import adhoc.server.ServerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.artemis.utils.collections.ConcurrentHashSet;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.*;

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
    public HostingState poll() {
        HostingState state = new HostingState();
        state.setManagerHosts(Collections.singleton("localhost"));
        state.setKioskHosts(Collections.singleton("localhost"));

        Map<Long, HostingState.ServerTask> tasks = new LinkedHashMap<>();
        state.setServerTasks(tasks);

        List<Server> servers = serverRepository.findAll();

        for (Server server : servers) {
            if (serverIds.contains(server.getId())) {

                HostingState.ServerTask task = new HostingState.ServerTask();

                task.setTaskId("local-task-" + server.getId());
                //task.setServerId(server.getId());
                //task.setManagerHost("127.0.0.1");
                task.setPrivateIp("127.0.0.1");
                task.setPublicIp("127.0.0.1");
                task.setPublicWebSocketPort(8889);

                tasks.put(server.getId(), task);
            }
        }

        return state;
    }

    @Override
    public void startServerTask(Server server) { //, Set<String> managerHosts) {
        log.info("Assuming locally running Unreal server is server " + server.getId());
        serverIds.add(server.getId());
    }

    @Override
    public void stopServerTask(HostingState.ServerTask task) {
        log.warn("Ignoring request to stop task {}", task);
    }

}
