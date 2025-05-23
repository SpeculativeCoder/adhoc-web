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

import adhoc.faction.Faction;
import adhoc.pawn.Pawn;
import adhoc.region.Region;
import adhoc.server.Server;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * A user account can be created by registering via the web application. This allows the user to login again later to
 * the same account. A user is also automatically created on joining an Unreal server if there was no logged-in user.
 */
@Entity
@Table(name = "adhoc_user", indexes = {
        @Index(name = "idx_user_name", columnList = "name"),
        @Index(name = "idx_user_email", columnList = "email"),
        @Index(name = "idx_user_faction_id", columnList = "faction_id"),
        @Index(name = "idx_user_region_id", columnList = "region_id"),
        @Index(name = "idx_user_created", columnList = "created"),
        @Index(name = "idx_user_seen", columnList = "seen"),
        @Index(name = "idx_user_server_id", columnList = "server_id")
})
// TODO: unique constraint(s)
//@DynamicInsert
//@DynamicUpdate
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "UserIdSequence")
    @SequenceGenerator(name = "UserIdSequence", initialValue = 1, allocationSize = 50)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false)
    private String name;

    private String email;

    @ToString.Exclude
    private String password;

    @Column(nullable = false)
    private boolean human;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @ToString.Exclude
    private Faction faction;

    @Column(precision = 128, scale = 64, nullable = false)
    private BigDecimal score;

    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    private Region region;

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

    private LocalDateTime lastLogin;

    private LocalDateTime navigated;

    private LocalDateTime lastJoin;

    private LocalDateTime seen;

    @ElementCollection(fetch = FetchType.EAGER) // TODO: can we make the login success handler transactional?
    @CollectionTable(name = "adhoc_user_roles")
    @Enumerated(EnumType.STRING)
    private Set<UserRole> roles;

    private UUID token;

    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    private Server destinationServer;

    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    private Server server;

    @OneToMany(mappedBy = "user")
    @ToString.Exclude
    private List<Pawn> pawns;

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> "ROLE_" + role.name())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toUnmodifiableSet());
    }
}
