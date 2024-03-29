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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Transactional
@Service
@Slf4j
@RequiredArgsConstructor
public class PawnService {

    private final PawnRepository pawnRepository;

    @Transactional(readOnly = true)
    public List<PawnDto> getPawns() {
        return pawnRepository.findAll(PageRequest.of(0, 100, Sort.Direction.ASC, "serverId", "index"))
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PawnDto getPawn(Long pawnId) {
        return toDto(pawnRepository.getReferenceById(pawnId));
    }

    PawnDto toDto(Pawn pawn) {
        return new PawnDto(
                pawn.getId(),
                pawn.getVersion(),
                pawn.getUuid(),
                pawn.getServer().getId(),
                pawn.getIndex(),
                pawn.getName(),
                pawn.getDescription(),
                pawn.getX(), pawn.getY(), pawn.getZ(),
                pawn.getUser() == null ? null : pawn.getUser().getId(),
                pawn.getHuman(),
                pawn.getFaction() == null ? null : pawn.getFaction().getId(),
                pawn.getFaction() == null ? null : pawn.getFaction().getIndex(),
                pawn.getSeen());
    }
}
