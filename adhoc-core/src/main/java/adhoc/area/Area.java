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

package adhoc.area;

import adhoc.objective.Objective;
import adhoc.region.Region;
import adhoc.server.Server;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

/**
 * An area within a region (a region is map/level in the Unreal project).
 * There may be more than one area per region. When users navigate from one area to another area,
 * they may have to connect to a new server unless the same server manages both the areas.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "area__region_id__index", columnNames = {"region_id", "index"}))
//@DynamicInsert
//@DynamicUpdate
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
public class Area {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "AreaIdSequence")
    @SequenceGenerator(name = "AreaIdSequence", initialValue = 1, allocationSize = 50)
    @ToString.Include
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Region region;

    @Column(nullable = false)
    private Integer index;

    @Column(nullable = false)
    @ToString.Include
    private String name;

    @Column(nullable = false)
    private Double x;
    @Column(nullable = false)
    private Double y;
    @Column(nullable = false)
    private Double z;

    @Column(nullable = false)
    private Double sizeX;
    @Column(nullable = false)
    private Double sizeY;
    @Column(nullable = false)
    private Double sizeZ;

    @OneToMany(mappedBy = "area")
    private List<Objective> objectives;

    /** Server currently representing this area. */
    @ManyToOne(fetch = FetchType.LAZY)
    private Server server;
}
