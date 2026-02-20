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

package adhoc.system.auth;

import adhoc.shared.properties.CoreProperties;
import com.google.common.base.Strings;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

/**
 * This matcher provides a way to identify server requests.
 * Used by {@link adhoc.system.WebSecurityConfiguration#securityFilterChain} to ignore CSRF checking on web requests from Unreal server.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdhocServerBasicAuthRequestMatcher implements RequestMatcher {

    private final CoreProperties coreProperties;

    private String encodedServerBasicAuth;

    @PostConstruct
    public void postConstruct() {
        if (!Strings.isNullOrEmpty(coreProperties.getServerBasicAuthUsername())
                && !Strings.isNullOrEmpty(coreProperties.getServerBasicAuthPassword())) {

            encodedServerBasicAuth = HttpHeaders.encodeBasicAuth(
                    coreProperties.getServerBasicAuthUsername(),
                    coreProperties.getServerBasicAuthPassword(),
                    null);
        }
    }

    @Override
    public boolean matches(@NonNull HttpServletRequest request) {

        boolean userServer = false;

        if (encodedServerBasicAuth != null) {
            String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

            userServer = encodedServerBasicAuth != null
                    && ("Basic " + encodedServerBasicAuth).equals(authorizationHeader);
        }

        return userServer;
    }
}
