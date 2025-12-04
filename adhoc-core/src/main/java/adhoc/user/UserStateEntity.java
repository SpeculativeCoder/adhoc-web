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

package adhoc.user;

import adhoc.region.RegionEntity;
import adhoc.server.ServerEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * State of user in the world. Generally this changes a lot but is otherwise not important to keep so can be wiped/reset without too much consequence.
 * Long term important information about the user (login details, score etc.) should not be kept in here and should be elsewhere i.e. in {@link UserEntity}
 */
@Entity(name = "UserState")
@Table(indexes = {
        @Index(name = "idx_user_state_region_id", columnList = "region_id"),
        @Index(name = "idx_user_state_created", columnList = "created"),
        @Index(name = "idx_user_state_seen", columnList = "seen"),
        @Index(name = "idx_user_state_server_id", columnList = "server_id")
})
//@DynamicInsert
//@DynamicUpdate
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class UserStateEntity {

    @Id
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    @ToString.Exclude
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    private RegionEntity region;

    @Column(precision = 128, scale = 64)
    private BigDecimal x;
    @Column(precision = 128, scale = 64)
    private BigDecimal y;
    @Column(precision = 128, scale = 64)
    private BigDecimal z;

    @Column(precision = 128, scale = 64)
    private BigDecimal pitch;
    @Column(precision = 128, scale = 64)
    private BigDecimal yaw;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime created;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updated;

    private LocalDateTime seen;

    private UUID token;

    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    private ServerEntity destinationServer;

    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    private ServerEntity server;
}
