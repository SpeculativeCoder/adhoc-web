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

@Transactional
@Service
@Slf4j
@RequiredArgsConstructor
public class TaskManagerJobService {

    private final TaskRepository taskRepository;

    private final HostingService hostingService;

    @Retryable(retryFor = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 100, maxDelay = 1000))
    public void manageTasks() {
        log.trace("Manage tasks...");

        // get state of running containers
        List<HostedTask> hostedTasks = hostingService.poll();
        log.debug("hostedTasks={}", hostedTasks);
        Verify.verifyNotNull(hostedTasks, "hostedTasks is null after polling hosting service!");

        LocalDateTime seen = LocalDateTime.now();

        List<String> taskIdentifiers = new ArrayList<>();

        for (HostedTask hostedTask : hostedTasks) {
            Task task = taskRepository.findByTaskIdentifier(hostedTask.getTaskIdentifier())
                    // TODO
                    .orElseGet(() -> switch (hostedTask) {
                        case HostedManagerTask hostedManagerTask -> new ManagerTask();
                        case HostedKioskTask hostedKioskTask -> new KioskTask();
                        case HostedServerTask hostedServerTask -> new ServerTask();
                        default -> throw new IllegalStateException("Unknown hosted task type: " + hostedTask.getClass());
                    });

            if (!Objects.equals(task.getTaskIdentifier(), hostedTask.getTaskIdentifier())) {
                task.setTaskIdentifier(hostedTask.getTaskIdentifier());
            }
            if (!Objects.equals(task.getPrivateIp(), hostedTask.getPrivateIp())) {
                task.setPrivateIp(hostedTask.getPrivateIp());
            }
            if (!Objects.equals(task.getPublicIp(), hostedTask.getPublicIp())) {
                task.setPublicIp(hostedTask.getPublicIp());
            }

            // TODO
            if (hostedTask instanceof HostedServerTask hostedServerTask) {
                ServerTask serverTask = (ServerTask) task;

                if (!Objects.equals(serverTask.getPublicWebSocketPort(), hostedServerTask.getPublicWebSocketPort())) {
                    serverTask.setPublicWebSocketPort(hostedServerTask.getPublicWebSocketPort());
                }
                if (!Objects.equals(serverTask.getServerId(), hostedServerTask.getServerId())) {
                    serverTask.setServerId(hostedServerTask.getServerId());
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
