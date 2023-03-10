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

package adhoc;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Getter
public class AdhocProperties {

    @Value("${adhoc.message-broker-host}")
    private String messageBrokerHost;
    @Value("${adhoc.message-broker.stomp-port}")
    private int messageBrokerStompPort;
    @Value("${adhoc.message-broker.core-port}")
    private int messageBrokerCorePort;

    @Value("${adhoc.manager-host}")
    private String managerHost;
    @Value("${adhoc.kiosk-host}")
    private String kioskHost;

    @Value("${adhoc.application.mode}")
    private AdhocApplication.Mode mode;
    @Value("${adhoc.feature-flags}")
    private String featureFlags;

    @Value("${adhoc.domain}")
    private String adhocDomain;
    @Value("${adhoc.unreal-project-name}")
    private String unrealProjectName;

    @EventListener
    public void contextRefreshed(ContextRefreshedEvent event) {
        log.info("messageBrokerHost={} messageBrokerStompPort={} messageBrokerCorePort={}", messageBrokerHost, messageBrokerStompPort, messageBrokerCorePort);
        log.info("managerHost={} kioskHost={}", managerHost, kioskHost);
        log.info("mode={} featureFlags={}", mode, featureFlags);
        log.info("adhocDomain={} unrealProjectName={}", adhocDomain, unrealProjectName);
    }
}
