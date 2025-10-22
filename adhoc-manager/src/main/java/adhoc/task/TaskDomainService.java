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

import adhoc.message.MessageService;
import adhoc.system.properties.ManagerProperties;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class TaskDomainService {

    private final ManagerProperties managerProperties;

    private final TaskRepository taskRepository;

    private final MessageService messageService;

    public record TaskDomain(
            Long taskId,
            String domain,
            List<String> publicIps
    ) {
    }

    @Retryable(retryFor = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 100, maxDelay = 1000))
    public List<TaskDomain> determineTaskDomains() {
        Map<Long, TaskDomain> taskDomains = new LinkedHashMap<>();

        for (TaskEntity task : taskRepository.findAll()) {
            if (task.getDomain() == null && task.getPublicIp() != null) {

                String domain = determineDomain(task);

                TaskDomain taskDomain = taskDomains.get(task.getId());
                if (taskDomain == null) {
                    taskDomains.put(task.getId(), new TaskDomain(task.getId(), domain, Lists.newArrayList(task.getPublicIp())));
                } else {
                    taskDomain.publicIps().add(task.getPublicIp());
                }
            }
        }

        return new ArrayList<>(taskDomains.values());
    }

    private String determineDomain(TaskEntity task) {
        return switch (task) {
            case ManagerTaskEntity managerTask -> managerProperties.getManagerDomain();
            case KioskTaskEntity kioskTask -> managerProperties.getKioskDomain();
            case ServerTaskEntity serverTask -> serverTask.getServerId() + "-" + managerProperties.getServerDomain();
            default -> throw new IllegalStateException("Unknown task type: " + task.getClass());
        };
    }

    @Retryable(retryFor = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 100, maxDelay = 1000))
    public void updateTaskDomain(Long taskId, String domain) {

        TaskEntity task = taskRepository.getReferenceById(taskId);

        if (!Objects.equals(task.getDomain(), domain)) {
            task.setDomain(domain);
        }

        messageService.addGlobalMessage(String.format("Task %d (of type %s) mapped to domain %s", task.getId(), task.getType().name(), domain));
    }
}
