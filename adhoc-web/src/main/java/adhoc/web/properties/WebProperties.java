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

package adhoc.web.properties;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@Getter
public class WebProperties {

    public enum ApplicationMode {
        MANAGER,
        KIOSK
    }

    @Value("${server.port}")
    private Integer serverPort;
    @Value("${server.port-2}")
    private Integer serverPort2;

    @Value("${adhoc.application.mode}")
    private ApplicationMode mode;
    @Value("${adhoc.feature-flags}")
    private String featureFlags;

    @Value("${adhoc.message-broker-host}")
    private String messageBrokerHost;
    @Value("${adhoc.message-broker.stomp-port}")
    private int messageBrokerStompPort;
    @Value("${adhoc.message-broker.core-port}")
    private int messageBrokerCorePort;

    @Value("${adhoc.manager-message-broker-host}")
    private String managerMessageBrokerHost;
    @Value("${adhoc.manager-message-broker-core-port}")
    private int managerMessageBrokerCorePort;

    @Value("${adhoc.kiosk-message-broker-host}")
    private String kioskMessageBrokerHost;
    @Value("${adhoc.kiosk-message-broker-core-port}")
    private int kioskMessageBrokerCorePort;

    @Value("${adhoc.manager-host}")
    private String managerHost;
    @Value("${adhoc.kiosk-host}")
    private String kioskHost;

    @Value("${adhoc.domain}")
    private String adhocDomain;

    @Value("${adhoc.unreal-project-name}")
    private String unrealProjectName;
    @Value("${adhoc.unreal-project-region-maps}")
    private List<String> unrealProjectRegionMaps;

    @EventListener
    public void contextRefreshed(ContextRefreshedEvent event) {
        log.info("serverPort={} serverPort2={}", serverPort, serverPort2);
        log.info("mode={} featureFlags={}", mode, featureFlags);
        log.info("messageBrokerHost={} messageBrokerStompPort={} messageBrokerCorePort={}", messageBrokerHost, messageBrokerStompPort, messageBrokerCorePort);
        log.info("managerMessageBrokerHost={} managerMessageBrokerCorePort={}", managerMessageBrokerHost, managerMessageBrokerCorePort);
        log.info("kioskMessageBrokerHost={} kioskMessageBrokerCorePort={}", kioskMessageBrokerHost, kioskMessageBrokerCorePort);
        log.info("managerHost={} kioskHost={}", managerHost, kioskHost);
        log.info("adhocDomain={}", adhocDomain);
        log.info("unrealProjectName={} unrealProjectRegionMaps={}", unrealProjectName, unrealProjectRegionMaps);
    }
}
