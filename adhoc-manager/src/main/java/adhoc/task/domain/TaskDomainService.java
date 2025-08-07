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

package adhoc.task.domain;

import adhoc.dns.DnsService;
import adhoc.message.MessageService;
import adhoc.system.event.Event;
import adhoc.system.properties.ManagerProperties;
import adhoc.task.KioskTask;
import adhoc.task.ManagerTask;
import adhoc.task.ServerTask;
import adhoc.task.Task;
import adhoc.task.TaskRepository;
import com.google.common.base.Verify;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.*;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class TaskDomainService {

    private final ManagerProperties managerProperties;

    private final TaskRepository taskRepository;

    private final DnsService dnsService;
    private final MessageService messageService;

    @Setter(onMethod_ = {@Autowired}, onParam_ = {@Lazy})
    private TaskDomainService self;

    public List<? extends Event> manageTaskDomains() {
        List<Event> events = new ArrayList<>();

        Map<Task, String> tasksDomains = new LinkedHashMap<>();
        MultiValueMap<Task, String> tasksPublicIps = new LinkedMultiValueMap<>();

        for (Task task : taskRepository.findAll()) {
            if (task.getDomain() == null && task.getPublicIp() != null) {

                String domain = determineDomain(task);

                tasksDomains.put(task, domain);
                tasksPublicIps.add(task, task.getPublicIp());
            }
        }

        for (Map.Entry<Task, String> taskDomain : tasksDomains.entrySet()) {
            Task task = taskDomain.getKey();
            String domain = taskDomain.getValue();
            List<String> publicIps = Verify.verifyNotNull(tasksPublicIps.get(task));

            //log.info("{} -> {}", domain, publicIps);
            dnsService.createOrUpdate(domain, new LinkedHashSet<>(publicIps));

            self.updateTaskDomainInNewTransaction(task.getId(), domain);
        }

        return events;
    }

    // NOTE: done in new transaction to avoid retries spamming DNS service due to optimistic locking
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(retryFor = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 100, maxDelay = 1000))
    public void updateTaskDomainInNewTransaction(Long taskId, String domain) {
        Task task = taskRepository.getReferenceById(taskId);

        if (!Objects.equals(task.getDomain(), domain)) {
            task.setDomain(domain);
        }

        messageService.addGlobalMessage(String.format("Task %d (of type %s) mapped to domain %s", task.getId(), task.getTaskType().name(), domain));
    }

    private String determineDomain(Task task) {
        return switch (task) {
            case ManagerTask managerTask -> managerProperties.getManagerDomain();
            case KioskTask kioskTask -> managerProperties.getKioskDomain();
            case ServerTask serverTask -> serverTask.getServerId() + "-" + managerProperties.getServerDomain();
            default -> throw new IllegalStateException("Unknown task type: " + task.getClass());
        };
    }
}
