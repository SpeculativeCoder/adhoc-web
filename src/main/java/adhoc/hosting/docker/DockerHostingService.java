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

package adhoc.hosting.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import adhoc.area.Area;
import adhoc.hosting.HostingService;
import adhoc.hosting.HostingState;
import adhoc.hosting.ServerTask;
import adhoc.ManagerProperties;
import adhoc.server.Server;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Implementation of hosting service using local Docker.
 * Good for locally testing multiple Unreal servers running at the same time.
 */
@Transactional
@Service
@Profile("hosting-docker")
@Slf4j
@RequiredArgsConstructor
public class DockerHostingService implements HostingService {

    private static final int PUBLIC_WEB_SOCKET_PORT_BASE = 8889;

    private static final String DEFAULT_MANAGER_HOST = "host.docker.internal";
    private static final String DEFAULT_KIOSK_HOST = "host.docker.internal";
    private static final Pattern SERVER_ID_PATTERN = Pattern.compile("^SERVER_ID=([0-9]+)$");

    private final ManagerProperties managerProperties;

    @Value("${adhoc.feature-flags}")
    private String featureFlags;

    @Value("${adhoc.docker.host}")
    private String dockerHost;

    @Value("${server.ssl.trust-certificate}")
    private Path caCertificate;

    @Value("${server.ssl.certificate}")
    private Path serverCertificate;

    @Value("${server.ssl.certificate-private-key}")
    private Path privateKey;

    @EventListener
    public void contextRefreshed(ContextRefreshedEvent event) {
        log.info("dockerHost={}", dockerHost);
    }

    private DockerClientConfig dockerClientConfig() {
        return new DefaultDockerClientConfig.Builder()
                .withDockerHost(dockerHost)
                .withRegistryUsername("")
                .withRegistryUrl("")
                .withApiVersion("1.41")
                .build();
    }

    private DockerHttpClient dockerHttpClient() {
        return new ApacheDockerHttpClient.Builder()
                .dockerHost(dockerClientConfig().getDockerHost())
                .connectionTimeout(Duration.ofSeconds(5))
                .responseTimeout(Duration.ofSeconds(5))
                .build();
    }

    private DockerClient dockerClient() {
        return DockerClientImpl.getInstance(dockerClientConfig(), dockerHttpClient());
    }

    @Override
    public HostingState poll() {
        log.debug("Polling Docker...");

        DockerClient dockerClient = dockerClient();

        List<Container> containers = dockerClient.listContainersCmd().exec();
        log.trace("containers: {}", containers);

        HostingState hostingState = new HostingState();
        hostingState.setManagerHosts(new LinkedHashSet<>());
        hostingState.setKioskHosts(new LinkedHashSet<>());
        hostingState.setServerTasks(new LinkedHashMap<>());

        // assume manager running on Docker host (unless we find a adhoc_manager container in Docker)
        hostingState.getManagerHosts().add(DEFAULT_MANAGER_HOST);
        // assume kiosk running on Docker host (unless we find a adhoc_kiosk container in Docker)
        hostingState.getManagerHosts().add(DEFAULT_KIOSK_HOST);

        for (Container container : containers) {
            InspectContainerResponse inspectedContainer = dockerClient.inspectContainerCmd(container.getId()).exec();
            log.trace("inspectedContainer: {}", inspectedContainer);
            log.trace("env: {}", (Object[]) inspectedContainer.getConfig().getEnv());
            log.trace("networks: {}", inspectedContainer.getNetworkSettings().getNetworks());

            ContainerNetwork bridgeNetwork = inspectedContainer.getNetworkSettings().getNetworks().get("bridge");
            if (bridgeNetwork == null) {
                continue;
            }

            String privateIp = bridgeNetwork.getIpAddress();
            log.debug("privateIp: {}", privateIp);
            String containerImage = container.getImage();
            log.debug("containerImage: {}", containerImage);

            if (containerImage.contains(managerProperties.getManagerImage())) {
                // as we have found a manager - remove the default assumption that the manager is the docker host
                hostingState.getManagerHosts().remove(DEFAULT_MANAGER_HOST);
                hostingState.getManagerHosts().add(privateIp);

            } else if (containerImage.contains(managerProperties.getKioskImage())) {
                // as we have found a kiosk - remove the default assumption that the kiosk is the docker host
                hostingState.getKioskHosts().remove(DEFAULT_KIOSK_HOST);
                hostingState.getKioskHosts().add(privateIp);

            } else if (containerImage.contains(managerProperties.getServerImage())) {
                for (String env : Objects.requireNonNull(inspectedContainer.getConfig().getEnv())) {
                    Matcher serverIdMatcher = SERVER_ID_PATTERN.matcher(env);
                    if (serverIdMatcher.matches()) {
                        Long serverId = parseServerId(serverIdMatcher.group(1));

                        ServerTask task = new ServerTask();
                        task.setTaskId(inspectedContainer.getId());
                        //task.setServerId(serverId);
                        task.setPrivateIp(privateIp);
                        task.setPublicIp("127.0.0.1");
                        task.setPublicWebSocketPort(calculatePublicWebSocketPort(serverId));

                        hostingState.getServerTasks().put(serverId, task);
                        break;
                    }
                }
            }
        }

        return hostingState;
    }

