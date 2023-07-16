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

package adhoc.pawn;

import adhoc.faction.Faction;
import adhoc.server.Server;
import adhoc.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A pawn is either a bot or user on an Unreal server.
 * Pawns are not updated regularly so only gives a recent location
 * (intended for an "at a glance" location of bots/users in the world).
 */
@Entity
@DynamicInsert
@DynamicUpdate
@SequenceGenerator(name = "PawnIdSequence", initialValue = 1, allocationSize = 1)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true, includeFieldNames = false)
public class Pawn {

    @Id
    @GeneratedValue(generator = "PawnIdSequence")
    @ToString.Include
    private Long id;

    @Version
    @Basic(optional = false)
    private Long version;

    @Basic(optional = false)
    private UUID uuid;

    @ManyToOne(optional = false)
    private Server server;

    @Basic(optional = false)
    private Integer index;

    @Basic(optional = false)
    @ToString.Include
    private String name;

    @Basic(optional = false)
    private Float x;
    @Basic(optional = false)
    private Float y;
    @Basic(optional = false)
    private Float z;

    @ManyToOne(optional = false)
    private Faction faction;

    @Basic(optional = false)
    private LocalDateTime seen;

    @ManyToOne
    private User user;
}
