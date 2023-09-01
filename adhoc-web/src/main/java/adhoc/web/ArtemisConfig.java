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

import adhoc.web.properties.WebProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.core.config.ClusterConnectionConfiguration;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyAcceptorFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants;
import org.apache.activemq.artemis.core.server.cluster.impl.MessageLoadBalancingType;
import org.apache.activemq.artemis.core.settings.impl.AddressSettings;
import org.apache.activemq.artemis.jms.server.config.TopicConfiguration;
import org.apache.activemq.artemis.jms.server.config.impl.TopicConfigurationImpl;
import org.springframework.boot.autoconfigure.jms.artemis.ArtemisConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configure Active MQ Artemis to act as an embedded message broker running locally.
 * In manager mode we will allow STOMP connections from the Unreal servers.
 * In kiosk mode we should allow STOMP connections from the user's web browsers.
 * The manager and kiosk brokers talk to each other via an Artemis cluster to ensure we pass along events.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ArtemisConfig implements ArtemisConfigurationCustomizer {

    private final WebProperties webProperties;

    @Bean
    public TopicConfiguration eventsTopic() {
        TopicConfigurationImpl topicConfiguration = new TopicConfigurationImpl();
        topicConfiguration.setName("events");
        topicConfiguration.setBindings("/topic/events");
        return topicConfiguration;
    }

    @Override
    public void customize(org.apache.activemq.artemis.core.config.Configuration configuration) {
        configuration.addAddressSetting("/topic/events", eventsAddressSettings());

        configuration.addAcceptorConfiguration(
                new TransportConfiguration(NettyAcceptorFactory.class.getName(), stompProps(), "stomp-acceptor"));
        configuration.addAcceptorConfiguration(
                new TransportConfiguration(NettyAcceptorFactory.class.getName(), coreProps(), "core-acceptor"));

        //configuration.addConnectorConfiguration("stomp-connector",
        //        new TransportConfiguration(NettyConnectorFactory.class.getName(), stompProps()));
        configuration.addConnectorConfiguration("core-connector",
                new TransportConfiguration(NettyConnectorFactory.class.getName(), coreProps()));

        configuration.addConnectorConfiguration("kiosk-core-connector",
                new TransportConfiguration(NettyConnectorFactory.class.getName(), kioskCoreProps()));

        configuration.addConnectorConfiguration("manager-core-connector",
                new TransportConfiguration(NettyConnectorFactory.class.getName(), managerCoreProps()));

        ClusterConnectionConfiguration clusterConnection = new ClusterConnectionConfiguration();
        clusterConnection.setName("adhoc-cluster-connection");
        clusterConnection.setAddress("");
        clusterConnection.setConnectorName("core-connector");
        clusterConnection.setClientFailureCheckPeriod(Duration.ofMinutes(1).toMillis());
        clusterConnection.setConnectionTTL(Duration.ofMinutes(2).toMillis());
        clusterConnection.setCallTimeout(Duration.ofMinutes(1).toMillis());
        clusterConnection.setRetryInterval(Duration.ofSeconds(1).toMillis());
        clusterConnection.setRetryIntervalMultiplier(2);
        clusterConnection.setMaxRetryInterval(Duration.ofMinutes(30).toMillis());
        clusterConnection.setDuplicateDetection(true);
        clusterConnection.setMessageLoadBalancingType(MessageLoadBalancingType.STRICT);
        clusterConnection.setMaxHops(1);
        //clusterConnection.setCallFailoverTimeout(Duration.ofMinutes(5).toMillis());
        clusterConnection.setClusterNotificationInterval(Duration.ofSeconds(2).toMillis());
        //clusterConnection.setAllowDirectConnectionsOnly(true);
        //clusterConnection.setReconnectAttempts(1);
        //clusterConnection.setInitialConnectAttempts(1);
        clusterConnection.setStaticConnectors(Arrays.asList("manager-core-connector")); //, "kiosk-core-connector")); //, "core-connector"
        configuration.addClusterConfiguration(clusterConnection);
    }

    private AddressSettings eventsAddressSettings() {
        AddressSettings addressSettings = new AddressSettings();
        addressSettings.setAutoCreateAddresses(true);
        addressSettings.setAutoCreateQueues(false);
        addressSettings.setAutoDeleteAddresses(false);
        addressSettings.setAutoDeleteQueues(false);
        return addressSettings;
    }

    private Map<String, Object> stompProps() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put(TransportConstants.SCHEME_PROP_NAME, "tcp");
        props.put(TransportConstants.HOST_PROP_NAME, webProperties.getMessageBrokerHost());
        props.put(TransportConstants.PORT_PROP_NAME, webProperties.getMessageBrokerStompPort());
        props.put(TransportConstants.PROTOCOLS_PROP_NAME, "STOMP");
        props.put(TransportConstants.CONNECTION_TTL, Long.toString(Duration.ofMinutes(2).toMillis()));
        props.put(TransportConstants.NETTY_CONNECT_TIMEOUT, Long.toString(Duration.ofMinutes(1).toMillis()));
        props.put(TransportConstants.HEART_BEAT_TO_CONNECTION_TTL_MODIFIER, "4");
        log.info("stompProps: props={}", props);
        return props;
    }

    private Map<String, Object> coreProps() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put(TransportConstants.SCHEME_PROP_NAME, "tcp");
        props.put(TransportConstants.HOST_PROP_NAME, webProperties.getMessageBrokerHost());
        props.put(TransportConstants.PORT_PROP_NAME, webProperties.getMessageBrokerCorePort());
        props.put(TransportConstants.PROTOCOLS_PROP_NAME, "CORE");
        props.put(TransportConstants.CONNECTION_TTL, Long.toString(Duration.ofMinutes(2).toMillis()));
        props.put(TransportConstants.NETTY_CONNECT_TIMEOUT, Long.toString(Duration.ofMinutes(1).toMillis()));
        log.info("coreProps: props={}", props);
        return props;
    }

    private Map<String, Object> managerCoreProps() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put(TransportConstants.SCHEME_PROP_NAME, "tcp");
        props.put(TransportConstants.HOST_PROP_NAME, webProperties.getManagerMessageBrokerHost());
        props.put(TransportConstants.PORT_PROP_NAME, webProperties.getManagerMessageBrokerCorePort());
        props.put(TransportConstants.PROTOCOLS_PROP_NAME, "CORE");
        props.put(TransportConstants.CONNECTION_TTL, Long.toString(Duration.ofMinutes(2).toMillis()));
        props.put(TransportConstants.NETTY_CONNECT_TIMEOUT, Long.toString(Duration.ofMinutes(1).toMillis()));
        log.info("managerCoreProps: props={}", props);
        return props;
    }

    private Map<String, Object> kioskCoreProps() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put(TransportConstants.SCHEME_PROP_NAME, "tcp");
        props.put(TransportConstants.HOST_PROP_NAME, webProperties.getKioskMessageBrokerHost());
        props.put(TransportConstants.PORT_PROP_NAME, webProperties.getKioskMessageBrokerCorePort());
        props.put(TransportConstants.PROTOCOLS_PROP_NAME, "CORE");
        props.put(TransportConstants.CONNECTION_TTL, Long.toString(Duration.ofMinutes(2).toMillis()));
        props.put(TransportConstants.NETTY_CONNECT_TIMEOUT, Long.toString(Duration.ofMinutes(1).toMillis()));
        log.info("kioskCoreProps: props={}", props);
        return props;
    }
}

