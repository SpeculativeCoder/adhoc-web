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

import adhoc.faction.FactionRepository;
import adhoc.server.Server;
import adhoc.server.ServerRepository;
import adhoc.server.event.ServerPawnsEvent;
import adhoc.user.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.TreeSet;

@Transactional
@Service
@Slf4j
@RequiredArgsConstructor
public class ManagerPawnService {

    private final PawnRepository pawnRepository;
    private final UserRepository userRepository;
    private final FactionRepository factionRepository;
    private final ServerRepository serverRepository;

    public void handleServerPawns(ServerPawnsEvent serverPawnsEvent) {
        Server server = serverRepository.getReferenceById(serverPawnsEvent.getServerId());

        LocalDateTime seen = LocalDateTime.now();

        Set<Long> seenUserIds = new TreeSet<>();

        for (PawnDto pawnDto : serverPawnsEvent.getPawns()) {
            if (pawnDto.getUserId() != null) {
                seenUserIds.add(pawnDto.getUserId());
            }
        }

        if (!seenUserIds.isEmpty()) {
            userRepository.updateUsersServerAndSeenByIdIn(server, seen, seenUserIds);
        }

        serverRepository.flush();

        Set<Long> seenPawnIds = new TreeSet<>();

        for (PawnDto pawnDto : serverPawnsEvent.getPawns()) {
            // TODO
            pawnDto.setServerId(server.getId());
            pawnDto.setSeen(seen);

            Pawn pawn = pawnRepository.save(
                    toEntity(pawnDto, pawnRepository.findPawnByUuid(pawnDto.getUuid()).orElseGet(Pawn::new)));

            seenPawnIds.add(pawn.getId());
        }

        pawnRepository.flush();

        // clean up any pawns we didn't update for this server
        if (!seenPawnIds.isEmpty() && pawnRepository.existsByServerAndIdNotIn(server, seenPawnIds)) {
            pawnRepository.deleteByServerAndIdNotIn(server, seenPawnIds);
        }
    }

    public void purgeOldPawns() {
        log.trace("Purging old pawns...");

        LocalDateTime oldPawnsDateTime = LocalDateTime.now().minusMinutes(5);

        if (pawnRepository.existsBySeenBefore(oldPawnsDateTime)) {
            pawnRepository.deleteBySeenBefore(oldPawnsDateTime);
        }
    }

    Pawn toEntity(PawnDto pawnDto, Pawn pawn) {
        pawn.setUuid(pawnDto.getUuid());
        pawn.setServer(serverRepository.getReferenceById(pawnDto.getServerId()));
        pawn.setIndex(pawnDto.getIndex());
        pawn.setName(pawnDto.getName());
        pawn.setFaction(factionRepository.getByIndex(pawnDto.getFactionIndex()));
        pawn.setX(pawnDto.getX());
        pawn.setY(pawnDto.getY());
        pawn.setZ(pawnDto.getZ());
        pawn.setSeen(pawnDto.getSeen());
        pawn.setUser(pawnDto.getUserId() == null ? null : userRepository.getReferenceById(pawnDto.getUserId()));

        return pawn;
    }
}
