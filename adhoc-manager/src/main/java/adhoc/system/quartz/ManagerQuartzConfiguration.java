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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.Date;

/**
 * Scheduled jobs triggered by Quartz.
 * Includes things like managing the servers, cleaning up old users, regular scoring etc.
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class ManagerQuartzConfiguration {

    public static final String MANAGE_SERVERS = "manageServers";
    public static final String MANAGE_TASKS = "manageTasks";
    public static final String MANAGE_SERVER_TASKS = "manageServerTasks";
    public static final String MANAGE_TASK_DOMAINS = "manageTaskDomains";
    public static final String AWARD_AND_DECAY_FACTION_SCORES = "awardAndDecayFactionScores";
    public static final String AWARD_AND_DECAY_USER_SCORES = "awardAndDecayUserScores";
    public static final String MANAGE_SEEN_USERS = "manageSeenUsers";
    public static final String PURGE_OLD_USERS = "purgeOldUsers";
    public static final String PURGE_OLD_SERVERS = "purgeOldServers";
    public static final String PURGE_OLD_PAWNS = "purgeOldPawns";

    public static final Instant baseStartInstant = Instant.now();
    public static long startOffset = 0;

    @Bean
    public Trigger manageServersTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(MANAGE_SERVERS)
                .withIdentity(MANAGE_SERVERS)
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .repeatForever()
                        .withIntervalInSeconds(10)
                        .withMisfireHandlingInstructionNextWithRemainingCount())
                .startAt(Date.from(baseStartInstant)) //.plusMillis(startOffset += 200)))
                .build();
    }

    @Bean
    public Trigger manageTasksTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(MANAGE_TASKS)
                .withIdentity(MANAGE_TASKS)
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .repeatForever()
                        .withIntervalInSeconds(10)
                        .withMisfireHandlingInstructionNextWithRemainingCount())
                .startAt(Date.from(baseStartInstant)) //.plusMillis(startOffset += 200)))
                .build();
    }

    @Bean
    public Trigger manageTaskDomainsTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(MANAGE_TASK_DOMAINS)
                .withIdentity(MANAGE_TASK_DOMAINS)
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .repeatForever()
                        .withIntervalInSeconds(10)
                        .withMisfireHandlingInstructionNextWithRemainingCount())
                .startAt(Date.from(baseStartInstant)) //.plusMillis(startOffset += 200)))
                .build();
    }

    @Bean
    public Trigger manageServerTasksTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(MANAGE_SERVER_TASKS)
                .withIdentity(MANAGE_SERVER_TASKS)
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .repeatForever()
                        .withIntervalInSeconds(10)
                        .withMisfireHandlingInstructionNextWithRemainingCount())
                .startAt(Date.from(baseStartInstant)) //.plusMillis(startOffset += 200)))
                .build();
    }

    @Bean
    public Trigger awardAndDecayFactionScoresTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(AWARD_AND_DECAY_FACTION_SCORES)
                .withIdentity(AWARD_AND_DECAY_FACTION_SCORES)
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .repeatForever()
                        .withIntervalInSeconds(10)
                        .withMisfireHandlingInstructionNextWithRemainingCount())
                .startAt(Date.from(baseStartInstant)) //.plusMillis(startOffset += 200)))
                .build();
    }

    @Bean
    public Trigger awardAndDecayUserScoresTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(AWARD_AND_DECAY_USER_SCORES)
                .withIdentity(AWARD_AND_DECAY_USER_SCORES)
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .repeatForever()
                        .withIntervalInSeconds(10)
                        .withMisfireHandlingInstructionNextWithRemainingCount())
                .startAt(Date.from(baseStartInstant)) //.plusMillis(startOffset += 200)))
                .build();
    }

    @Bean
    public Trigger manageSeenUsersTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(MANAGE_SEEN_USERS)
                .withIdentity(MANAGE_SEEN_USERS)
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .repeatForever()
                        .withIntervalInSeconds(10)
                        .withMisfireHandlingInstructionNextWithRemainingCount())
                .startAt(Date.from(baseStartInstant)) //.plusMillis(startOffset += 200)))
                .build();
    }

    @Bean
    public Trigger purgeOldUsersTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(PURGE_OLD_USERS)
                .withIdentity(PURGE_OLD_USERS)
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .repeatForever()
                        .withIntervalInSeconds(10)
                        .withMisfireHandlingInstructionNextWithRemainingCount())
                .startAt(Date.from(baseStartInstant)) //.plusMillis(startOffset += 200)))
                .build();
    }

    @Bean
    public Trigger purgeOldServers() {
        return TriggerBuilder.newTrigger()
                .forJob(PURGE_OLD_SERVERS)
                .withIdentity(PURGE_OLD_SERVERS)
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .repeatForever()
                        .withIntervalInSeconds(10)
                        .withMisfireHandlingInstructionNextWithRemainingCount())
                .startAt(Date.from(baseStartInstant)) //.plusMillis(startOffset += 200)))
                .build();
    }

    @Bean
    public Trigger purgeOldPawnsTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(PURGE_OLD_PAWNS)
                .withIdentity(PURGE_OLD_PAWNS)
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .repeatForever()
                        .withIntervalInSeconds(10)
                        .withMisfireHandlingInstructionNextWithRemainingCount())
                .startAt(Date.from(baseStartInstant)) //.plusMillis(startOffset += 200)))
                .build();
    }

    @Bean
    public JobDetail manageServersJobDetail() {
        return JobBuilder.newJob(ManagerQuartzJob.class).withIdentity(MANAGE_SERVERS).storeDurably().build();
    }

    @Bean
    public JobDetail manageTasksJobDetail() {
        return JobBuilder.newJob(ManagerQuartzJob.class).withIdentity(MANAGE_TASKS).storeDurably().build();
    }

    @Bean
    public JobDetail manageTaskDomainsJobDetail() {
        return JobBuilder.newJob(ManagerQuartzJob.class).withIdentity(MANAGE_TASK_DOMAINS).storeDurably().build();
    }

    @Bean
    public JobDetail manageServerTasksJobDetail() {
        return JobBuilder.newJob(ManagerQuartzJob.class).withIdentity(MANAGE_SERVER_TASKS).storeDurably().build();
    }

    @Bean
    public JobDetail awardAndDecayFactionScoresJobDetail() {
        return JobBuilder.newJob(ManagerQuartzJob.class).withIdentity(AWARD_AND_DECAY_FACTION_SCORES).storeDurably().build();
    }

    @Bean
    public JobDetail awardAndDecayUserScoresJobDetail() {
        return JobBuilder.newJob(ManagerQuartzJob.class).withIdentity(AWARD_AND_DECAY_USER_SCORES).storeDurably().build();
    }

    @Bean
    public JobDetail manageUserPawnsJobDetail() {
        return JobBuilder.newJob(ManagerQuartzJob.class).withIdentity(MANAGE_SEEN_USERS).storeDurably().build();
    }

    @Bean
    public JobDetail purgeOldUsersJobDetail() {
        return JobBuilder.newJob(ManagerQuartzJob.class).withIdentity(PURGE_OLD_USERS).storeDurably().build();
    }

    @Bean
    public JobDetail purgeOldServersJobDetail() {
        return JobBuilder.newJob(ManagerQuartzJob.class).withIdentity(PURGE_OLD_SERVERS).storeDurably().build();
    }

    @Bean
    public JobDetail purgeOldPawnsJobDetail() {
        return JobBuilder.newJob(ManagerQuartzJob.class).withIdentity(PURGE_OLD_PAWNS).storeDurably().build();
    }
}
