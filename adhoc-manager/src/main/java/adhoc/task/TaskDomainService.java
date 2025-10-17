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

import adhoc.Event;
import adhoc.dns.DnsService;
import adhoc.system.properties.ManagerProperties;
import com.google.common.base.Verify;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class TaskDomainService {

    private final ManagerProperties managerProperties;

    private final TaskRepository taskRepository;

    private final TaskManagerService taskManagerService;

    private final DnsService dnsService;

    public List<? extends Event> manageTaskDomains() {
        List<Event> events = new ArrayList<>();

        Map<TaskEntity, String> tasksDomains = new LinkedHashMap<>();
        MultiValueMap<TaskEntity, String> tasksPublicIps = new LinkedMultiValueMap<>();

        for (TaskEntity task : taskRepository.findAll()) {
            if (task.getDomain() == null && task.getPublicIp() != null) {

                String domain = determineDomain(task);

                tasksDomains.put(task, domain);
                tasksPublicIps.add(task, task.getPublicIp());
            }
        }

        for (Map.Entry<TaskEntity, String> taskDomain : tasksDomains.entrySet()) {
            TaskEntity task = taskDomain.getKey();
            String domain = taskDomain.getValue();
            List<String> publicIps = Verify.verifyNotNull(tasksPublicIps.get(task));

            //log.info("{} -> {}", domain, publicIps);
            dnsService.createOrUpdate(domain, new LinkedHashSet<>(publicIps));

            taskManagerService.updateTaskDomainInNewTransaction(task.getId(), domain);
        }

        return events;
    }

    private String determineDomain(TaskEntity task) {
        return switch (task) {
            case ManagerTaskEntity managerTask -> managerProperties.getManagerDomain();
            case KioskTaskEntity kioskTask -> managerProperties.getKioskDomain();
            case ServerTaskEntity serverTask -> serverTask.getServerId() + "-" + managerProperties.getServerDomain();
            default -> throw new IllegalStateException("Unknown task type: " + task.getClass());
        };
    }
}
