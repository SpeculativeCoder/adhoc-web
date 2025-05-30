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

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Value
@AllArgsConstructor
@Builder(toBuilder = true)
@Jacksonized
public class UserFullDto {

    @NotNull
    @Min(1)
    Long id;

    @Min(0)
    Long version;

    @NotEmpty
    String name;

    @NotNull
    Boolean human;

    @NotNull
    @Min(1)
    Long factionId;

    @NotNull
    BigDecimal score;

    @NotNull
    @Min(1)
    Long regionId;

    @NotNull
    BigDecimal x;
    @NotNull
    BigDecimal y;
    @NotNull
    BigDecimal z;

    @NotNull
    BigDecimal pitch;
    @NotNull
    BigDecimal yaw;

    @NotNull
    LocalDateTime created;
    @NotNull
    LocalDateTime updated;
    LocalDateTime lastLogin;
    LocalDateTime navigated;
    LocalDateTime lastJoin;
    LocalDateTime seen;

    @NotNull
    @Size(min = 1)
    List<String> roles;

    @NotEmpty
    String token;

    @Min(1)
    Long destinationServerId;

    @Min(1)
    Long serverId;
}
