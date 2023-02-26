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

import adhoc.server.event.ServerStartedEvent;
import adhoc.server.dto.ServerDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile("mode-manager")
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class ServerManagerController {

    private final ServerManagerService serverManagerService;

    @PutMapping("/servers/{serverId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ServerDto putServer(@PathVariable("serverId") Long serverId, @Valid @RequestBody ServerDto serverDto) {
        serverDto.setId(serverId);

        return serverManagerService.updateServer(serverDto);
    }
    @MessageMapping("ServerStarted")
    @PreAuthorize("hasRole('SERVER')")
    public void handleServerStarted(@Valid @RequestBody ServerStartedEvent serverStartedEvent) {
        log.info("Handling: {}", serverStartedEvent);

        serverManagerService.processServerStarted(serverStartedEvent);
    }
}
