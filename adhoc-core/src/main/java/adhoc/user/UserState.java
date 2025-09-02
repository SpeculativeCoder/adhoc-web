package adhoc.user;

import adhoc.region.Region;
import adhoc.server.Server;
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
 * Long term important information about the user (login details, score etc.) should not be kept in here and should be elsewhere i.e. in {@link adhoc.user.User}
 */
@Entity
@Table(name = "adhoc_user_state", indexes = {
        @Index(name = "idx_user_region_id", columnList = "region_id"),
        @Index(name = "idx_user_created", columnList = "created"),
        @Index(name = "idx_user_seen", columnList = "seen"),
        @Index(name = "idx_user_server_id", columnList = "server_id")
})
//@DynamicInsert
//@DynamicUpdate
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class UserState {

    @Id
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    private User user;

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

    private LocalDateTime seen;

    private UUID token;

    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    private Server destinationServer;

    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    private Server server;
}
