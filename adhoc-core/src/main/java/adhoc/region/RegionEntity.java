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

package adhoc.region;

import adhoc.area.AreaEntity;
import adhoc.server.ServerEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;

/**
 * Region is a map/level in the Unreal project.
 * Users connecting via HTML5 client will only have the content for one region available,
 * but can move freely from area to area within the region (which may sometimes mean joining different servers)
 * without having to download the HTML5 client again.
 */
@Entity(name = "Region")
//@DynamicInsert
//@DynamicUpdate
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class RegionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "RegionIdSequence")
    @SequenceGenerator(name = "RegionIdSequence", initialValue = 1, allocationSize = 50)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false)
    private String name;

    /** Name of map/level in the Unreal project. */
    @Column(nullable = false)
    private String mapName;

    @Column(precision = 128, scale = 64, nullable = false)
    private BigDecimal x;
    @Column(precision = 128, scale = 64, nullable = false)
    private BigDecimal y;
    @Column(precision = 128, scale = 64, nullable = false)
    private BigDecimal z;

    @OneToMany(mappedBy = "region")
    @ToString.Exclude
    private List<AreaEntity> areas;

    @OneToMany(mappedBy = "region")
    @ToString.Exclude
    private List<ServerEntity> servers;
}