//        log.info("configuration={}", configuration);
//        log.info("configuration.getConnectorConfigurations()={}", configuration.getConnectorConfigurations());
//        log.info("configuration.getAcceptorConfigurations()={}", configuration.getAcceptorConfigurations());
//        log.info("configuration.getClusterConfigurations()={}", configuration.getClusterConfigurations());

//        props.put("anycastPrefix", "/queue/");
//        props.put("multicastPrefix", "/topic/");

//        List<String> managerConnectorNames = new ArrayList<>();
//        Optional<Set<String>> optionalManagerHosts = worldService.findManagerHosts();
//        if (optionalManagerHosts.isPresent()) {
//            int index = 0;
//            for (String managerHost : optionalManagerHosts.get()) {
//                String managerConnectorName = "manager-core-connector-" + index;
//
//                props = new LinkedHashMap<>();
//                props.put(TransportConstants.SCHEME_PROP_NAME, "tcp");
//                props.put(TransportConstants.HOST_PROP_NAME, managerHost);
//                props.put(TransportConstants.PORT_PROP_NAME, MANAGER_MESSAGE_BROKER_PORT);
//                props.put(TransportConstants.PROTOCOLS_PROP_NAME, "CORE");
//                configuration.addConnectorConfiguration(managerConnectorName,
//                        new TransportConfiguration(NettyConnectorFactory.class.getName(), props));
//
//                log.info("Will connect {} to manager message broker at {}", managerConnectorName, managerHost);
//
//                managerConnectorNames.add(managerConnectorName);
//
//                index++;
//            }
//        }

//        UDPBroadcastEndpointFactory broadcastEndpoint = new UDPBroadcastEndpointFactory();
//        broadcastEndpoint.setGroupAddress("231.7.7.7");
//        broadcastEndpoint.setGroupPort(9876);
//
//        BroadcastGroupConfiguration broadcastGroup = new BroadcastGroupConfiguration();
//        broadcastGroup.setName("adhoc-broadcast-group");
//        broadcastGroup.setEndpointFactory(broadcastEndpoint);
//        broadcastGroup.setConnectorInfos(Arrays.asList("core-connector"));
//        configuration.addBroadcastGroupConfiguration(broadcastGroup);
//
//        DiscoveryGroupConfiguration discoveryGroup = new DiscoveryGroupConfiguration();
//        discoveryGroup.setName("adhoc-discovery-group");
//        discoveryGroup.setBroadcastEndpointFactory(broadcastEndpoint);
//        configuration.addDiscoveryGroupConfiguration("adhoc-discovery-group", discoveryGroup);

//        try {
//            configuration.addConnectorConfiguration("stomp-connector", "tcp://localhost:61613?protocols=STOMP");
//            configuration.addAcceptorConfiguration("stomp-acceptor", "tcp://localhost:61613?protocols=STOMP");
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }

//        clusterConnection.setDiscoveryGroupName("adhoc-discovery-group");
