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

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByName(String name);

    Optional<User> findByNameOrEmail(String name, String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    User getForUpdateById(Long id);

    @Query("select id from AdhocUser where created < :createdBefore and seen is null and password is null and pawns is empty")
    List<Long> findIdsByCreatedBeforeAndSeenIsNullAndPasswordIsNullAndPawnsIsEmpty(@Param("createdBefore") LocalDateTime createdBefore);

    @Query("select id from AdhocUser where seen < :seenBefore and password is null and pawns is empty")
    List<Long> findIdsBySeenBeforeAndPasswordIsNullAndPawnsIsEmpty(@Param("seenBefore") LocalDateTime seenBefore);

    @Modifying
    @Query("update AdhocUser set version = version + 1, server.id = :serverId, seen = :seen where id in :idIn")
    void updateServerIdAndSeenByIdIn(@Param("serverId") Long serverId, @Param("seen") LocalDateTime seen, @Param("idIn") Collection<Long> idIn);

    @Modifying
    @Query("update AdhocUser set version = version + 1, score = score + :scoreAdd where faction.id = :factionId and seen > :seenAfter")
    void updateScoreAddByFactionIdAndSeenAfter(@Param("scoreAdd") float scoreAdd, @Param("factionId") Long factionId, @Param("seenAfter") LocalDateTime seenAfter);

    @Modifying
    @Query("update AdhocUser set version = version + 1, score = score * :scoreMultiply")
    void updateScoreMultiply(@Param("scoreMultiply") float scoreMultiply);
}
