/*
 * Copyright (c) 2022-2023 SpeculativeCoder (https://github.com/SpeculativeCoder)
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
import adhoc.server.Server;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * A user account can be created by registering via the web application. This allows the user to login again later to the same account.
 * A user is also automatically created on joining an Unreal server if there was no logged-in user.
 */
@Entity(name = "AdhocUser")
@SequenceGenerator(name = "UserIdSequence", initialValue = 1, allocationSize = 1)
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
public class User implements UserDetails {

    @Id
    @GeneratedValue(generator = "UserIdSequence")
    @ToString.Include
    private Long id;

    @Version
    @Basic(optional = false)
    private Long version;

    @Basic(optional = false)
    @ToString.Include
    private String name;

    private String email;

    @ToString.Exclude
    private String password;

    @ManyToOne(optional = false)
    private Faction faction;

    @Basic(optional = false)
    private Boolean bot;

    @Basic(optional = false)
    private Float score;

    private Float x;
    private Float y;
    private Float z;

    private Float pitch;
    private Float yaw;

    @CreationTimestamp
    @Basic(optional = false)
    private LocalDateTime created;

    @UpdateTimestamp
    @Basic(optional = false)
    private LocalDateTime updated;

    private LocalDateTime lastLogin;

    private LocalDateTime lastJoin;

    private LocalDateTime seen;

    @ElementCollection(fetch = FetchType.EAGER) // TODO: can we make the login success handler transactional?
    private Set<UserRole> roles;

    private UUID token;

    @ManyToOne
    private Server server;

    @OneToMany(mappedBy = "user")
    private List<Pawn> pawns;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream().map(role -> "ROLE_" + role.name()).map(SimpleGrantedAuthority::new).collect(Collectors.toSet());
    }

    @Override
    public String getUsername() {
        return name;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return password != null;
    }

}
