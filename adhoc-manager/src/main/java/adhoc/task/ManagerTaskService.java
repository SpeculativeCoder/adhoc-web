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
import adhoc.world.ManagerWorldService;
import adhoc.world.event.WorldUpdatedEvent;
import com.google.common.base.Verify;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Transactional
@Service
@Slf4j
@RequiredArgsConstructor
public class ManagerTaskService {

    private final ManagerTaskRepository managerTaskRepository;
    private final KioskTaskRepository kioskTaskRepository;
    private final ServerTaskRepository serverTaskRepository;

    private final ManagerWorldService managerWorldService;
    private final HostingService hostingService;

    public List<? extends Event> refreshTasks() {
        log.trace("Refreshing tasks...");
        List<Event> events = new ArrayList<>();

        // get state of running containers
        HostingState hostingState = hostingService.poll();
        log.debug("refreshTasks: hostingState={}", hostingState);
        Verify.verifyNotNull(hostingState, "hostingState is null after polling hosting service");

        // TODO: WIP
        List<String> managerTaskIdentifiers = new ArrayList<>();
        for (ManagerTask task : hostingState.getManagerTasks()) {
            ManagerTask managerTask = managerTaskRepository.findByTaskIdentifier(task.getTaskIdentifier()).orElseGet(ManagerTask::new);
            managerTask.setTaskIdentifier(task.getTaskIdentifier());
            managerTask.setName(task.getName());
            managerTask.setPrivateIp(task.getPrivateIp());
            managerTask.setPublicIp(task.getPublicIp());
            //managerTask.setPublicWebSocketPort(task.getPublicWebSocketPort());
            //managerTask.setManagerId(task.getManagerId());
            managerTaskRepository.save(managerTask);
            managerTaskIdentifiers.add(managerTask.getTaskIdentifier());
        }
        managerTaskRepository.deleteByTaskIdentifierNotIn(managerTaskIdentifiers);

        // TODO: WIP
        List<String> kioskTaskIdentifiers = new ArrayList<>();
        for (KioskTask task : hostingState.getKioskTasks()) {
            KioskTask kioskTask = kioskTaskRepository.findByTaskIdentifier(task.getTaskIdentifier()).orElseGet(KioskTask::new);
            kioskTask.setTaskIdentifier(task.getTaskIdentifier());
            kioskTask.setName(task.getName());
            kioskTask.setPrivateIp(task.getPrivateIp());
            kioskTask.setPublicIp(task.getPublicIp());
            //kioskTask.setPublicWebSocketPort(task.getPublicWebSocketPort());
            //kioskTask.setKioskId(task.getKioskId());
            kioskTaskRepository.save(kioskTask);
            kioskTaskIdentifiers.add(kioskTask.getTaskIdentifier());
        }
        kioskTaskRepository.deleteByTaskIdentifierNotIn(kioskTaskIdentifiers);

        // TODO: WIP
        List<String> serverTaskIdentifiers = new ArrayList<>();
        for (ServerTask task : hostingState.getServerTasks()) {
            ServerTask serverTask = serverTaskRepository.findByTaskIdentifier(task.getTaskIdentifier()).orElseGet(ServerTask::new);
            serverTask.setTaskIdentifier(task.getTaskIdentifier());
            serverTask.setName(task.getName());
            serverTask.setPrivateIp(task.getPrivateIp());
            serverTask.setPublicIp(task.getPublicIp());
            serverTask.setPublicWebSocketPort(task.getPublicWebSocketPort());
            serverTask.setServerId(task.getServerId());
            serverTaskRepository.save(serverTask);
            serverTaskIdentifiers.add(serverTask.getTaskIdentifier());
        }
        serverTaskRepository.deleteByTaskIdentifierNotIn(serverTaskIdentifiers);

        // TODO
        Optional<WorldUpdatedEvent> optionalWorldUpdatedEvent =
                managerWorldService.updateManagerAndKioskHosts(
                        hostingState.getManagerTasks().stream().map(ManagerTask::getPublicIp).collect(Collectors.toSet()),
                        hostingState.getKioskTasks().stream().map(KioskTask::getPublicIp).collect(Collectors.toSet()));

        optionalWorldUpdatedEvent.ifPresent(events::add);

        return events;
    }
}
