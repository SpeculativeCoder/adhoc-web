/*
 * Copyright (c) 2022-2024 SpeculativeCoder (https://github.com/SpeculativeCoder)
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

import adhoc.hosting.*;
import adhoc.task.kiosk.KioskTask;
import adhoc.task.manager.ManagerTask;
import adhoc.task.server.ServerTask;
import com.google.common.base.Verify;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
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
public class TaskManagerJobService {

    private final TaskRepository taskRepository;

    private final HostingService hostingService;

    @Retryable(retryFor = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 100, maxDelay = 1000))
    public void manageTasks() {
        log.trace("Managing tasks...");

        // get state of running containers
        List<HostingTask> hostingTasks = hostingService.poll();
        log.debug("hostingTasks={}", hostingTasks);
        Verify.verifyNotNull(hostingTasks, "hostingTasks is null after polling hosting service!");

        LocalDateTime seen = LocalDateTime.now();

        List<String> taskIdentifiers = new ArrayList<>();

        for (HostingTask hostingTask : hostingTasks) {
            Task task = taskRepository.findByTaskIdentifier(hostingTask.getTaskIdentifier())
                    // TODO
                    .orElseGet(() -> switch (hostingTask) {
                        case ManagerHostingTask hostedManagerTask -> new ManagerTask();
                        case KioskHostingTask hostedKioskTask -> new KioskTask();
                        case ServerHostingTask hostedServerTask -> new ServerTask();
                        default -> throw new IllegalStateException("Unknown hosting task type: " + hostingTask.getClass());
                    });

            if (!Objects.equals(task.getTaskIdentifier(), hostingTask.getTaskIdentifier())) {
                task.setTaskIdentifier(hostingTask.getTaskIdentifier());
            }
            if (!Objects.equals(task.getPrivateIp(), hostingTask.getPrivateIp())) {
                task.setPrivateIp(hostingTask.getPrivateIp());
            }
            if (!Objects.equals(task.getPublicIp(), hostingTask.getPublicIp())) {
                task.setPublicIp(hostingTask.getPublicIp());
            }

            // TODO
            if (hostingTask instanceof ServerHostingTask serverHostingTask) {
                ServerTask serverTask = (ServerTask) task;

                if (!Objects.equals(serverTask.getPublicWebSocketPort(), serverHostingTask.getPublicWebSocketPort())) {
                    serverTask.setPublicWebSocketPort(serverHostingTask.getPublicWebSocketPort());
                }
                if (!Objects.equals(serverTask.getServerId(), serverHostingTask.getServerId())) {
                    serverTask.setServerId(serverHostingTask.getServerId());
                }
            }

            task.setSeen(seen);

            if (task.getId() == null) {
                task = taskRepository.save(task);
            }

            taskIdentifiers.add(task.getTaskIdentifier());
        }

        // any tasks we have seen in a previous poll but are no longer running - delete their entry
        taskRepository.deleteByTaskIdentifierNotInAndSeenNotNull(taskIdentifiers);
    }
}
