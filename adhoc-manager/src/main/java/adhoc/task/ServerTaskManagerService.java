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

package adhoc.task;

import adhoc.hosting.HostedServerTask;
import adhoc.hosting.HostingService;
import adhoc.server.Server;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
@Slf4j
@RequiredArgsConstructor
public class ServerTaskManagerService {

    private final HostingService hostingService;

    public void startServerTask(Server server) {
        try {
            log.info("Starting server task for server {}", server.getId());
            HostedServerTask hostedServerTask = hostingService.startServerTask(server);

        } catch (Exception e) {
            log.warn("Failed to start server task for server {}!", server.getId(), e);
        }
    }

    public void stopServerTask(ServerTask serverTask) {
        try {
            log.info("Stopping server task for server task {}", serverTask.getServerId());
            hostingService.stopServerTask(serverTask.getTaskIdentifier());

        } catch (Exception e) {
            log.warn("Failed to stop server task for server task {}!", serverTask.getServerId(), e);
        }
    }
}
