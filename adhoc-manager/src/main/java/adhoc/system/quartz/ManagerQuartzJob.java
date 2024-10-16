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

import adhoc.faction.FactionScoreService;
import adhoc.pawn.PawnPurgeService;
import adhoc.server.ServerManagerService;
import adhoc.server.ServerPurgeService;
import adhoc.system.event.Event;
import adhoc.task.TaskDomainService;
import adhoc.task.TaskPollService;
import adhoc.task.server.ServerTaskManagerService;
import adhoc.user.UserPurgeService;
import adhoc.user.UserScoreService;
import adhoc.user.UserSeenService;
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

    private final ServerManagerService serverManagerService;
    private final ServerPurgeService serverPurgeService;
    private final TaskPollService taskPollService;
    private final TaskDomainService taskDomainService;
    private final ServerTaskManagerService serverTaskManagerService;
    private final FactionScoreService factionScoreService;
    private final UserSeenService userSeenService;
    private final UserScoreService userScoreService;
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
                events = serverManagerService.manageServers();
                break;
            case ManagerQuartzConfiguration.POLL_TASKS:
                taskPollService.pollTasks();
                break;
            case ManagerQuartzConfiguration.MANAGE_TASK_DOMAINS:
                events = taskDomainService.manageTaskDomains();
                break;
            case ManagerQuartzConfiguration.MANAGE_SERVER_TASKS:
                serverTaskManagerService.manageServerTasks();
                break;
            case ManagerQuartzConfiguration.AWARD_AND_DECAY_FACTION_SCORES:
                factionScoreService.awardAndDecayFactionScores();
                break;
            case ManagerQuartzConfiguration.AWARD_AND_DECAY_USER_SCORES:
                userScoreService.awardAndDecayUserScores();
                break;
            case ManagerQuartzConfiguration.MANAGE_SEEN_USERS:
                userSeenService.manageSeenUsers();
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
