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
import adhoc.hosting.*;
import adhoc.properties.ManagerProperties;
import adhoc.system.event.Event;
import adhoc.task.kiosk.KioskTask;
import adhoc.task.manager.ManagerTask;
import adhoc.task.server.ServerTask;
import com.google.common.base.Verify;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Stream;

@Transactional
@Service
@Slf4j
@RequiredArgsConstructor
public class TaskManagerService {

    private final ManagerProperties managerProperties;

    private final TaskRepository taskRepository;

    private final HostingService hostingService;
    private final DnsService dnsService;

    @Retryable(retryFor = {TransientDataAccessException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 1000, maxDelay = 3000))
    public List<? extends Event> refreshTasks() {
        log.trace("Refreshing tasks...");
        List<Event> events = new ArrayList<>();

        // get state of running containers
        List<HostedTask> hostedTasks = hostingService.poll();
        log.debug("hostedTasks={}", hostedTasks);
        Verify.verifyNotNull(hostedTasks, "hostedTasks is null after polling hosting service!");

        List<String> taskIdentifiers = new ArrayList<>();

        for (HostedTask hostedTask : hostedTasks) {
            Task task = taskRepository.findByTaskIdentifier(hostedTask.getTaskIdentifier())
                    .orElseGet(() -> newTask(hostedTask));

            task = taskRepository.save(toEntity(hostedTask, task));

            taskIdentifiers.add(task.getTaskIdentifier());
        }

        taskRepository.deleteByTaskIdentifierNotIn(taskIdentifiers);

        return events;
    }

    private static Task newTask(HostedTask hostedTask) {
        if (hostedTask instanceof HostedManagerTask) {
            return new ManagerTask();
        } else if (hostedTask instanceof HostedKioskTask) {
            return new KioskTask();
        } else if (hostedTask instanceof HostedServerTask) {
            return new ServerTask();
        } else {
            throw new IllegalStateException("Unknown hosted task type: " + hostedTask.getClass());
        }
    }

    private Task toEntity(HostedTask hostedTask, Task task) {
        task.setTaskIdentifier(hostedTask.getTaskIdentifier());
        task.setName(hostedTask.getName());
        task.setPrivateIp(hostedTask.getPrivateIp());
        task.setPublicIp(hostedTask.getPublicIp());
        if (hostedTask instanceof HostedServerTask hostedServerTask) {
            ServerTask serverTask = (ServerTask) task;
            serverTask.setPublicWebSocketPort(hostedServerTask.getPublicWebSocketPort());
            serverTask.setServerId(hostedServerTask.getServerId());
        }
        return task;
    }

    @Retryable(retryFor = {TransientDataAccessException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 1000, maxDelay = 3000))
    public List<? extends Event> manageTaskDomains() {
        log.trace("Managing task domains...");
        List<Event> events = new ArrayList<>();

        Map<String, Set<String>> domainsIps = new LinkedHashMap<>();

        try (Stream<Task> tasks = taskRepository.streamBy()) {
            tasks.forEach(task -> {
                if (task.getDomain() == null && task.getPublicIp() != null) {

                    if (task instanceof ManagerTask managerTask) {
                        task.setDomain(managerProperties.getManagerDomain());

                    } else if (task instanceof KioskTask kioskTask) {
                        task.setDomain(managerProperties.getKioskDomain());

                    } else if (task instanceof ServerTask serverTask) {
                        task.setDomain(serverTask.getServerId() + "-" + managerProperties.getServerDomain());

                    } else {
                        throw new IllegalStateException("Unknown task type: " + task.getClass());
                    }

                    domainsIps.merge(task.getDomain(),
                            new LinkedHashSet<>(Collections.singleton(task.getPublicIp())),
                            (ips1, ips2) -> {
                                ips1.addAll(ips2);
                                return ips1;
                            });
                }
            });
        }

        for (Map.Entry<String, Set<String>> domainIps : domainsIps.entrySet()) {
            //log.info("{} -> {}", domainIps.getKey(), domainIps.getValue());
            dnsService.createOrUpdateDnsRecord(domainIps.getKey(), domainIps.getValue());
        }

        return events;
    }
}
