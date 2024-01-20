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

import adhoc.faction.FactionRepository;
import adhoc.pawn.event.ServerPawnsEvent;
import adhoc.server.Server;
import adhoc.server.ServerRepository;
import adhoc.user.UserRepository;
import com.google.common.base.Preconditions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Transactional
@Service
@Slf4j
@RequiredArgsConstructor
public class ManagerPawnService {

    private final PawnRepository pawnRepository;
    private final UserRepository userRepository;
    private final FactionRepository factionRepository;
    private final ServerRepository serverRepository;

    private final PawnService pawnService;

    Pawn toEntity(PawnDto pawnDto, Pawn pawn) {
        pawn.setUuid(pawnDto.getUuid());
        pawn.setServer(serverRepository.getReferenceById(pawnDto.getServerId()));
        pawn.setIndex(pawnDto.getIndex());
        pawn.setName(pawnDto.getName());
        pawn.setDescription(pawnDto.getDescription());
        pawn.setX(pawnDto.getX());
        pawn.setY(pawnDto.getY());
        pawn.setZ(pawnDto.getZ());
        pawn.setUser(pawnDto.getUserId() == null ? null : userRepository.getReferenceById(pawnDto.getUserId()));
        pawn.setHuman(pawnDto.getHuman());
        pawn.setFaction(pawnDto.getFactionIndex() == null ? null : factionRepository.getByIndex(pawnDto.getFactionIndex()));
        pawn.setSeen(pawnDto.getSeen());

        return pawn;
    }

    @Retryable(retryFor = {ObjectOptimisticLockingFailureException.class, PessimisticLockingFailureException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 100, maxDelay = 500))
    public List<PawnDto> handleServerPawns(ServerPawnsEvent serverPawnsEvent) {
        LocalDateTime seen = LocalDateTime.now();

        Server server = serverRepository.getReferenceById(serverPawnsEvent.getServerId());

        // update users seen
        Set<Long> userIds = serverPawnsEvent.getPawns().stream()
                .map(PawnDto::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));
        userRepository.updateServerAndSeenByIdIn(server, seen, userIds);

        // clean up any pawns that are no longer on this server
        List<UUID> uuids = serverPawnsEvent.getPawns().stream()
                .map(PawnDto::getUuid)
                .toList();
        pawnRepository.deleteByServerAndUuidNotIn(server, uuids);

        return serverPawnsEvent.getPawns().stream().map(pawnDto -> {
            Preconditions.checkArgument(Objects.equals(server.getId(), pawnDto.getServerId()));

            Pawn pawn = toEntity(pawnDto,
                    pawnRepository.findByServerAndUuid(server, pawnDto.getUuid()).orElseGet(Pawn::new));

            pawn.setSeen(seen); // TODO

            return pawnService.toDto(pawnRepository.save(pawn));
        }).toList();
    }

    @Retryable(retryFor = {ObjectOptimisticLockingFailureException.class, PessimisticLockingFailureException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 500, maxDelay = 2000))
    public void purgeOldPawns() {
        log.trace("Purging old pawns...");

        pawnRepository.deleteBySeenBefore(LocalDateTime.now().minusMinutes(5));
    }
}
