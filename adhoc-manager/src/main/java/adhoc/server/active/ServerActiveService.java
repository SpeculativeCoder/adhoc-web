/*
 * Copyright (c) 2022-2026 SpeculativeCoder (https://github.com/SpeculativeCoder)
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

package adhoc.server.active;

import adhoc.server.ServerEntity;
import adhoc.server.ServerRepository;
import adhoc.task.ServerTaskEntity;
import adhoc.task.ServerTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class ServerActiveService {

    private final ServerProperties serverProperties;

    private final ServerRepository serverRepository;
    private final ServerTaskRepository serverTaskRepository;

    private final Clock clock;

    @Retryable(includes = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxRetries = 3, delay = 100, jitter = 10, multiplier = 1, maxDelay = 1000)
    public void handleServerStarted(ServerStartedEvent serverStartedEvent) {
        ServerEntity server = serverRepository.getReferenceById(serverStartedEvent.getServerId());

        // TODO: internal server status?
        server.setActive(true);
    }

    @Retryable(includes = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxRetries = 3, delay = 100, jitter = 10, multiplier = 1, maxDelay = 1000)
    public void manageActiveServers() {
        log.trace("Managing active servers...");

        try (Stream<ServerEntity> servers = serverRepository.streamBy()) {
            servers.forEach(server -> {

                String publicIp = null;
                Integer publicWebSocketPort = null;
                String domain = null;
                String webSocketUrl = null;

                Optional<ServerTaskEntity> optionalServerTask = serverTaskRepository.findFirstByServerId(server.getId());
                if (optionalServerTask.isPresent()) {
                    ServerTaskEntity serverTask = optionalServerTask.get();

                    publicIp = serverTask.getPublicIp();
                    publicWebSocketPort = serverTask.getPublicWebSocketPort();
                    domain = serverTask.getDomain();

                    if (server.isEnabled() && server.isActive() &&
                            serverTask.getPublicIp() != null && serverTask.getPublicWebSocketPort() != null &&
                            (serverTask.getDomain() != null || !serverProperties.getSsl().isEnabled())) {
                        webSocketUrl = (serverProperties.getSsl().isEnabled()
                                ? "wss://" + serverTask.getDomain()
                                : "ws://" + serverTask.getPublicIp())
                                + ":" + serverTask.getPublicWebSocketPort();
                    }

                    server.setSeen(LocalDateTime.now(clock));

                } else {
                    server.setActive(false);
                }

                server.setPublicIp(publicIp);
                server.setPublicWebSocketPort(publicWebSocketPort);
                server.setDomain(domain);
                server.setWebSocketUrl(webSocketUrl);
            });
        }
    }
}
