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

package adhoc.server;

import adhoc.area.AreaEntity;
import adhoc.pawn.PawnEntity;
import adhoc.region.RegionEntity;
import adhoc.user.UserStateEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A server is assigned to represent one or more areas of a region.
 * When a server is enabled, a server task (typically in the cloud) should be launched.
 * Once the server task is running, the server is considered active and users can navigate to it.
 * A server which is not enabled will have any associated task torn down and will be eventually purged.
 */
@Entity(name = "Server")
//@DynamicInsert
//@DynamicUpdate
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class ServerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ServerIdSequence")
    @SequenceGenerator(name = "ServerIdSequence", initialValue = 1, allocationSize = 1)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @ToString.Exclude
    private RegionEntity region;

    @OneToMany(mappedBy = "server")
    @ToString.Exclude
    private List<AreaEntity> areas;

    @Column(nullable = false)
    private String mapName;

    private BigDecimal x;
    private BigDecimal y;
    private BigDecimal z;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private boolean active;

    private String publicIp;

    private Integer publicWebSocketPort;

    private String domain;

    private String webSocketUrl;

    private LocalDateTime initiated;

    private LocalDateTime started;

    private LocalDateTime stopped;

    private LocalDateTime seen;

    @OneToMany(mappedBy = "destinationServer")
    @ToString.Exclude
    private List<UserStateEntity> destinedUserStates;

    @OneToMany(mappedBy = "server")
    @ToString.Exclude
    private List<UserStateEntity> userStates;

    @OneToMany(mappedBy = "server")
    @ToString.Exclude
    private List<PawnEntity> pawns;
}
