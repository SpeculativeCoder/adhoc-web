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
import adhoc.user.User;
import adhoc.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

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
        LocalDateTime now = LocalDateTime.now();

        Set<Long> seenPawnIds = new HashSet<>();
        for (PawnDto pawnDto : serverPawnsEvent.getPawns()) {

            User user = pawnDto.getUserId() == null ? null : userRepository.getUserById(pawnDto.getUserId());
            if (user != null) {
                user.setServer(server);
                user.setSeen(now);
            }

            Pawn pawn = toEntity(pawnDto, now, server, user);

            pawn = pawnRepository.save(pawn);
            seenPawnIds.add(pawn.getId());
        }

        // clean up anything we didn't update for this server
        pawnRepository.deleteByServerAndIdNotIn(server, seenPawnIds);
    }

    public void purgeOldPawns() {
        log.trace("Purging old pawns...");

        try (Stream<Pawn> pawnsToDelete = pawnRepository.streamPawnsBySeenBeforeOrderById(LocalDateTime.now().minusMinutes(1))) {
            pawnsToDelete
                    .forEach(oldPawn -> {
                        log.debug("Purging old pawn: oldPawn={}", oldPawn);
                        pawnRepository.delete(oldPawn);
                    });
        }
    }

    Pawn toEntity(PawnDto pawnDto, LocalDateTime seen, Server server, User user) {
        Pawn pawn = pawnRepository.findPawnByServerAndIndex(server, pawnDto.getIndex()).orElseGet(Pawn::new);

        pawn.setName(pawnDto.getName());
        pawn.setServer(server);
        pawn.setIndex(pawnDto.getIndex());
        pawn.setFaction(factionRepository.getByIndex(pawnDto.getFactionIndex()));
        pawn.setX(pawnDto.getX());
        pawn.setY(pawnDto.getY());
        pawn.setZ(pawnDto.getZ());
        pawn.setSeen(seen);
        pawn.setUser(user);

        return pawn;
    }
}
