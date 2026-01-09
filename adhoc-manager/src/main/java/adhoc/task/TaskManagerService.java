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

import adhoc.task.server.ServerTaskEntity;
import com.google.common.base.Verify;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
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

    @Retryable(includes = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxRetries = 3, delay = 100, jitter = 10, multiplier = 1, maxDelay = 1000)
    void updateTasks(List<TaskEntity> hostedTasks) {

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

        LocalDateTime initiatedOrSeenBefore = LocalDateTime.now().minusMinutes(1);

        // any other tasks that are no longer running - delete their entries
        taskRepository.deleteByTaskIdentifierNotInAndSeenBefore(taskIdentifiers, initiatedOrSeenBefore);
        taskRepository.deleteByTaskIdentifierNotInAndInitiatedBefore(taskIdentifiers, initiatedOrSeenBefore);
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
