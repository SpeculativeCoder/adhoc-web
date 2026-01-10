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

package adhoc.system.quartz;

import adhoc.faction.award_decay.FactionAwardDecayService;
import adhoc.pawn.purge.PawnPurgeService;
import adhoc.server.allocate.ServerAllocateService;
import adhoc.server.purge.ServerPurgeService;
import adhoc.shared.Event;
import adhoc.task.domain.TaskDomainOrchestrator;
import adhoc.task.refresh.TaskRefreshOrchestrator;
import adhoc.task.server.ServerTaskManagerOrchestrator;
import adhoc.user.award_decay.UserAwardDecayService;
import adhoc.user.purge.UserPurgeService;
import adhoc.user.state.UserStateManagerService;
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

    private final ServerAllocateService serverAllocateService;
    private final ServerPurgeService serverPurgeService;
    private final TaskRefreshOrchestrator taskRefreshOrchestrator;
    private final TaskDomainOrchestrator taskDomainOrchestrator;
    private final ServerTaskManagerOrchestrator serverTaskManagerOrchestrator;
    private final FactionAwardDecayService factionAwardDecayService;
    private final UserStateManagerService userStateManagerService;
    private final UserAwardDecayService userAwardDecayService;
    private final UserPurgeService userPurgeService;
    private final PawnPurgeService pawnPurgeService;

    private final SimpMessageSendingOperations stomp;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String jobName = context.getJobDetail().getKey().getName();

        List<? extends Event> events = Collections.emptyList();

        try (MDC.MDCCloseable closeable = MDC.putCloseable("job", jobName)) {
            log.trace("jobName={}", jobName);

            switch (jobName) {
            case ManagerQuartzConfiguration.ALLOCATE_SERVERS:
                events = serverAllocateService.allocateServers();
                break;
            case ManagerQuartzConfiguration.REFRESH_TASKS:
                taskRefreshOrchestrator.refreshTasks();
                break;
            case ManagerQuartzConfiguration.MANAGE_TASK_DOMAINS:
                taskDomainOrchestrator.manageTaskDomains();
                break;
            case ManagerQuartzConfiguration.MANAGE_SERVER_TASKS:
                serverTaskManagerOrchestrator.manageServerTasks();
                break;
            case ManagerQuartzConfiguration.AWARD_AND_DECAY_FACTION_SCORES:
                factionAwardDecayService.awardAndDecayFactionScores();
                break;
            case ManagerQuartzConfiguration.AWARD_AND_DECAY_USER_SCORES:
                userAwardDecayService.awardAndDecayUserScores();
                break;
            case ManagerQuartzConfiguration.MANAGE_SEEN_USERS:
                userStateManagerService.manageSeenUsers();
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