    private static Long parseServerId(String serverIdString) {
        try {
            return Long.valueOf(serverIdString);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Failed to parse server ID", e);
        }
    }

    private static int calculatePublicWebSocketPort(Long serverId) {
        return PUBLIC_WEB_SOCKET_PORT_BASE + serverId.intValue();
    }

    @Override
    public void startServerTask(Server server) { //, Set<String> managerHosts) {
        log.info("Starting Docker container for server {}", server.getId()); // linked to managers {}", managerHosts);
        int publicWebSocketPort = calculatePublicWebSocketPort(server.getId());

        CreateContainerResponse createdContainer = dockerClient()
                .createContainerCmd(managerProperties.getServerImage() + ":latest")
                .withEnv(Arrays.asList(
                        String.format("SERVER_ID=%d", server.getId()),
                        String.format("MANAGER_HOST=%s", "host.docker.internal"), // TODO
                        //String.format("MANAGER_HOST=%s", managerHosts.iterator().next()),
                        //String.format("INITIAL_MANAGER_HOSTS=%s", String.join(",", managerHosts)),
                        String.format("REGION_ID=%d", server.getRegion().getId()),
                        //String.format("INITIAL_AREA_IDS=%s",
                        //        server.getAreas().stream()
                        //                .map(Area::getId)
                        //                .map(Object::toString)
                        //                .collect(Collectors.joining(","))),
                        String.format("INITIAL_AREA_INDEXES=%s",
                                server.getAreas().stream()
                                        .map(Area::getIndex)
                                        .map(Object::toString)
                                        .collect(Collectors.joining(","))),
                        String.format("MAX_PAWNS=%d", managerProperties.getMaxPawns()),
                        String.format("MAX_PLAYERS=%d", managerProperties.getMaxPlayers()),
                        String.format("MAX_BOTS=%d", managerProperties.getMaxBots()),
                        String.format("FEATURE_FLAGS=%s", featureFlags),
                        String.format("CA_CERTIFICATE=%s", multilineEnvironmentVariable(caCertificate)),
                        String.format("SERVER_CERTIFICATE=%s", multilineEnvironmentVariable(serverCertificate)),
                        String.format("PRIVATE_KEY=%s", multilineEnvironmentVariable(privateKey))))
                .withHostConfig(
                        HostConfig.newHostConfig()
                                .withPortBindings(
                                        new PortBinding(
                                                new Ports.Binding("0.0.0.0", Integer.toString(publicWebSocketPort)),
                                                ExposedPort.tcp(8889)))
                                .withAutoRemove(true))
                .exec();
        log.trace("createdContainer: {}", createdContainer);

        dockerClient().startContainerCmd(createdContainer.getId()).exec();
    }

    private String multilineEnvironmentVariable(Path path) {
        try {
            return Files.readString(path).replaceAll("\n", "\\\\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stopServerTask(ServerTask task) {
        dockerClient()
                .removeContainerCmd(task.getTaskId())
                .withForce(true)
                .exec();
    }

}
