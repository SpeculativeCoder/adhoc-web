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
    public static final String AWARD_FACTION_SCORES = "awardFactionScores";
    public static final String DECAY_FACTION_SCORES = "decayFactionScores";
    public static final String DECAY_USER_SCORES = "decayUserScores";
    public static final String PURGE_OLD_USERS = "purgeOldUsers";
    public static final String PURGE_OLD_PAWNS = "purgeOldPawns";

    @Bean
    public Trigger manageServersTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(MANAGE_SERVERS).withIdentity(MANAGE_SERVERS)
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .repeatForever().withIntervalInSeconds(30))
                .startAt(Date.from(Instant.now().plusSeconds(10))).build();
    }

    @Bean
    public Trigger grantFactionScoresTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(AWARD_FACTION_SCORES).withIdentity(AWARD_FACTION_SCORES)
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .repeatForever().withIntervalInSeconds(5))
                .startAt(Date.from(Instant.now().plusSeconds(10))).build();
    }

    @Bean
    public Trigger decayFactionScoresTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(DECAY_FACTION_SCORES).withIdentity(DECAY_FACTION_SCORES)
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .repeatForever().withIntervalInSeconds(5))
                .startAt(Date.from(Instant.now().plusSeconds(10))).build();
    }

    @Bean
    public Trigger decayUserScoresTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(DECAY_USER_SCORES).withIdentity(DECAY_USER_SCORES)
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .repeatForever().withIntervalInSeconds(5))
                .startAt(Date.from(Instant.now().plusSeconds(10))).build();
    }

    @Bean
    public Trigger purgeOldUsersTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(PURGE_OLD_USERS).withIdentity(PURGE_OLD_USERS)
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .repeatForever().withIntervalInSeconds(5))
                .startAt(Date.from(Instant.now().plusSeconds(10))).build();
    }

    @Bean
    public Trigger purgeOldPawnsTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(PURGE_OLD_PAWNS).withIdentity(PURGE_OLD_PAWNS)
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .repeatForever().withIntervalInSeconds(5))
                .startAt(Date.from(Instant.now().plusSeconds(10))).build();
    }

    @Bean
    public JobDetail manageServersJobDetail() {
        return JobBuilder.newJob(ManagerQuartzJob.class).withIdentity(MANAGE_SERVERS).storeDurably().build();
    }

    @Bean
    public JobDetail grantFactionScoresJobDetail() {
        return JobBuilder.newJob(ManagerQuartzJob.class).withIdentity(AWARD_FACTION_SCORES).storeDurably().build();
    }

    @Bean
    public JobDetail decayFactionScoresJobDetail() {
        return JobBuilder.newJob(ManagerQuartzJob.class).withIdentity(DECAY_FACTION_SCORES).storeDurably().build();
    }

    @Bean
    public JobDetail decayUserScoresJobDetail() {
        return JobBuilder.newJob(ManagerQuartzJob.class).withIdentity(DECAY_USER_SCORES).storeDurably().build();
    }

    @Bean
    public JobDetail purgeOldUsersJobDetail() {
        return JobBuilder.newJob(ManagerQuartzJob.class).withIdentity(PURGE_OLD_USERS).storeDurably().build();
    }

    @Bean
    public JobDetail purgeOldPawnsJobDetail() {
        return JobBuilder.newJob(ManagerQuartzJob.class).withIdentity(PURGE_OLD_PAWNS).storeDurably().build();
    }
}
