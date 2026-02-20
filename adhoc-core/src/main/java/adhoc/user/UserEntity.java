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

package adhoc.user;

import adhoc.faction.FactionEntity;
import adhoc.pawn.PawnEntity;
import adhoc.user.state.UserStateEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A user account can be created by registering via the web application. This allows the user to login again later to
 * the same account. A user is also automatically created on joining an Unreal server if there was no logged-in user.
 */
@Entity(name = "User")
@Table(name = "user_", indexes = {
        @Index(name = "idx_user_email", columnList = "email"),
        @Index(name = "idx_user_score", columnList = "score"),
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
    @SequenceGenerator(name = "UserIdSequence", initialValue = 1, allocationSize = 1)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    @PrimaryKeyJoinColumn
    private UserStateEntity state;

    @Column(nullable = false, unique = true)
    private String name;

    private String email;

    @ToString.Exclude
    @Setter(AccessLevel.NONE)
    private String password;

    @ToString.Exclude
    @Setter(AccessLevel.NONE)
    private String quickLoginPassword;

    @Column(nullable = false)
    private boolean human;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @ToString.Exclude
    private FactionEntity faction;

    @Column(nullable = false)
    private BigDecimal score;

    @Column(nullable = false)
    private String roles;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime created;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updated;

    private LocalDateTime lastLogin;

    @OneToMany(mappedBy = "user")
    @ToString.Exclude
    private List<PawnEntity> pawns;

    public UserStateEntity getState() {
        if (state == null) {
            state = new UserStateEntity();
            state.setUser(this);
        }
        return state;
    }

    public void setUserRoles(Set<UserRole> userRoles) {
        roles = userRoles.stream()
                .map(UserRole::name)
                .collect(Collectors.joining(","));
    }

    public Set<UserRole> getUserRoles() {
        return Arrays.stream(roles.split(",", -1))
                .map(UserRole::valueOf)
                .collect(Collectors.toCollection(HashSet::new));
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return getUserRoles().stream()
                .map(role -> "ROLE_" + role.name())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toUnmodifiableSet());
    }

    public void setPassword(String password, PasswordEncoder passwordEncoder) {
        this.password = password == null ? null : passwordEncoder.encode(password);
    }


    public String getQuickLoginPassword(String quickLoginPasswordEncryptionKey) {

        if (quickLoginPasswordEncryptionKey == null) {
            return null;

        } else {
            String quickLoginPasswordEncryptionSalt = quickLoginPassword.substring(0, 16);
            TextEncryptor textEncryptor = Encryptors.text(quickLoginPasswordEncryptionKey, quickLoginPasswordEncryptionSalt);

            return textEncryptor.decrypt(quickLoginPassword.substring(quickLoginPasswordEncryptionSalt.length()));
        }
    }

    public void setQuickLoginPassword(String quickLoginPassword, String quickLoginPasswordEncryptionKey) {

        if (quickLoginPassword == null) {
            this.quickLoginPassword = null;

        } else {
            //new String(Hex.encode(KeyGenerators.secureRandom().generateKey()), StandardCharsets.UTF_8);
            String quickLoginPasswordSalt = KeyGenerators.string().generateKey();
            TextEncryptor textEncryptor = Encryptors.text(quickLoginPasswordEncryptionKey, quickLoginPasswordSalt);

            this.quickLoginPassword = quickLoginPasswordSalt + textEncryptor.encrypt(quickLoginPassword);
        }
    }
}
