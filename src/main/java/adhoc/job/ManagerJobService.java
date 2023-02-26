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

package adhoc.job;

import adhoc.faction.FactionManagerJobService;
import adhoc.pawn.PawnManagerJobService;
import adhoc.server.ServerManagerJobService;
import adhoc.user.UserManagerJobService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Transactional
@Service
@Profile("mode-manager")
@Slf4j
@RequiredArgsConstructor
public class ManagerJobService implements Job {

    private final ServerManagerJobService serverManagerJobService;

    private final FactionManagerJobService factionManagerJobService;

    private final UserManagerJobService userManagerJobService;

    private final PawnManagerJobService pawnManagerJobService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            String jobName = context.getJobDetail().getKey().getName();
            switch (jobName) {
                case ManagerJobConfig.MANAGE_SERVERS:
                    serverManagerJobService.manageServers();
                    break;
                case ManagerJobConfig.AWARD_FACTION_SCORES:
                    factionManagerJobService.awardFactionScores();
                    break;
                case ManagerJobConfig.DECAY_FACTION_SCORES:
                    factionManagerJobService.decayFactionScores();
                    break;
                case ManagerJobConfig.DECAY_USER_SCORES:
                    factionManagerJobService.decayUserScores();
                    break;
                case ManagerJobConfig.PURGE_OLD_USERS:
                    userManagerJobService.purgeOldUsers();
                    break;
                case ManagerJobConfig.PURGE_OLD_PAWNS:
                    pawnManagerJobService.purgeOldPawns();
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
