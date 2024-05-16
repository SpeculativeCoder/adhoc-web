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

package adhoc.quartz;

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
public class ManagerQuartzConfig {

    public static final String MANAGE_SERVERS = "manageServers";
    public static final String REFRESH_TASKS = "refreshTasks";
    public static final String MANAGE_SERVER_TASKS = "manageServerTasks";
    public static final String MANAGE_TASK_DOMAINS = "manageTaskDomains";
    public static final String MANAGE_FACTION_SCORES = "manageFactionScores";
    public static final String MANAGE_USER_SCORES = "manageUserScores";
    public static final String MANAGE_USER_LOCATIONS = "manageUserLocations";
    public static final String LEAVE_UNSEEN_USERS = "leaveUnseenUsers";
    public static final String PURGE_OLD_USERS = "purgeOldUsers";
    public static final String PURGE_OLD_SERVERS = "purgeOldServers";
    public static final String PURGE_OLD_PAWNS = "purgeOldPawns";

    public static final Instant baseStartInstant = Instant.now();
    public static long startOffset = 0;

    @Bean
    public Trigger manageServersTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(MANAGE_SERVERS).withIdentity(MANAGE_SERVERS)
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .repeatForever().withIntervalInSeconds(10).withMisfireHandlingInstructionNextWithRemainingCount())
                .startAt(Date.from(baseStartInstant.plusMillis(startOffset += 100))).build();
    }

    @Bean
    public Trigger refreshTasksTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(REFRESH_TASKS).withIdentity(REFRESH_TASKS)
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .repeatForever().withIntervalInSeconds(10).withMisfireHandlingInstructionNextWithRemainingCount())
                .startAt(Date.from(baseStartInstant.plusMillis(startOffset += 100))).build();
    }

    @Bean
    public Trigger manageTaskDomainsTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(MANAGE_TASK_DOMAINS).withIdentity(MANAGE_TASK_DOMAINS)
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .repeatForever().withIntervalInSeconds(10).withMisfireHandlingInstructionNextWithRemainingCount())
                .startAt(Date.from(baseStartInstant.plusMillis(startOffset += 100))).build();
    }

    @Bean
    public Trigger manageServerTasksTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(MANAGE_SERVER_TASKS).withIdentity(MANAGE_SERVER_TASKS)
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .repeatForever().withIntervalInSeconds(10).withMisfireHandlingInstructionNextWithRemainingCount())
                .startAt(Date.from(baseStartInstant.plusMillis(startOffset += 100))).build();
    }

    @Bean
    public Trigger manageFactionScoresTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(MANAGE_FACTION_SCORES).withIdentity(MANAGE_FACTION_SCORES)
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .repeatForever().withIntervalInSeconds(10).withMisfireHandlingInstructionNextWithRemainingCount())
                .startAt(Date.from(baseStartInstant.plusMillis(startOffset += 100))).build();
    }

    @Bean
    public Trigger manageUserScoresTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(MANAGE_USER_SCORES).withIdentity(MANAGE_USER_SCORES)
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .repeatForever().withIntervalInSeconds(10).withMisfireHandlingInstructionNextWithRemainingCount())
                .startAt(Date.from(baseStartInstant.plusMillis(startOffset += 100))).build();
    }

    @Bean
    public Trigger manageUserLocationsTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(MANAGE_USER_LOCATIONS).withIdentity(MANAGE_USER_LOCATIONS)
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .repeatForever().withIntervalInSeconds(10).withMisfireHandlingInstructionNextWithRemainingCount())
                .startAt(Date.from(baseStartInstant.plusMillis(startOffset += 100))).build();
    }

    @Bean
    public Trigger leaveUnseenUsersTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(LEAVE_UNSEEN_USERS).withIdentity(LEAVE_UNSEEN_USERS)
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .repeatForever().withIntervalInSeconds(10).withMisfireHandlingInstructionNextWithRemainingCount())
                .startAt(Date.from(baseStartInstant.plusMillis(startOffset += 100))).build();
    }

    @Bean
    public Trigger purgeOldUsersTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(PURGE_OLD_USERS).withIdentity(PURGE_OLD_USERS)
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .repeatForever().withIntervalInSeconds(10).withMisfireHandlingInstructionNextWithRemainingCount())
                .startAt(Date.from(baseStartInstant.plusMillis(startOffset += 100))).build();
    }

    @Bean
    public Trigger purgeOldServers() {
        return TriggerBuilder.newTrigger()
                .forJob(PURGE_OLD_SERVERS).withIdentity(PURGE_OLD_SERVERS)
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .repeatForever().withIntervalInSeconds(10).withMisfireHandlingInstructionNextWithRemainingCount())
                .startAt(Date.from(baseStartInstant.plusMillis(startOffset += 100))).build();
    }

    @Bean
    public Trigger purgeOldPawnsTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(PURGE_OLD_PAWNS).withIdentity(PURGE_OLD_PAWNS)
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .repeatForever().withIntervalInSeconds(10).withMisfireHandlingInstructionNextWithRemainingCount())
                .startAt(Date.from(baseStartInstant.plusSeconds(startOffset += 100))).build();
    }

    @Bean
    public JobDetail manageServersJobDetail() {
        return JobBuilder.newJob(ManagerQuartzJob.class).withIdentity(MANAGE_SERVERS).storeDurably().build();
    }

    @Bean
    public JobDetail refreshTasksJobDetail() {
        return JobBuilder.newJob(ManagerQuartzJob.class).withIdentity(REFRESH_TASKS).storeDurably().build();
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
    public JobDetail manageFactionScoresJobDetail() {
        return JobBuilder.newJob(ManagerQuartzJob.class).withIdentity(MANAGE_FACTION_SCORES).storeDurably().build();
    }

    @Bean
    public JobDetail manageUserScoresJobDetail() {
        return JobBuilder.newJob(ManagerQuartzJob.class).withIdentity(MANAGE_USER_SCORES).storeDurably().build();
    }

    @Bean
    public JobDetail manageUserLocationsJobDetail() {
        return JobBuilder.newJob(ManagerQuartzJob.class).withIdentity(MANAGE_USER_LOCATIONS).storeDurably().build();
    }

    @Bean
    public JobDetail leaveUnseenUsersJobDetail() {
        return JobBuilder.newJob(ManagerQuartzJob.class).withIdentity(LEAVE_UNSEEN_USERS).storeDurably().build();
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
