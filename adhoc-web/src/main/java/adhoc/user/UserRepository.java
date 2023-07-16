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

package adhoc.user;

import adhoc.faction.Faction;
import adhoc.server.Server;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByName(String name);

    Optional<User> findByNameOrEmail(String name, String email);

    Optional<User> findByNameOrEmailAndPasswordIsNotNull(String name, String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    User getUserById(Long id);

    @Transactional
    @Modifying
    @Query("update AdhocUser u set u.version = u.version + 1, u.server = :server, u.seen = :seen where u.id in :ids")
    void updateUsersServerAndSeenByIdIn(@Param("server") Server server, @Param("seen") LocalDateTime seen, @Param("ids") Collection<Long> ids);

    @Transactional
    @Modifying
    @Query("update AdhocUser u set u.version = u.version + 1, u.score = u.score + :score where u.faction = :faction and u.seen > :cutoffTime")
    void updateUsersAddScoreByFactionAndSeenAfter(@Param("score") float score, @Param("faction") Faction faction, @Param("cutoffTime") LocalDateTime cutoffTime);

    @Transactional
    @Modifying
    @Query("update AdhocUser u set u.version = u.version + 1, u.score = u.score * :multiplier")
    void updateUsersMultiplyScore(@Param("multiplier") float multiplier);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    void deleteUsersByCreatedBeforeAndSeenIsNullAndPasswordIsNullAndPawnsEmpty(LocalDateTime createdBefore);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    void deleteUsersBySeenBeforeAndPasswordIsNullAndPawnsEmpty(LocalDateTime seenBefore);
}
