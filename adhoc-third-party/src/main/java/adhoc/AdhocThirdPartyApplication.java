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

package adhoc;

import com.google.common.collect.ImmutableMap;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

/**
 * For testing, this will host the content we plan to upload to a third party (e.g. itch).
 * It pretends to be a third party / different location by being on a different host and/or port etc.
 */
@SpringBootApplication
@RestController
@RequiredArgsConstructor
@Slf4j
public class AdhocThirdPartyApplication {

    @Value("${adhoc.server-port-http}")
    private Integer serverPortHttp;

    @Value("${adhoc.manager-domain}")
    private String managerDomain;

    @GetMapping(value = {"/"})
    public ModelAndView getIndex(HttpServletRequest request) {

        ModelAndView index = new ModelAndView("index",
                ImmutableMap.of("ADHOC_URL", request.getScheme() + "://" + managerDomain));
        return index;
    }

    @Bean(destroyMethod = "destroy")
    public Connector httpConnector() {
        final Connector connector = new Connector();
        connector.setThrowOnFailure(true);
        connector.setPort(serverPortHttp);
        //connector.addUpgradeProtocol(new Http2Protocol());
        return connector;
    }

    public static void main(String[] args) {
        SpringApplication.run(AdhocThirdPartyApplication.class, args);
    }
}
