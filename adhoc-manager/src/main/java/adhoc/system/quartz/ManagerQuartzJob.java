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

package adhoc.system.quartz;

import adhoc.faction.ManagerFactionService;
import adhoc.pawn.purge.PawnPurgeService;
import adhoc.server.ManagerServerService;
import adhoc.server.purge.ServerPurgeService;
import adhoc.system.event.Event;
import adhoc.task.ManagerTaskService;
import adhoc.task.server.ManagerServerTaskService;
import adhoc.user.ManagerUserService;
import adhoc.user.leave.UserLeaveService;
import adhoc.user.purge.UserPurgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.MDC;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
//@PersistJobDataAfterExecution
@DisallowConcurrentExecution
@Slf4j
@RequiredArgsConstructor
public class ManagerQuartzJob implements Job {

    private final ManagerServerService managerServerService;
    private final ServerPurgeService serverPurgeService;
    private final ManagerTaskService managerTaskService;
    private final ManagerServerTaskService managerServerTaskService;
    private final ManagerFactionService managerFactionService;
    private final ManagerUserService managerUserService;
    private final UserLeaveService userLeaveService;
    private final UserPurgeService userPurgeService;
    private final PawnPurgeService pawnPurgeService;

    private final SimpMessageSendingOperations stomp;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String jobName = context.getJobDetail().getKey().getName();

        List<? extends Event> events = Collections.emptyList();

        try (MDC.MDCCloseable closeable = MDC.putCloseable("job", jobName)) {
            //log.info("jobName={}", jobName);
            switch (jobName) {
            case ManagerQuartzConfiguration.MANAGE_SERVERS:
                events = managerServerService.manageServers();
                break;
            case ManagerQuartzConfiguration.REFRESH_TASKS:
                managerTaskService.refreshTasks();
                break;
            case ManagerQuartzConfiguration.MANAGE_TASK_DOMAINS:
                events = managerTaskService.manageTaskDomains();
                break;
            case ManagerQuartzConfiguration.MANAGE_SERVER_TASKS:
                managerServerTaskService.manageServerTasks();
                break;
            case ManagerQuartzConfiguration.MANAGE_FACTION_SCORES:
                managerFactionService.manageFactionScores();
                break;
            case ManagerQuartzConfiguration.MANAGE_USER_SCORES:
                managerUserService.manageUserScores();
                break;
            case ManagerQuartzConfiguration.MANAGE_USER_LOCATIONS:
                managerUserService.manageUserLocations();
                break;
            case ManagerQuartzConfiguration.LEAVE_UNSEEN_USERS:
                userLeaveService.leaveUnseenUsers();
                break;
            case ManagerQuartzConfiguration.PURGE_OLD_USERS:
                userPurgeService.purgeOldUsers();
                break;
            case ManagerQuartzConfiguration.PURGE_OLD_SERVERS:
                serverPurgeService.purgeOldServers();
                break;
            case ManagerQuartzConfiguration.PURGE_OLD_PAWNS:
                pawnPurgeService.purgeOldPawns();
                break;
            default:
                log.warn("Skipping unknown manager quartz job! jobName={}", jobName);
                break;
            }

            for (Event event : events) {
                log.debug("Sending: {}", event);
                stomp.convertAndSend("/topic/events", event);
            }

        } catch (Exception e) {
            String message = "Manager quartz job exception! jobName=" + jobName + " message=" + e.getMessage();
            log.warn(message, e);
            throw new JobExecutionException(message, e);
        }
    }
}
