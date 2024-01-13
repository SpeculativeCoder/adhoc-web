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

package adhoc.pawn;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;
import java.util.UUID;

@Value
@AllArgsConstructor
@Builder(toBuilder = true)
@Jacksonized
public class PawnDto {

    @Min(1)
    private Long id;

    @Min(0)
    private Long version;

    @NotNull
    private UUID uuid;

    @NotNull
    @Min(1)
    private Long serverId;

    @NotNull
    @Min(0)
    private Integer index;

    @NotNull
    private String name;

    @NotNull
    private String description;

    @NotNull
    private Float x;
    @NotNull
    private Float y;
    @NotNull
    private Float z;

    @Min(1)
    private Long userId;

    @NotNull
    private Boolean human;

    @Min(1)
    private Long factionId;

    @Min(0)
    private Integer factionIndex;

    private LocalDateTime seen;
}
