/*
 * Copyright (c) 2022-2023 SpeculativeCoder (https://github.com/SpeculativeCoder)
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

package adhoc.quartz;

import adhoc.faction.ManagerFactionService;
import adhoc.pawn.ManagerPawnService;
import adhoc.server.ManagerServerService;
import adhoc.user.ManagerUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ManagerQuartzJob implements Job {

    private final ManagerServerService managerServerService;

    private final ManagerFactionService managerFactionService;

    private final ManagerUserService managerUserService;

    private final ManagerPawnService managerPawnService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            String jobName = context.getJobDetail().getKey().getName();
            switch (jobName) {
                case ManagerQuartzConfig.MANAGE_SERVERS:
                    // TODO: have these on separate schedules?
                    managerServerService.manageNeededServers();
                    managerServerService.manageHostingTasks();
                    break;
                case ManagerQuartzConfig.AWARD_FACTION_SCORES:
                    managerFactionService.awardFactionScores();
                    break;
                case ManagerQuartzConfig.DECAY_FACTION_SCORES:
                    managerFactionService.decayFactionScores();
                    break;
                case ManagerQuartzConfig.DECAY_USER_SCORES:
                    managerUserService.decayUserScores();
                    break;
                case ManagerQuartzConfig.PURGE_OLD_USERS:
                    managerUserService.purgeOldUsers();
                    break;
                case ManagerQuartzConfig.PURGE_OLD_PAWNS:
                    managerPawnService.purgeOldPawns();
                    break;
                default:
                    log.error("Skipping unknown job! jobName={}", jobName);
                    break;
            }
        } catch (Exception e) {
            log.error("Quartz job exception! message={}", e.getMessage(), e);
        }
    }
}