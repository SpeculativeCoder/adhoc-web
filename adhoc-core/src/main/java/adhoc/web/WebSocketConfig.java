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

import adhoc.properties.CoreProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final CoreProperties coreProperties;

    @Lazy
    @Autowired
    @SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
    private TaskScheduler taskScheduler;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        //registry.addEndpoint("/ws/stomp/user")
        //        .addInterceptors(new HttpSessionHandshakeInterceptor());

        registry.addEndpoint("/ws/stomp/server")
                .addInterceptors(new HttpSessionHandshakeInterceptor());

        registry.addEndpoint("/ws/stomp/user_sockjs")
                .addInterceptors(new HttpSessionHandshakeInterceptor())
                .setAllowedOriginPatterns(
                        "https://" + coreProperties.getAdhocDomain(),
                        "http://" + coreProperties.getAdhocDomain(),
                        "https://*." + coreProperties.getAdhocDomain(),
                        "http://*." + coreProperties.getAdhocDomain())
                .withSockJS()
                //.setHeartbeatTime(Duration.ofSeconds(30).toMillis())
                .setTaskScheduler(taskScheduler);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.setApplicationDestinationPrefixes("/app");

        config.setPreservePublishOrder(true);

        //config.enableSimpleBroker("/queue", "/topic");
        config.enableStompBrokerRelay("/queue", "/topic")
                .setRelayHost(coreProperties.getMessageBrokerHost())
                .setRelayPort(coreProperties.getMessageBrokerStompPort())
                //.setSystemHeartbeatReceiveInterval(Duration.ofSeconds(30).toMillis())
                //.setSystemHeartbeatSendInterval(Duration.ofSeconds(30).toMillis())
                .setTaskScheduler(taskScheduler);
    }
}

//@Autowired
//public void setTaskScheduler(@Lazy TaskScheduler taskScheduler) {
//    this.taskScheduler = taskScheduler;
//}

//    @Override
//    public void configureClientInboundChannel(ChannelRegistration registration) {
//        // TODO
//    }