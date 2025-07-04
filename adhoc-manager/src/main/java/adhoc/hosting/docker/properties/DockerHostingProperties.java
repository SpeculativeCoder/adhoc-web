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

package adhoc.hosting.docker.properties;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Profile("hosting-docker")
@Slf4j
@Getter
public class DockerHostingProperties {

    @Value("${adhoc.docker.host}")
    private String dockerHost;

    @Value("${adhoc.docker.auto-remove}")
    private Boolean autoRemove;

    @EventListener
    public void contextRefreshed(ContextRefreshedEvent event) {
        log.info("dockerHost={}", dockerHost);
        log.info("autoRemove={}", autoRemove);
    }
}

//    @Value("${server.ssl.trust-certificate}")
//    private Path caCertificate;
//
//    @Value("${server.ssl.certificate}")
//    private Path serverCertificate;
//
//    @Value("${server.ssl.certificate-private-key}")
//    private Path privateKey;
