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

package adhoc.area;

import adhoc.objective.Objective;
import adhoc.region.Region;
import adhoc.server.Server;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.util.List;

/**
 * An area within a region (a region is map in the Unreal project).
 * There may be more than one area per region. When users navigate from one area to another area,
 * they may have to connect to a new server unless the same server manages both the areas.
 */
@Entity
@SequenceGenerator(name = "AreaIdSequence", initialValue = 1, allocationSize = 1)
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true, includeFieldNames = false)
public class Area {

    @Id
    @GeneratedValue(generator = "AreaIdSequence")
    @ToString.Include
    private Long id;

    @Version
    @Basic(optional = false)
    private Long version;

    @ManyToOne(optional = false)
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

    @Basic(optional = false)
    private Float sizeX;
    @Basic(optional = false)
    private Float sizeY;
    @Basic(optional = false)
    private Float sizeZ;

    @OneToMany(mappedBy = "area")
    private List<Objective> objectives;

    /**
     * Server currently representing this area.
     */
    @ManyToOne
    private Server server;
}
