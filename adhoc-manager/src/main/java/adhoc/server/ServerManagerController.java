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

package adhoc.server;

import com.google.common.base.Preconditions;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/adhoc_api")
@Slf4j
@RequiredArgsConstructor
public class ServerManagerController {

    private final ServerManagerService serverManagerService;

    @PutMapping("/servers/{serverId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ServerDto putServer(
            @PathVariable Long serverId,
            @Valid @RequestBody ServerDto serverDto) {

        Preconditions.checkArgument(Objects.equals(serverId, serverDto.getId()),
                "Server ID mismatch: %s != %s", serverId, serverDto.getId());

        return serverManagerService.updateServer(serverDto);
    }

    @GetMapping("/servers/{serverId}/servers")
    @PreAuthorize("hasRole('SERVER')")
    public List<ServerDto> getServerServers(
            @PathVariable Long serverId) {

        return serverManagerService.getServerServers(serverId);
    }

    @MessageMapping("ServerStarted")
    @PreAuthorize("hasRole('SERVER')")
    public ServerUpdatedEvent handleServerStarted(
            @Valid @RequestBody ServerStartedEvent serverStartedEvent) {

        log.debug("Handling: {}", serverStartedEvent);

        return serverManagerService.handleServerStarted(serverStartedEvent);
    }
}
