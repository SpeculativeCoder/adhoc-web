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

import adhoc.hosting.HostingService;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class TaskRefreshService {

    private final TaskRepository taskRepository;

    private final HostingService hostingService;

    @Setter(onMethod_ = {@Autowired}, onParam_ = {@Lazy})
    private TaskRefreshService self;

    /** Query the hosting service for the current state of the tasks. */
    public void refreshTasks() {
        List<TaskEntity> hostedTasks = hostingService.poll();

        log.debug("hostedTasks={}", hostedTasks);
        Verify.verifyNotNull(hostedTasks, "hostedTasks is null!");

        self.updateTasksInNewTransaction(hostedTasks);
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
