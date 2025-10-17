package adhoc.task;

import adhoc.message.MessageService;
import com.google.common.base.Verify;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class TaskManagerService {

    private final TaskRepository taskRepository;

    private final MessageService messageService;

    // NOTE: done in new transaction to avoid retries spamming DNS service due to optimistic locking
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(retryFor = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 100, maxDelay = 1000))
    public void updateTaskDomainInNewTransaction(Long taskId, String domain) {
        TaskEntity task = taskRepository.getReferenceById(taskId);

        if (!Objects.equals(task.getDomain(), domain)) {
            task.setDomain(domain);
        }

        messageService.addGlobalMessage(String.format("Task %d (of type %s) mapped to domain %s", task.getId(), task.getType().name(), domain));
    }

    // NOTE: done in new transaction to avoid retries spamming hosting service due to optimistic locking
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(retryFor = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 100, maxDelay = 1000))
    void updateTasksInNewTransaction(List<TaskEntity> hostedTasks) {

        List<String> taskIdentifiers = new ArrayList<>();
        LocalDateTime seen = LocalDateTime.now();

        for (TaskEntity hostedTask : hostedTasks) {
            Verify.verifyNotNull(hostedTask.getTaskIdentifier(), "hosted task identifier is null! task=%s", hostedTask);
            taskIdentifiers.add(hostedTask.getTaskIdentifier());

            TaskEntity task = taskRepository.findByTaskIdentifier(hostedTask.getTaskIdentifier())
                    .map(existingTask -> updateExistingTask(existingTask, hostedTask))
                    .orElse(hostedTask); // else will save this as a new task

            task.setSeen(seen);

            if (task.getId() == null) {
                taskRepository.save(task);
            }
        }

        // any tasks we have seen in a previous poll but are no longer running - delete their entry
        taskRepository.deleteByTaskIdentifierNotInAndSeenNotNull(taskIdentifiers);
    }

    private static TaskEntity updateExistingTask(TaskEntity existingTask, TaskEntity hostedTask) {

        if (!Objects.equals(existingTask.getPrivateIp(), hostedTask.getPrivateIp())) {
            existingTask.setPrivateIp(hostedTask.getPrivateIp());
        }
        if (!Objects.equals(existingTask.getPublicIp(), hostedTask.getPublicIp())) {
            existingTask.setPublicIp(hostedTask.getPublicIp());
        }

        // TODO
        if (existingTask instanceof ServerTaskEntity existingServerTask
                && hostedTask instanceof ServerTaskEntity hostedServerTask) {

            if (!Objects.equals(existingServerTask.getPublicWebSocketPort(), hostedServerTask.getPublicWebSocketPort())) {
                existingServerTask.setPublicWebSocketPort(hostedServerTask.getPublicWebSocketPort());
            }
            if (!Objects.equals(existingServerTask.getServerId(), hostedServerTask.getServerId())) {
                existingServerTask.setServerId(hostedServerTask.getServerId());
            }
        }

        return existingTask;
    }
}
