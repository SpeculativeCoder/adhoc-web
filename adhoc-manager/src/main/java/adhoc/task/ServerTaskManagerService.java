package adhoc.task;

import adhoc.message.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class ServerTaskManagerService {

    private final ServerTaskRepository serverTaskRepository;

    private final MessageService messageService;

    public List<String> findSeenUnusedServerTasks() {
        LocalDateTime initiatedBefore = LocalDateTime.now().minusMinutes(1);

        // TODO
        serverTaskRepository.deleteBySeenIsNullAndInitiatedBefore(initiatedBefore);

        return serverTaskRepository.findTaskIdentifierByInitiatedBeforeAndServerNotEnabled(initiatedBefore);
    }

    @Retryable(retryFor = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 100, maxDelay = 1000))
    void createServerTask(TaskDto serverTask) {

        ServerTaskEntity serverTaskEntity = new ServerTaskEntity();
        serverTaskEntity.setServerId(serverTask.getServerId());
        serverTaskEntity.setTaskIdentifier(serverTask.getTaskIdentifier());
        serverTaskEntity.setPublicWebSocketPort(serverTask.getPublicWebSocketPort());
        serverTaskEntity.setInitiated(LocalDateTime.now()); // TODO

        serverTaskRepository.save(serverTaskEntity);

        messageService.addGlobalMessage(String.format("Server task %d created", serverTaskEntity.getId()));
    }

    @Retryable(retryFor = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 100, maxDelay = 1000))
    void deleteServerTask(String serverTaskIdentifier) {

        serverTaskRepository.deleteByTaskIdentifier(serverTaskIdentifier);
    }
}
