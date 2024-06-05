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

package adhoc.user.navigate;

import adhoc.area.Area;
import adhoc.area.AreaRepository;
import adhoc.server.Server;
import adhoc.server.ServerRepository;
import adhoc.user.User;
import adhoc.user.UserRepository;
import com.google.common.base.Preconditions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
@Slf4j
@RequiredArgsConstructor
public class UserNavigateService {

    private final UserRepository userRepository;
    private final ServerRepository serverRepository;
    private final AreaRepository areaRepository;

    @Retryable(retryFor = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 100, maxDelay = 1000))
    public ResponseEntity<ServerUserNavigateResponse> serverUserNavigate(ServerUserNavigateRequest serverUserNavigateRequest) {
        User user = userRepository.getReferenceById(serverUserNavigateRequest.getUserId());
        Server sourceServer = serverRepository.getReferenceById(serverUserNavigateRequest.getSourceServerId());
        Area destinationArea = areaRepository.getReferenceById(serverUserNavigateRequest.getDestinationAreaId());

        Preconditions.checkArgument(user.getServer() == sourceServer);

        Server destinationServer = destinationArea.getServer();
        if (destinationServer == null) {
            log.warn("User {} tried to navigate to area {} which does not have a server!", user.getId(), destinationArea.getId());
            return ResponseEntity.unprocessableEntity().build();
        }

        if (!destinationServer.isEnabled()) {
            log.warn("User {} tried to navigate to server {} which is not enabled!", user.getId(), destinationServer.getId());
            return ResponseEntity.unprocessableEntity().build();
        }

        if (!destinationServer.isActive()) {
            log.warn("User {} tried to navigate to server {} which is not active!", user.getId(), destinationServer.getId());
            return ResponseEntity.unprocessableEntity().build();
        }

        user.setX(serverUserNavigateRequest.getX());
        user.setY(serverUserNavigateRequest.getY());
        user.setZ(serverUserNavigateRequest.getZ());

        user.setYaw(serverUserNavigateRequest.getYaw());
        user.setPitch(serverUserNavigateRequest.getPitch());

        // NOTE: when user joins new server this will be updated
        //user.setServer(destinationServer);

        return ResponseEntity.ok(new ServerUserNavigateResponse(destinationServer.getId(),
                //adhocProperties.getServerDomain(),
                destinationServer.getPublicIp(),
                destinationServer.getPublicWebSocketPort(),
                destinationServer.getWebSocketUrl()));
    }
}
