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

import adhoc.hosting.HostingService;
import adhoc.hosting.HostingState;
import adhoc.system.event.Event;
import adhoc.task.kiosk.KioskTask;
import adhoc.task.kiosk.KioskTaskRepository;
import adhoc.task.manager.ManagerTask;
import adhoc.task.manager.ManagerTaskRepository;
import adhoc.task.server.ServerTask;
import adhoc.task.server.ServerTaskRepository;
import adhoc.world.WorldManagerService;
import com.google.common.base.Verify;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Transactional
@Service
@Slf4j
@RequiredArgsConstructor
public class TaskManagerService {

    private final ManagerTaskRepository managerTaskRepository;
    private final KioskTaskRepository kioskTaskRepository;
    private final ServerTaskRepository serverTaskRepository;

    private final WorldManagerService worldManagerService;
    private final HostingService hostingService;

    public List<? extends Event> refreshTasks() {
        log.trace("Refreshing tasks...");
        List<Event> events = new ArrayList<>();

        // get state of running containers
        HostingState hostingState = hostingService.poll();
        log.debug("hostingState={}", hostingState);
        Verify.verifyNotNull(hostingState, "hostingState is null after polling hosting service!");

        List<String> managerTaskIdentifiers = new ArrayList<>();
        for (ManagerTask hostingManagerTask : hostingState.getManagerTasks()) {
            ManagerTask managerTask = managerTaskRepository.findByTaskIdentifier(hostingManagerTask.getTaskIdentifier()).orElseGet(ManagerTask::new);

            assignCommonFields(managerTask, hostingManagerTask);

            managerTaskRepository.save(managerTask);

            managerTaskIdentifiers.add(managerTask.getTaskIdentifier());
        }

        managerTaskRepository.deleteByTaskIdentifierNotIn(managerTaskIdentifiers);

        List<String> kioskTaskIdentifiers = new ArrayList<>();
        for (KioskTask hostingKioskTask : hostingState.getKioskTasks()) {
            KioskTask kioskTask = kioskTaskRepository.findByTaskIdentifier(hostingKioskTask.getTaskIdentifier()).orElseGet(KioskTask::new);

            assignCommonFields(kioskTask, hostingKioskTask);

            kioskTaskRepository.save(kioskTask);

            kioskTaskIdentifiers.add(kioskTask.getTaskIdentifier());
        }

        kioskTaskRepository.deleteByTaskIdentifierNotIn(kioskTaskIdentifiers);

        List<String> serverTaskIdentifiers = new ArrayList<>();
        for (ServerTask serverHostingTask : hostingState.getServerTasks()) {
            ServerTask serverTask = serverTaskRepository.findByTaskIdentifier(serverHostingTask.getTaskIdentifier()).orElseGet(ServerTask::new);

            assignCommonFields(serverTask, serverHostingTask);
            assignServerSpecificFields(serverTask, serverHostingTask);

            serverTaskRepository.save(serverTask);

            serverTaskIdentifiers.add(serverTask.getTaskIdentifier());
        }

        serverTaskRepository.deleteByTaskIdentifierNotIn(serverTaskIdentifiers);

        return events;
    }

    private static void assignCommonFields(Task task, Task hostingTask) {
        task.setTaskIdentifier(hostingTask.getTaskIdentifier());
        task.setName(hostingTask.getName());
        task.setPrivateIp(hostingTask.getPrivateIp());
        task.setPublicIp(hostingTask.getPublicIp());
    }

    private static void assignServerSpecificFields(ServerTask serverTask, ServerTask serverHostingTask) {
        serverTask.setPublicWebSocketPort(serverHostingTask.getPublicWebSocketPort());
        serverTask.setServerId(serverHostingTask.getServerId());
    }
}
