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
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Profile({"mode-manager"})
@Slf4j
@Getter
public class ManagerProperties {

    @Value("${adhoc.server.basic-auth.username}")
    private String serverBasicAuthUsername;
    @Value("${adhoc.server.basic-auth.password}")
    private String serverBasicAuthPassword;

    @Value("${adhoc.default-user-password}")
    private String defaultUserPassword;
    @Value("${adhoc.default-admin-password}")
    private String defaultAdminPassword;

    @Value("${adhoc.kiosk-message-broker-host}")
    private String kioskMessageBrokerHost;
    @Value("${adhoc.kiosk-message-broker-core-port}")
    private int kioskMessageBrokerCorePort;

    @Value("${adhoc.manager-domain}")
    private String managerDomain;
    @Value("${adhoc.kiosk-domain}")
    private String kioskDomain;
    @Value("${adhoc.server-domain}")
    private String serverDomain;

    @Value("${MANAGER_IMAGE:adhoc_manager}")
    private String managerImage;
    @Value("${KIOSK_IMAGE:adhoc_kiosk}")
    private String kioskImage;
    @Value("${SERVER_IMAGE:adhoc_server}")
    private String serverImage;

    @Value("${adhoc.max-pawns}")
    private Integer maxPawns;
    @Value("${adhoc.max-players}")
    private Integer maxPlayers;
    @Value("${adhoc.max-bots}")
    private Integer maxBots;

    @EventListener
    public void contextRefreshed(ContextRefreshedEvent event) {
        log.info("serverBasicAuthUsername={} serverBasicAuthPassword={}", serverBasicAuthUsername, serverBasicAuthPassword == null ? null : "***");
        log.info("defaultUserPassword={} defaultAdminPassword={}", defaultUserPassword, defaultAdminPassword == null ? null : "***");
        log.info("kioskMessageBrokerHost={} kioskMessageBrokerCorePort={}", kioskMessageBrokerHost, kioskMessageBrokerCorePort);
        log.info("managerDomain={} kioskDomain={} serverDomain={}", managerDomain, kioskDomain, serverDomain);
        log.info("managerImage={} kioskImage={} serverImage={}", managerImage, kioskImage, serverImage);
        log.info("maxPawns={} maxPlayers={} maxBots={}", maxPawns, maxPlayers, maxBots);
    }
}
