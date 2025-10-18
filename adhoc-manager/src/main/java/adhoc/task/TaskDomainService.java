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
