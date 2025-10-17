package adhoc.task;

import adhoc.message.MessageService;
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

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class ServerTaskManagerService {

    private final ServerTaskRepository serverTaskRepository;

    private final TaskService taskService;
    private final MessageService messageService;

    //public Optional<TaskDto> findFirstByServerId(Long serverId) {
    //    return serverTaskRepository.findFirstByServerId(serverId).map(taskService::toDto);
    //}

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(retryFor = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 100, maxDelay = 1000))
    void createServerTaskInNewTransaction(ServerTaskEntity serverTask) {

        serverTask.setInitiated(LocalDateTime.now()); // TODO

        serverTaskRepository.save(serverTask);

        messageService.addGlobalMessage(String.format("Server task %d created", serverTask.getId()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(retryFor = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 100, maxDelay = 1000))
    void deleteServerTaskInNewTransaction(Long serverTaskId) {

        serverTaskRepository.deleteById(serverTaskId);

        messageService.addGlobalMessage(String.format("Server task %d deleted", serverTaskId));
    }
}
