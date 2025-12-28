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

import adhoc.area.AreaEntity;
import adhoc.area.AreaRepository;
import adhoc.region.RegionEntity;
import adhoc.server.ServerEntity;
import adhoc.server.ServerRepository;
import adhoc.system.random_uuid.RandomUUIDUtils;
import adhoc.user.requests.UserNavigateRequest;
import adhoc.user.responses.UserNavigateResponse;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class UserNavigateService {

    private final UserRepository userRepository;
    private final ServerRepository serverRepository;
    private final AreaRepository areaRepository;

    /**
     * User navigation should be called some time prior to the client being launched.
     * It will provide connection details for an appropriate destination server.
     * <p>
     * You can choose a specific destination server, or an area (the server for the area will be chosen).
     * If you do not specify a server or area, a random enabled and active server will be chosen.
     * <p>
     * A server can also call this to try to automatically navigate the user to another server
     * (i.e. when the user's pawn has moved into an area represented by another server).
     */
    @Retryable(includes = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxRetries = 3, delay = 100, jitter = 10, multiplier = 1, maxDelay = 1000)
    public UserNavigateResponse userNavigate(UserNavigateRequest request) {

        UserEntity user = userRepository.getReferenceById(request.getUserId());

        ServerEntity destinationServer = request.getDestinationServerId() != null ?
                serverRepository.getReferenceById(request.getDestinationServerId()) : null;

        AreaEntity destinationArea = request.getDestinationAreaId() != null ?
                areaRepository.getReferenceById(request.getDestinationAreaId()) : null;

        // if no server provided, use server from area if that was provided
        if (destinationServer == null && destinationArea != null) {
            destinationServer = destinationArea.getServer();
        }

        if (destinationServer != null) {
            Preconditions.checkState(destinationServer.isEnabled(),
                    "User %s tried to navigate to server %s which is not enabled!", user.getId(), destinationServer.getId());
            Preconditions.checkState(destinationServer.isActive(),
                    "User %s tried to navigate to server %s which is not active!", user.getId(), destinationServer.getId());
        }

        // if no server specified (or server from area above) then just pick a random active/enabled server
        if (destinationServer == null) {
            List<ServerEntity> servers = serverRepository.findAll().stream()
                    .filter(server -> server.isEnabled() && server.isActive()).toList();
            Verify.verify(!servers.isEmpty());

            destinationServer = servers.get(ThreadLocalRandom.current().nextInt(servers.size()));
        }

        ServerEntity oldDestinationServer = user.getState().getDestinationServer();
        RegionEntity oldRegion = oldDestinationServer == null ? null : oldDestinationServer.getRegion();

        // when moving servers, update the position to ensure they can spawn at the right location
        if (destinationServer != oldDestinationServer
                && Objects.equals(destinationServer.getRegion(), oldRegion)) {
            user.getState().setX(request.getX());
            user.getState().setY(request.getY());
            user.getState().setZ(request.getZ());
            user.getState().setYaw(request.getYaw());
            user.getState().setPitch(request.getPitch());
        }

        user.getState().setDestinationServer(destinationServer);
        user.getState().setNavigated(LocalDateTime.now());

        UUID newToken = RandomUUIDUtils.randomUUID();
        user.getState().setToken(newToken);

        return new UserNavigateResponse(
                user.getState().getDestinationServer().getPublicIp(),
                user.getState().getDestinationServer().getPublicWebSocketPort(),
                user.getState().getDestinationServer().getWebSocketUrl(),
                user.getState().getDestinationServer().getRegion().getMapName(),
                user.getId(),
                user.getFaction().getId(),
                user.getState().getToken().toString(),
                user.getState().getX(), user.getState().getY(), user.getState().getZ(), user.getState().getYaw(), user.getState().getPitch());
    }

    //private static boolean isAuthenticatedAsServer() {
    //    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    //    return authentication != null
    //            && authentication.getAuthorities().stream().anyMatch(authority -> "ROLE_SERVER".equals(authority.getAuthority()));
    //}
}
