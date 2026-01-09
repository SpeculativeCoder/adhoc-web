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

package adhoc.server;

import adhoc.area.AreaEntity;
import adhoc.area.AreaRepository;
import adhoc.region.RegionRepository;
import adhoc.task.server.ServerTaskEntity;
import adhoc.task.server.ServerTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.data.domain.Sort;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class ServerManagerService {

    private final ServerRepository serverRepository;
    private final ServerTaskRepository serverTaskRepository;
    private final RegionRepository regionRepository;
    private final AreaRepository areaRepository;

    private final ServerService serverService;

    public List<ServerDto> getServerServers(Long serverId) {
        return serverRepository.findAll(Sort.by("id")).stream().map(serverService::toDto).toList();
    }

    public ServerDto updateServer(ServerDto serverDto) {
        ServerEntity server = toEntity(serverDto, serverRepository.getReferenceById(serverDto.getId()));

        return serverService.toDto(server);
    }

    @Retryable(includes = {TransientDataAccessException.class, LockAcquisitionException.class},
            maxRetries = 3, delay = 100, jitter = 10, multiplier = 1, maxDelay = 1000)
    public ServerUpdatedEvent handleServerStarted(ServerStartedEvent serverStartedEvent) {
        ServerEntity server = serverRepository.getReferenceById(serverStartedEvent.getServerId());

        // TODO: internal server status?
        server.setActive(true);

        return toServerUpdatedEvent(server);
    }

    public List<ServerDto> findEnabledTasklessServers() {
        List<ServerDto> enabledTasklessServers = new ArrayList<>();

        try (Stream<ServerEntity> enabledServers = serverRepository.streamByEnabledTrue()) {
            enabledServers.forEach(enabledServer -> {
                Optional<ServerTaskEntity> serverTask = serverTaskRepository.findFirstByServerId(enabledServer.getId());
                if (serverTask.isEmpty()) {
                    // no existing server task? will need to start a new one
                    ServerDto enabledTasklessServer = serverService.toDto(enabledServer);
                    enabledTasklessServers.add(enabledTasklessServer);
                }
            });
        }

        return enabledTasklessServers;
    }

    ServerEntity toEntity(ServerDto serverDto, ServerEntity server) {
        server.setRegion(regionRepository.getReferenceById(serverDto.getRegionId()));
        server.setAreas(serverDto.getAreaIds().stream().map(areaRepository::getReferenceById).collect(Collectors.toList())); // TODO

        server.setMapName(serverDto.getMapName());

        server.setX(server.getX());
        server.setY(server.getY());
        server.setZ(server.getZ());

        server.setPublicIp(server.getPublicIp());

        server.setWebSocketUrl(server.getWebSocketUrl());

        return server;
    }

    public ServerUpdatedEvent toServerUpdatedEvent(ServerEntity server) {
        ServerUpdatedEvent event = new ServerUpdatedEvent(
                server.getId(),
                server.getVersion(),
                server.getRegion().getId(),
                server.getAreas().stream().map(AreaEntity::getId).collect(Collectors.toList()),
                server.getAreas().stream().map(AreaEntity::getIndex).collect(Collectors.toList()),
                server.isEnabled(),
                server.isActive(),
                server.getPublicIp(),
                server.getPublicWebSocketPort(),
                server.getWebSocketUrl());
        return event;
    }
}
