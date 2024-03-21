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

package adhoc.region;

import adhoc.area.Area;
import adhoc.server.Server;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

/**
 * A region corresponds to a map in the Unreal project.
 * Users connecting via HTML5 client will only have the content for one region available,
 * but can move freely from area to area within the region (which may sometimes mean joining different servers)
 * without having to download the HTML5 client again.
 */
@Entity
//@DynamicInsert
//@DynamicUpdate
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class Region {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "RegionIdSequence")
    @SequenceGenerator(name = "RegionIdSequence", initialValue = 1, allocationSize = 50)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false)
    private String name;

    /** Name of map/level in Unreal project. */
    @Column(nullable = false)
    private String mapName;

    @Column(nullable = false)
    private Double x;
    @Column(nullable = false)
    private Double y;
    @Column(nullable = false)
    private Double z;

    @OneToMany(mappedBy = "region")
    @ToString.Exclude
    private List<Area> areas;

    @OneToMany(mappedBy = "region")
    @ToString.Exclude
    private List<Server> servers;
}
