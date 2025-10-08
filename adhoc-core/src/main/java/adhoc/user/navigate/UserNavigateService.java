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

package adhoc.user.navigate;

import adhoc.area.Area;
import adhoc.area.AreaRepository;
import adhoc.region.Region;
import adhoc.server.Server;
import adhoc.server.ServerRepository;
import adhoc.user.User;
import adhoc.user.UserRepository;
import adhoc.user.UserService;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
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
     * when the user's pawn has moved into an area represented by the other server.
     */
    @Retryable(retryFor = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 100, maxDelay = 1000))
    public UserNavigateResponse userNavigate(UserNavigateRequest request) {

        log.debug("userNavigate: request={}", request);

        // for now, only server may specify location update
        if (!isAuthenticatedAsServer()) {
            Preconditions.checkArgument(request.getX() == null);
            Preconditions.checkArgument(request.getY() == null);
            Preconditions.checkArgument(request.getZ() == null);
            Preconditions.checkArgument(request.getYaw() == null);
            Preconditions.checkArgument(request.getPitch() == null);
        }

        User user = userRepository.getReferenceById(request.getUserId());

        Server destinationServer = request.getDestinationServerId() != null ?
                serverRepository.getReferenceById(request.getDestinationServerId()) : null;

        Area destinationArea = request.getDestinationAreaId() != null ?
                areaRepository.getReferenceById(request.getDestinationAreaId()) : null;

        // if no server provided, use server from area if that was provided
        if (destinationServer == null && destinationArea != null) {
            destinationServer = destinationArea.getServer();
        }

        if (destinationServer != null) {
            Preconditions.checkState(destinationServer.isEnabled(),
                    "User {} tried to navigate to server {} which is not enabled!", user.getId(), destinationServer.getId());
            Preconditions.checkState(destinationServer.isActive(),
                    "User {} tried to navigate to server {} which is not active!", user.getId(), destinationServer.getId());
        }

        // if no server specified (or server from area above) then just pick a random active/enabled server
        if (destinationServer == null) {
            List<Server> servers = serverRepository.findAll().stream()
                    .filter(server -> server.isEnabled() && server.isActive()).toList();
            Verify.verify(!servers.isEmpty());

            destinationServer = servers.get(ThreadLocalRandom.current().nextInt(servers.size()));
        }

        Server oldDestinationServer = user.getState().getDestinationServer();
        Region oldRegion = oldDestinationServer == null ? null : oldDestinationServer.getRegion();

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
        user.setNavigated(LocalDateTime.now());

        UserNavigateResponse response = new UserNavigateResponse(
                user.getState().getDestinationServer().getPublicIp(),
                user.getState().getDestinationServer().getPublicWebSocketPort(),
                user.getState().getDestinationServer().getWebSocketUrl(),
                user.getState().getDestinationServer().getRegion().getMapName(),
                user.getState().getX(), user.getState().getY(), user.getState().getZ(), user.getState().getYaw(), user.getState().getPitch());

        log.debug("userNavigate: response={}", response);

        return response;
    }

    private static boolean isAuthenticatedAsServer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.getAuthorities().stream().anyMatch(authority -> "ROLE_SERVER".equals(authority.getAuthority()));
    }
}
