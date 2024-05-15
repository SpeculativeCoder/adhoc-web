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

package adhoc.user;

import adhoc.server.Server;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByName(String name);

    Optional<User> findByNameOrEmail(String name, String email);

    Optional<User> findFirstByHumanFalseAndFactionIdAndSeenBefore(Long factionId, LocalDateTime seenBefore);

    Stream<User> streamByServerNotNullAndSeenBefore(LocalDateTime seenBefore);

    @Query("select u.id from AdhocUser u where u.created < ?1 and u.seen is null and u.password is null and u.pawns is empty")
    List<Long> findIdsByCreatedBeforeAndSeenIsNullAndPasswordIsNullAndPawnsIsEmpty(LocalDateTime createdBefore);

    @Query("select u.id from AdhocUser u where u.seen < ?1 and u.password is null and u.pawns is empty")
    List<Long> findIdsBySeenBeforeAndPasswordIsNullAndPawnsIsEmpty(LocalDateTime seenBefore);

    @Modifying
    @Query("update AdhocUser u set u.version = u.version + 1, u.server = ?1, u.seen = ?2 where u.id in ?3")
    void updateServerAndSeenByIdIn(Server server, LocalDateTime seen, Collection<Long> idIn);

    @Modifying
    @Query("update AdhocUser u set u.version = u.version + 1, u.score = u.score + ?1 where u.id = ?2")
    void updateScoreAddById(BigDecimal scoreAdd, Long id);

    @Modifying
    @Query("update AdhocUser u " +
            "set u.version = u.version + 1, " +
            "    u.score = u.score + (case u.human when true then ?1 else ?2 end) " +
            "where u.faction.id = ?3 and u.seen > ?4")
    void updateScoreAddByFactionIdAndSeenAfter(BigDecimal humanScoreAdd, BigDecimal notHumanScoreAdd, Long factionId, LocalDateTime seenAfter);

    @Modifying
    @Query("update AdhocUser u set u.version = u.version + 1, u.score = u.score * ?1 where u.seen < ?2")
    void updateScoreMultiply(BigDecimal scoreMultiply, LocalDateTime seenBefore);
}
