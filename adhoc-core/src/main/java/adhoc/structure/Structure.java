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

import adhoc.faction.Faction;
import adhoc.region.Region;
import adhoc.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Structures are objects that are pre-placed or that users have placed in the world e.g. barriers etc.
 */
@Entity
//@DynamicInsert
//@DynamicUpdate
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class Structure {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "StructureIdSequence")
    @SequenceGenerator(name = "StructureIdSequence", initialValue = 1, allocationSize = 50)
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
    private Region region;

    @Column(precision = 128, scale = 64, nullable = false)
    private BigDecimal x;
    @Column(precision = 128, scale = 64, nullable = false)
    private BigDecimal y;
    @Column(precision = 128, scale = 64, nullable = false)
    private BigDecimal z;

    @Column(precision = 128, scale = 64, nullable = false)
    private BigDecimal pitch;
    @Column(precision = 128, scale = 64, nullable = false)
    private BigDecimal yaw;
    @Column(precision = 128, scale = 64, nullable = false)
    private BigDecimal roll;

    @Column(precision = 128, scale = 64, nullable = false)
    private BigDecimal scaleX;
    @Column(precision = 128, scale = 64, nullable = false)
    private BigDecimal scaleY;
    @Column(precision = 128, scale = 64, nullable = false)
    private BigDecimal scaleZ;

    @Column(precision = 128, scale = 64, nullable = false)
    private BigDecimal sizeX;
    @Column(precision = 128, scale = 64, nullable = false)
    private BigDecimal sizeY;
    @Column(precision = 128, scale = 64, nullable = false)
    private BigDecimal sizeZ;

    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    private Faction faction;

    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    private User user;
}
