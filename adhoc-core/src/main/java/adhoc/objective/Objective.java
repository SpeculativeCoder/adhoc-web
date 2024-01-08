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

package adhoc.objective;

import adhoc.area.Area;
import adhoc.faction.Faction;
import adhoc.region.Region;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

/**
 * An objective that can be taken by a faction.
 */
@Entity
@SequenceGenerator(name = "ObjectiveIdSequence", initialValue = 1, allocationSize = 50)
@Table(uniqueConstraints = @UniqueConstraint(name = "objective__region_id__index", columnNames = {"region_id", "index"}))
//@DynamicInsert
//@DynamicUpdate
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
public class Objective {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ObjectiveIdSequence")
    @ToString.Include
    private Long id;

    @Version
    @Basic(optional = false)
    private Long version;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Region region;

    @Basic(optional = false)
    private Integer index;

    @Basic(optional = false)
    @ToString.Include
    private String name;

    @Basic(optional = false)
    private Float x;
    @Basic(optional = false)
    private Float y;
    @Basic(optional = false)
    private Float z;

    @ManyToOne(fetch = FetchType.LAZY)
    private Faction initialFaction;
    @ManyToOne(fetch = FetchType.LAZY)
    private Faction faction;

    @ManyToMany(fetch = FetchType.LAZY)
    private Set<Objective> linkedObjectives;

    @ManyToOne(fetch = FetchType.LAZY)
    private Area area;
}
