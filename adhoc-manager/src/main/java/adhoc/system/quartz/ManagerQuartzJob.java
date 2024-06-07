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

import adhoc.faction.FactionScoreJobService;
import adhoc.pawn.PawnPurgeJobService;
import adhoc.server.ServerManagerJobService;
import adhoc.server.ServerPurgeJobService;
import adhoc.system.event.Event;
import adhoc.task.TaskDomainJobService;
import adhoc.task.TaskManagerJobService;
import adhoc.task.server.ServerTaskManagerJobService;
import adhoc.user.UserManagerJobService;
import adhoc.user.UserPurgeJobService;
import adhoc.user.UserScoreJobService;
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

    private final ServerManagerJobService serverManagerJobService;
    private final ServerPurgeJobService serverPurgeJobService;
    private final TaskManagerJobService taskManagerJobService;
    private final TaskDomainJobService taskDomainJobService;
    private final ServerTaskManagerJobService serverTaskManagerJobService;
    private final FactionScoreJobService factionScoreJobService;
    private final UserManagerJobService userManagerJobService;
    private final UserScoreJobService userScoreJobService;
    private final UserPurgeJobService userPurgeJobService;
    private final PawnPurgeJobService pawnPurgeJobService;

    private final SimpMessageSendingOperations stomp;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String jobName = context.getJobDetail().getKey().getName();

        List<? extends Event> events = Collections.emptyList();

        try (MDC.MDCCloseable closeable = MDC.putCloseable("job", jobName)) {
            //log.info("jobName={}", jobName);
            switch (jobName) {
            case ManagerQuartzConfiguration.MANAGE_SERVERS:
                events = serverManagerJobService.manageServers();
                break;
            case ManagerQuartzConfiguration.MANAGE_TASKS:
                taskManagerJobService.manageTasks();
                break;
            case ManagerQuartzConfiguration.MANAGE_TASK_DOMAINS:
                events = taskDomainJobService.manageTaskDomains();
                break;
            case ManagerQuartzConfiguration.MANAGE_SERVER_TASKS:
                serverTaskManagerJobService.manageServerTasks();
                break;
            case ManagerQuartzConfiguration.AWARD_AND_DECAY_FACTION_SCORES:
                factionScoreJobService.awardAndDecayFactionScores();
                break;
            case ManagerQuartzConfiguration.AWARD_AND_DECAY_USER_SCORES:
                userScoreJobService.awardAndDecayUserScores();
                break;
            case ManagerQuartzConfiguration.MANAGE_USERS:
                userManagerJobService.manageUsers();
                break;
            case ManagerQuartzConfiguration.PURGE_OLD_USERS:
                userPurgeJobService.purgeOldUsers();
                break;
            case ManagerQuartzConfiguration.PURGE_OLD_SERVERS:
                serverPurgeJobService.purgeOldServers();
                break;
            case ManagerQuartzConfiguration.PURGE_OLD_PAWNS:
                pawnPurgeJobService.purgeOldPawns();
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
