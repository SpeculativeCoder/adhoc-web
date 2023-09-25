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

package adhoc.web;

import adhoc.web.channel_interceptor.ServerIgnoreCsrfChannelInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;
import org.springframework.security.messaging.web.csrf.CsrfChannelInterceptor;
import org.springframework.security.util.FieldUtils;

import java.util.List;
import java.util.ListIterator;

@Configuration
public class SecurityWebSocketConfig extends AbstractSecurityWebSocketMessageBrokerConfigurer {

    // uncomment if you need to disable CSRF on web socket for debugging
    //@Override
    //protected boolean sameOriginDisabled() {
    //    return true;
    //}

    @Override
    protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
        // TODO
        messages.simpMessageDestMatchers("/**").hasAnyRole("SERVER");
    }

    /** Swap out the CSRF interceptor so we can ignore CSRF for Unreal server communication. */
    @Override
    public void customizeClientInboundChannel(ChannelRegistration registration) {
        @SuppressWarnings("unchecked") final List<ChannelInterceptor> interceptors =
                (List<ChannelInterceptor>) FieldUtils.getProtectedFieldValue("interceptors", registration);

        for (ListIterator<ChannelInterceptor> iter = interceptors.listIterator(); iter.hasNext(); ) {
            ChannelInterceptor interceptor = iter.next();
            if (interceptor instanceof CsrfChannelInterceptor csrfChannelInterceptor) {
                iter.set(new ServerIgnoreCsrfChannelInterceptor(csrfChannelInterceptor));
            }
        }
    }
}

// TODO
//@Configuration
//@EnableWebSocketSecurity
//public class WebSocketSecurityConfig {
//
//    @Bean
//    AuthorizationManager<Message<?>> messageAuthorizationManager(MessageMatcherDelegatingAuthorizationManager.Builder messages) {
//        return messages
//                .simpMessageDestMatchers("/**").hasAnyRole("SERVER")
//                .build();
//    }
//}

//                .simpSubscribeDestMatchers("/topic/events").permitAll();

//                .simpTypeMatchers(SimpMessageType.MESSAGE).hasAnyRole("SERVER")
//                .simpTypeMatchers(SimpMessageType.MESSAGE).authenticated()
//                .simpTypeMatchers(SimpMessageType.MESSAGE).denyAll();

//                .simpMessageDestMatchers("/app/**").hasAnyRole("SERVER")
//                .simpMessageDestMatchers("/topic/**").hasAnyRole("SERVER");

//                .simpTypeMatchers(SimpMessageType.SUBSCRIBE, SimpMessageType.MESSAGE).permitAll()
//                .simpTypeMatchers(SimpMessageType.SUBSCRIBE, SimpMessageType.MESSAGE).denyAll()
//                .simpDestMatchers("/app/**").hasAnyRole("SERVER")
//                .simpMessageDestMatchers("/topic/**").hasAnyRole("SERVER")
