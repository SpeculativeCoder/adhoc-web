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

package adhoc.task.server;

import adhoc.message.MessageService;
import adhoc.task.TaskDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.resilience.annotation.Retryable;
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

        return serverTaskRepository.findTaskIdentifierByInitiatedBeforeAndServerNotEnabled(initiatedBefore);
    }

    @Retryable(includes = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxRetries = 3, delay = 100, jitter = 10, multiplier = 1, maxDelay = 1000)
    void createServerTask(TaskDto serverTask) {

        ServerTaskEntity serverTaskEntity = new ServerTaskEntity();
        serverTaskEntity.setServerId(serverTask.getServerId());
        serverTaskEntity.setTaskIdentifier(serverTask.getTaskIdentifier());
        serverTaskEntity.setPublicWebSocketPort(serverTask.getPublicWebSocketPort());
        serverTaskEntity.setInitiated(LocalDateTime.now()); // TODO

        serverTaskRepository.save(serverTaskEntity);

        messageService.addGlobalMessage(String.format("Server task %d created", serverTaskEntity.getId()));
    }

    @Retryable(includes = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxRetries = 3, delay = 100, jitter = 10, multiplier = 1, maxDelay = 1000)
    void deleteServerTask(String serverTaskIdentifier) {

        serverTaskRepository.deleteByTaskIdentifier(serverTaskIdentifier);
    }
}
