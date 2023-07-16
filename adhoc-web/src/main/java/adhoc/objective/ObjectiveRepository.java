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

package adhoc.objective;

import adhoc.faction.Faction;
import adhoc.region.Region;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface ObjectiveRepository extends JpaRepository<Objective, Long> {

    int countByFaction(Faction faction);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Objective getObjectiveById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Objective getObjectiveByRegionAndIndex(Region region, Integer index);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Objective> findObjectiveByRegionAndIndex(Region region, Integer index);

    @Transactional
    @Modifying
    @Query("update Objective o set o.version = o.version + 1, o.area = null where o.region = :region and o.area.id not in :areaIds")
    void updateObjectivesSetAreaNullByRegionAndAreaIdNotIn(@Param("region") Region region, @Param("areaIds") Collection<Long> areaIds);

    //@Lock(LockModeType.PESSIMISTIC_WRITE)
    //Stream<Objective> streamObjectivesByRegionAndAreaIdNotIn(Region region, Collection<Long> areaIds);
}
