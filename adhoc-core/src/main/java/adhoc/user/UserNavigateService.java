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

import adhoc.area.Area;
import adhoc.area.AreaRepository;
import adhoc.server.Server;
import adhoc.server.ServerRepository;
import adhoc.user.request_response.UserNavigateRequest;
import adhoc.user.request_response.UserNavigateResponse;
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
import java.util.concurrent.ThreadLocalRandom;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class UserNavigateService {

    private final UserRepository userRepository;
    private final ServerRepository serverRepository;
    private final AreaRepository areaRepository;

    private final UserService userService;

    /**
     * User navigation should be called some time prior to the client being launched.
     * It will provide connection details for an appropriate destination server.
     * <p>
     * You can choose a specific destination server, or an area (the server for the area will be chosen).
     * If you do not specify a server or area, a random enabled and active will be chosen.
     * <p>
     * A server can also call this to try to automatically navigate the user to another server
     * when the user's pawn has moved into an area represented by the other server.
     */
    @Retryable(retryFor = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 100, maxDelay = 1000))
    public UserNavigateResponse userNavigate(UserNavigateRequest userNavigateRequest) {

        // for now, only server may specify location update
        if (!isAuthenticatedAsServer()) {
            Preconditions.checkArgument(userNavigateRequest.getX() == null);
            Preconditions.checkArgument(userNavigateRequest.getY() == null);
            Preconditions.checkArgument(userNavigateRequest.getZ() == null);
            Preconditions.checkArgument(userNavigateRequest.getYaw() == null);
            Preconditions.checkArgument(userNavigateRequest.getPitch() == null);
        }

        User user = userRepository.getReferenceById(userNavigateRequest.getUserId());

        Server destinationServer = userNavigateRequest.getDestinationServerId() != null ?
                serverRepository.getReferenceById(userNavigateRequest.getDestinationServerId()) : null;

        Area destinationArea = userNavigateRequest.getDestinationAreaId() != null ?
                areaRepository.getReferenceById(userNavigateRequest.getDestinationAreaId()) : null;

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

        // when moving servers, update the position to ensure they can spawn at the right location
        if (destinationServer != user.getDestinationServer()) {
            user.setX(userNavigateRequest.getX());
            user.setY(userNavigateRequest.getY());
            user.setZ(userNavigateRequest.getZ());
            user.setYaw(userNavigateRequest.getYaw());
            user.setPitch(userNavigateRequest.getPitch());
        }

        user.setDestinationServer(destinationServer);
        user.setNavigated(LocalDateTime.now());

        return new UserNavigateResponse(
                user.getDestinationServer().getPublicIp(),
                user.getDestinationServer().getPublicWebSocketPort(),
                user.getDestinationServer().getWebSocketUrl(),
                user.getDestinationServer().getRegion().getMapName(),
                user.getX(), user.getY(), user.getZ(), user.getYaw(), user.getPitch());
    }

    private static boolean isAuthenticatedAsServer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.getAuthorities().stream().anyMatch(authority -> "ROLE_SERVER".equals(authority.getAuthority()));
    }
}
