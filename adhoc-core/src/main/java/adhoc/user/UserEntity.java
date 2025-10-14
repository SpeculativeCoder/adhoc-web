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

import adhoc.faction.FactionEntity;
import adhoc.pawn.PawnEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A user account can be created by registering via the web application. This allows the user to login again later to
 * the same account. A user is also automatically created on joining an Unreal server if there was no logged-in user.
 */
@Entity(name = "User")
@Table(name = "adhoc_user", indexes = {
        @Index(name = "idx_user_name", columnList = "name"),
        @Index(name = "idx_user_email", columnList = "email"),
        @Index(name = "idx_user_faction_id", columnList = "faction_id"),
        @Index(name = "idx_user_created", columnList = "created")
})
// TODO: unique constraint(s)
//@DynamicInsert
//@DynamicUpdate
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "UserIdSequence")
    @SequenceGenerator(name = "UserIdSequence", initialValue = 1, allocationSize = 50)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    @PrimaryKeyJoinColumn
    private UserStateEntity state;

    @Column(nullable = false)
    private String name;

    private String email;

    @ToString.Exclude
    @Setter(AccessLevel.NONE)
    private String password;

    @Column(nullable = false)
    private boolean human;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @ToString.Exclude
    private FactionEntity faction;

    @Column(precision = 128, scale = 64, nullable = false)
    private BigDecimal score;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime created;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updated;

    private LocalDateTime lastLogin;

    private LocalDateTime navigated;

    private LocalDateTime lastJoin;

    @ElementCollection(fetch = FetchType.EAGER) // TODO: can we make the login success handler transactional?
    @CollectionTable(name = "adhoc_user_roles")
    @Enumerated(EnumType.STRING)
    private Set<Role> roles;

    @OneToMany(mappedBy = "user")
    @ToString.Exclude
    private List<PawnEntity> pawns;

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> "ROLE_" + role.name())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toUnmodifiableSet());
    }

    public void setPassword(String password, PasswordEncoder passwordEncoder) {
        this.password = password == null ? null : passwordEncoder.encode(password);
    }

    public enum Role {
        ADMIN, // access to administration functions / information (TODO)
        USER, // normal logged in user
        SERVER, // unreal server
        DEBUG, // see non-sensitive but noisy debug information
    }
}
