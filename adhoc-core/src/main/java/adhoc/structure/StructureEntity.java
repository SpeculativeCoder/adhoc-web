/*
 * Copyright (c) 2022-2025 SpeculativeCoder (https://github.com/SpeculativeCoder)
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

package adhoc.structure;

import adhoc.faction.FactionEntity;
import adhoc.region.RegionEntity;
import adhoc.user.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Structures are objects that are pre-placed or that users have placed in the world e.g. barriers etc.
 */
@Entity(name = "Structure")
//@DynamicInsert
//@DynamicUpdate
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class StructureEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "StructureIdSequence")
    @SequenceGenerator(name = "StructureIdSequence", initialValue = 1, allocationSize = 1)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false)
    private UUID uuid;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @ToString.Exclude
    private RegionEntity region;

    @Column(nullable = false)
    private BigDecimal x;
    @Column(nullable = false)
    private BigDecimal y;
    @Column(nullable = false)
    private BigDecimal z;

    @Column(nullable = false)
    private BigDecimal pitch;
    @Column(nullable = false)
    private BigDecimal yaw;
    @Column(nullable = false)
    private BigDecimal roll;

    @Column(nullable = false)
    private BigDecimal scaleX;
    @Column(nullable = false)
    private BigDecimal scaleY;
    @Column(nullable = false)
    private BigDecimal scaleZ;

    @Column(nullable = false)
    private BigDecimal sizeX;
    @Column(nullable = false)
    private BigDecimal sizeY;
    @Column(nullable = false)
    private BigDecimal sizeZ;

    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    private FactionEntity faction;

    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    private UserEntity user;
}
