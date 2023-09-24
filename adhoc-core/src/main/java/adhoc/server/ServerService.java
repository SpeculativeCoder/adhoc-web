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
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Transactional
@Service
@Slf4j
@RequiredArgsConstructor
public class ServerService {

    private final ServerRepository serverRepository;

    @Transactional(readOnly = true)
    public List<ServerDto> getServers() {
        return serverRepository.findAll(PageRequest.of(0, 100, Sort.Direction.ASC, "id"))
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ServerDto getServer(Long serverId) {
        return toDto(serverRepository.getReferenceById(serverId));
    }

    ServerDto toDto(Server server) {
        return new ServerDto(
                server.getId(),
                server.getVersion(),
                server.getName(),
                server.getRegion().getId(),
                server.getAreas().stream().map(Area::getId).collect(Collectors.toList()),
                server.getAreas().stream().map(Area::getIndex).collect(Collectors.toList()),
                server.getMapName(),
                server.getX(), server.getY(), server.getZ(),
                server.getStatus().name(),
                server.getManagerHost(),
                server.getPrivateIp(), server.getPublicIp(), server.getPublicWebSocketPort(),
                server.getWebSocketUrl(),
                server.getInitiated(),
                server.getSeen());
    }
}
