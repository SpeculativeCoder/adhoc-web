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
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Transactional
@Service
@Slf4j
@RequiredArgsConstructor
public class PawnManagerService {

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
        pawn.setPitch(pawnDto.getPitch());
        pawn.setYaw(pawnDto.getYaw());
        pawn.setUser(pawnDto.getUserId() == null ? null : userRepository.getReferenceById(pawnDto.getUserId()));
        pawn.setHuman(pawnDto.getHuman());
        pawn.setFaction(pawnDto.getFactionId() == null ? null : factionRepository.getReferenceById(pawnDto.getFactionId()));

        return pawn;
    }

    @Retryable(retryFor = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 100, maxDelay = 1000))
    public List<PawnDto> handleServerPawns(ServerPawnsEvent serverPawnsEvent) {
        LocalDateTime seen = LocalDateTime.now();

        Server server = serverRepository.getReferenceById(serverPawnsEvent.getServerId());

        List<UUID> pawnUuids = new ArrayList<>();
        List<Long> userIds = new ArrayList<>();
        List<PawnDto> pawnDtos = new ArrayList<>();

        for (PawnDto dto : serverPawnsEvent.getPawns()) {
            Preconditions.checkArgument(Objects.equals(server.getId(), dto.getServerId()));

            Pawn pawn = toEntity(dto,
                    pawnRepository.findByUuid(dto.getUuid()).orElseGet(Pawn::new));

            pawn.setSeen(seen);

            pawnUuids.add(pawn.getUuid());

            if (pawn.getUser() != null) {
                // TODO
                //pawn.getUser().setX(pawn.getX());
                //pawn.getUser().setY(pawn.getY());
                //pawn.getUser().setZ(pawn.getZ());
                //pawn.getUser().setPitch(pawn.getPitch());
                //pawn.getUser().setYaw(pawn.getYaw());
                //pawn.getUser().setServer(server);
                //pawn.getUser().setSeen(seen);

                userIds.add(pawn.getUser().getId());
            }

            if (pawn.getId() == null) {
                pawn = pawnRepository.save(pawn);
            }

            pawnDtos.add(pawnService.toDto(pawn));
        }

        // clean up any pawns that are no longer on this server
        pawnRepository.deleteByServerAndUuidNotIn(server, pawnUuids);

        // update users seen
        userRepository.updateServerAndSeenByIdIn(server, seen, userIds);

        return pawnDtos;
    }

    @Retryable(retryFor = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 100, maxDelay = 1000))
    public void purgeOldPawns() {
        log.trace("Purging old pawns...");

        pawnRepository.deleteBySeenBefore(LocalDateTime.now().minusMinutes(1));
    }
}
