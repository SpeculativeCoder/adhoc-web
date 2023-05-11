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

package adhoc.server;

import adhoc.area.Area;
import adhoc.area.AreaRepository;
import adhoc.region.RegionRepository;
import adhoc.server.event.ServerStartedEvent;
import adhoc.server.event.ServerUpdatedEvent;
import adhoc.server.dto.ServerDto;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Transactional
@Service
@Profile("mode-manager")
@Slf4j
@RequiredArgsConstructor
public class ManagerServerService {

    private final ServerRepository serverRepository;
    private final RegionRepository regionRepository;
    private final AreaRepository areaRepository;
    private final ServerService serverService;
    private final SimpMessageSendingOperations stomp;

    public ServerDto updateServer(ServerDto serverDto) {
        return serverService.toDto(
                serverRepository.save(toEntity(serverDto)));
    }

    Server toEntity(ServerDto serverDto) {
        Server server = serverDto.getId() == null ? new Server() : serverRepository.getReferenceById(serverDto.getId());

        server.setName(serverDto.getName());

        server.setRegion(regionRepository.getReferenceById(serverDto.getRegionId()));
        server.setAreas(serverDto.getAreaIds().stream().map(areaRepository::getReferenceById).collect(Collectors.toList()));

        server.setMapName(serverDto.getMapName());

        server.setX(server.getX());
        server.setY(server.getY());
        server.setZ(server.getZ());

        server.setStatus(ServerStatus.INACTIVE);

        server.setManagerHost(server.getManagerHost());
        server.setPrivateIp(server.getPrivateIp());
        server.setPublicIp(server.getPublicIp());

        server.setWebSocketUrl(server.getWebSocketUrl());

        return server;
    }

    public void processServerStarted(ServerStartedEvent serverStartedEvent) {
        Server server = serverRepository.getReferenceById(serverStartedEvent.getServerId());

        server.setStatus(ServerStatus.ACTIVE);
        server.setPrivateIp(serverStartedEvent.getPrivateIp());
        //server.setManagerHost(server.getManagerHost());

        sendServerUpdatedEvent(server);
    }

    public void sendServerUpdatedEvent(Server server) {
        log.trace("Sending server info event for server {}", server.getId());

        ServerUpdatedEvent event = new ServerUpdatedEvent(
                server.getId(),
                server.getVersion(),
                server.getName(),
                server.getRegion().getId(),
                server.getAreas().stream().map(Area::getId).collect(Collectors.toList()),
                server.getAreas().stream().map(Area::getIndex).collect(Collectors.toList()),
                server.getStatus().name(),
                server.getManagerHost(),
                server.getPrivateIp(),
                server.getPublicIp(),
                server.getPublicWebSocketPort(),
                server.getWebSocketUrl());

        log.info("Sending: {}", event);
        stomp.convertAndSend("/topic/events", event);
    }
}
