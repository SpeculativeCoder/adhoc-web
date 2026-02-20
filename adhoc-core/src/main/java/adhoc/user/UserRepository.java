/*
 * Copyright (c) 2022-2026 SpeculativeCoder (https://github.com/SpeculativeCoder)
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

import adhoc.server.ServerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    boolean existsByName(String name);

    boolean existsByEmail(String name);

    Optional<UserEntity> findByNameOrEmail(String name, String email);

    Optional<UserEntity> findFirstByHumanFalseAndFactionIdAndStateSeenBefore(Long factionId, LocalDateTime seenBefore);

    Stream<UserEntity> streamForWriteByStateServerNotNull();

    @Query("select u.id from User u where u.created < ?1 and u.state.seen is null and u.password is null and u.pawns is empty")
    List<Long> findIdsByCreatedBeforeAndSeenIsNullAndPasswordIsNullAndPawnsIsEmpty(LocalDateTime createdBefore);

    @Query("select u.id from User u where u.state.seen < ?1 and u.password is null and u.pawns is empty")
    List<Long> findIdsByStateSeenBeforeAndPasswordIsNullAndPawnsIsEmpty(LocalDateTime seenBefore);

    @Query("select cast(count(1) as boolean) " +
            "from User u " +
            "where u.human and ((u.state.destinationServer = ?1 and u.state.navigated > ?2) or (u.state.server = ?1))")
    boolean existsByHumanTrueAnd_DestinationServerAndNavigatedAfterOrServer_(ServerEntity server, LocalDateTime navigatedAfter);

    @Modifying
    @Query("update User u set u.version = u.version + 1, u.score = u.score + ?1 where u.id = ?2")
    void updateScoreAddById(BigDecimal scoreAdd, Long id);

    @Modifying
    @Query("update User u " +
            "set u.version = u.version + 1, " +
            "    u.score = u.score + (case u.human when true then ?1 else ?2 end) " +
            "where u.faction.id = ?3 and (select us.seen from u.state us) > ?4")
    void updateScoreAddByFactionIdAndStateSeenAfter(BigDecimal scoreToAddForHumans, BigDecimal scoreToAddForNonHumans, Long factionId, LocalDateTime seenAfter);

    @Modifying
    @Query("update User u set u.version = u.version + 1, u.score = u.score * ?1")
    void updateScoreMultiply(BigDecimal scoreMultiply);
}
