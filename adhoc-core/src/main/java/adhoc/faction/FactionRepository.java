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

package adhoc.faction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;

public interface FactionRepository extends JpaRepository<Faction, Long> {

    Faction getByIndex(Integer index);

    @Modifying
    // TODO
    //@Query("update Faction f set f.version = f.version + 1, f.score = f.score + ?1 where f.id = ?2")
    @Query(nativeQuery = true, value = "update faction f set f.version = f.version + 1, f.score = f.score + ?1 where f.id = ?2")
    void updateScoreAddById(BigDecimal scoreAdd, Long factionId);

    @Modifying
    // TODO
    //@Query("update Faction f set f.version = f.version + 1, f.score = f.score * ?1")
    @Query(nativeQuery = true, value = "update faction f set f.version = f.version + 1, f.score = f.score * ?1")
    void updateScoreMultiply(BigDecimal scoreMultiply);
}
