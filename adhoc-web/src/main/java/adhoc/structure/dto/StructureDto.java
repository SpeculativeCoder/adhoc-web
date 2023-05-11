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

package adhoc.structure.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(includeFieldNames = false)
public class StructureDto {

    @Min(1)
    private Long id;

    @Min(0)
    private Long version;

    @NotNull
    private UUID uuid;

    @NotEmpty
    private String name;

    @NotEmpty
    private String type;

    @NotNull
    @Min(1)
    private Long regionId;

    @NotNull
    private Float x;
    @NotNull
    private Float y;
    @NotNull
    private Float z;

    @NotNull
    private Float pitch;
    @NotNull
    private Float yaw;
    @NotNull
    private Float roll;

    @NotNull
    @Positive
    private Float scaleX;
    @NotNull
    @Positive
    private Float scaleY;
    @NotNull
    @Positive
    private Float scaleZ;

    @NotNull
    @Positive
    private Float sizeX;
    @NotNull
    @Positive
    private Float sizeY;
    @NotNull
    @Positive
    private Float sizeZ;

    @Min(1)
    private Long factionId;
    @Min(0)
    private Integer factionIndex;

    @Min(1)
    private Long userId;
}
