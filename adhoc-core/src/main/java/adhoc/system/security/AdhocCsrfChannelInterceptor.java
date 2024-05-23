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

package adhoc.system.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.messaging.web.csrf.XorCsrfChannelInterceptor;
import org.springframework.stereotype.Component;

/**
 * Replacement for Spring CSRF channel interceptor. This ignores CSRF for web socket connections from Unreal server.
 * All other requests are handled as usual by the Spring {@link XorCsrfChannelInterceptor}.
 */
@Primary
@Component("csrfChannelInterceptor")
@Slf4j
@RequiredArgsConstructor
public class AdhocCsrfChannelInterceptor implements ChannelInterceptor {

    private final XorCsrfChannelInterceptor xorCsrfChannelInterceptor = new XorCsrfChannelInterceptor();

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof UsernamePasswordAuthenticationToken authenticationToken &&
                authenticationToken.getAuthorities()
                        .stream().anyMatch(authority -> "ROLE_SERVER".equals(authority.getAuthority()))) {
            return message;
        }

        return xorCsrfChannelInterceptor.preSend(message, channel);
    }
}
