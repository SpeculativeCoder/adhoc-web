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

package adhoc.area;

import adhoc.objective.ObjectiveEntity;
import adhoc.region.RegionEntity;
import adhoc.server.ServerEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;

/**
 * Area within a region (a region is map/level in the Unreal project).
 * There may be more than one area per region. When users navigate from one area to another area,
 * they may have to connect to a new server unless the same server manages both the areas.
 */
@Entity(name = "Area")
@Table(uniqueConstraints = @UniqueConstraint(name = "uc_area_region_id_index", columnNames = {"region_id", "index"}))
//@DynamicInsert
//@DynamicUpdate
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class AreaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "AreaIdSequence")
    @SequenceGenerator(name = "AreaIdSequence", initialValue = 1, allocationSize = 1)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @ToString.Exclude
    private RegionEntity region;

    @Column(nullable = false)
    private Integer index;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private BigDecimal x;
    @Column(nullable = false)
    private BigDecimal y;
    @Column(nullable = false)
    private BigDecimal z;

    @Column(nullable = false)
    private BigDecimal sizeX;
    @Column(nullable = false)
    private BigDecimal sizeY;
    @Column(nullable = false)
    private BigDecimal sizeZ;

    @OneToMany(mappedBy = "area")
    @ToString.Exclude
    private List<ObjectiveEntity> objectives;

    /** Server currently representing this area. */
    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    private ServerEntity server;
}
