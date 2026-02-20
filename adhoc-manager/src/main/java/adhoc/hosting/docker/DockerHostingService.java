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

package adhoc.hosting.docker;

import adhoc.hosting.HostingService;
import adhoc.hosting.docker.properties.DockerHostingProperties;
import adhoc.server.ServerDto;
import adhoc.shared.properties.CoreProperties;
import adhoc.shared.properties.ManagerProperties;
import adhoc.task.TaskDto;
import adhoc.task.TaskEntity;
import adhoc.task.kiosk.KioskTaskEntity;
import adhoc.task.manager.ManagerTaskEntity;
import adhoc.task.server.ServerTaskEntity;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.transport.DockerHttpClient;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * Implementation of hosting service using local Docker.
 * Good for locally testing multiple Unreal servers running at the same time.
 */
@Service
@Profile("hosting-docker")
@Slf4j
@RequiredArgsConstructor
public class DockerHostingService implements HostingService {

    private static final int PUBLIC_WEB_SOCKET_PORT_BASE = 8889;

    private static final Pattern SERVER_ID_PATTERN = Pattern.compile("^SERVER_ID=([0-9]+)$");

    private final CoreProperties coreProperties;
    private final ManagerProperties managerProperties;
    private final DockerHostingProperties dockerHostingProperties;

    private final ServerProperties serverProperties;

    private DockerClientConfig dockerClientConfig() {
        return new DefaultDockerClientConfig.Builder()
                .withDockerHost(dockerHostingProperties.getDockerHost())
                .withRegistryUsername("")
                .withRegistryUrl("")
                //.withApiVersion("1.44")
                .build();
    }

    private DockerHttpClient dockerHttpClient() {
        //return new ApacheDockerHttpClient.Builder()
        return new ZerodepDockerHttpClient.Builder()
                .dockerHost(dockerClientConfig().getDockerHost())
                .connectionTimeout(Duration.ofSeconds(5))
                .responseTimeout(Duration.ofSeconds(5))
                .build();
    }

    private DockerClient dockerClient() {
        return DockerClientImpl.getInstance(dockerClientConfig(), dockerHttpClient());
    }

    @Override
    public List<TaskEntity> poll() {
        log.debug("Polling Docker...");

        DockerClient dockerClient = dockerClient();

        List<Container> containers = dockerClient.listContainersCmd().exec();
        log.trace("containers: {}", containers);

        List<TaskEntity> tasks = new ArrayList<>();

        // assume manager running on Docker host (unless we find an adhoc_manager container in Docker)
        ManagerTaskEntity defaultManagerTask = new ManagerTaskEntity();
        defaultManagerTask.setTaskIdentifier("host manager task");
        defaultManagerTask.setPrivateIp("host.docker.internal");
        defaultManagerTask.setPublicIp("127.0.0.1");
        tasks.add(defaultManagerTask);
        // assume kiosk running on Docker host (unless we find an adhoc_kiosk container in Docker)
        KioskTaskEntity defaultKioskTask = new KioskTaskEntity();
        defaultKioskTask.setTaskIdentifier("host kiosk task");
        defaultKioskTask.setPrivateIp("host.docker.internal");
        defaultKioskTask.setPublicIp("127.0.0.1");
        tasks.add(defaultKioskTask);

        for (Container container : containers) {
            //log.debug("state: {}", container.getState());

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
                tasks.remove(defaultManagerTask);

                ManagerTaskEntity managerTask = new ManagerTaskEntity();
                managerTask.setTaskIdentifier(inspectedContainer.getId());
                managerTask.setPrivateIp(privateIp);
                managerTask.setPublicIp("127.0.0.1");
                tasks.add(managerTask);

            } else if (containerImage.contains(managerProperties.getKioskImage())) {
                // as we have found a kiosk - remove the default assumption that the kiosk is the docker host
                tasks.remove(defaultKioskTask);

                KioskTaskEntity kioskTask = new KioskTaskEntity();
                kioskTask.setTaskIdentifier(inspectedContainer.getId());
                kioskTask.setPrivateIp(privateIp);
                kioskTask.setPublicIp("127.0.0.1");
                tasks.add(kioskTask);

            } else if (containerImage.contains(managerProperties.getServerImage())) {
                for (String env : Objects.requireNonNull(inspectedContainer.getConfig().getEnv())) {
                    Matcher serverIdMatcher = SERVER_ID_PATTERN.matcher(env);
                    if (serverIdMatcher.matches()) {
                        Long serverId = parseServerId(serverIdMatcher.group(1));

                        ServerTaskEntity task = new ServerTaskEntity();
                        task.setTaskIdentifier(inspectedContainer.getId());
                        task.setPrivateIp(privateIp);
                        task.setPublicIp("127.0.0.1");
                        task.setPublicWebSocketPort(calculatePublicWebSocketPort(serverId));
                        task.setServerId(serverId);

                        tasks.add(task);
                        break;
                    }
                }
            }
        }

        return tasks;
    }

    @Override
    public TaskDto startServerTask(ServerDto server) {
        log.debug("Starting Docker container for {}", server); // linked to managers {}", managerHosts);
        int publicWebSocketPort = calculatePublicWebSocketPort(server.getId());

        DockerClient dockerClient = dockerClient();

        CreateContainerResponse createdContainer = dockerClient
                .createContainerCmd(managerProperties.getServerImage() + ":latest")
                //.withName(server.getId() + "-" + managerProperties.getServerImage())
                .withEnv(Arrays.asList(
                        String.format("UNREAL_PROJECT_NAME=%s", coreProperties.getUnrealProjectName()),
                        String.format("MAP_NAME=%s", server.getMapName()),
                        String.format("SERVER_ID=%d", server.getId()),
                        String.format("MANAGER_HOST=%s", "host.docker.internal"), // TODO
                        //String.format("MANAGER_HOST=%s", managerHosts.iterator().next()),
                        //String.format("INITIAL_MANAGER_HOSTS=%s", String.join(",", managerHosts)),
                        String.format("REGION_ID=%d", server.getRegionId()),
                        //String.format("INITIAL_AREA_IDS=%s",
                        //        server.getAreas().stream()
                        //                .map(Area::getId)
                        //                .map(Object::toString)
                        //                .collect(Collectors.joining(","))),
                        String.format("INITIAL_AREA_INDEXES=%s", server.getAreaIndexes().stream()
                                .map(Object::toString)
                                .collect(Collectors.joining(","))),
                        String.format("MAX_CONTROLLERS=%d", managerProperties.getMaxControllers()),
                        String.format("MAX_PLAYERS=%d", managerProperties.getMaxPlayers()),
                        String.format("MAX_BOTS=%d", managerProperties.getMaxBots()),
                        String.format("FEATURE_FLAGS=%s", coreProperties.getFeatureFlags()),
                        String.format("SSL_ENABLED=%s", serverProperties.getSsl().isEnabled()),
                        String.format("CA_CERTIFICATE=%s", serverProperties.getSsl().isEnabled() ? multilineEnvironmentVariable(serverProperties.getSsl().getTrustCertificate()) : "unused"),
                        String.format("SERVER_CERTIFICATE=%s", serverProperties.getSsl().isEnabled() ? multilineEnvironmentVariable(serverProperties.getSsl().getCertificate()) : "unused"),
                        String.format("PRIVATE_KEY=%s", serverProperties.getSsl().isEnabled() ? multilineEnvironmentVariable(serverProperties.getSsl().getCertificatePrivateKey()) : "unused"),
                        String.format("SERVER_BASIC_AUTH_PASSWORD=%s", coreProperties.getServerBasicAuthPassword())))
                .withHostConfig(
                        HostConfig.newHostConfig()
                                .withPortBindings(
                                        new PortBinding(
                                                new Ports.Binding("0.0.0.0", Integer.toString(publicWebSocketPort)),
                                                ExposedPort.tcp(8889)))
                                .withAutoRemove(dockerHostingProperties.getAutoRemove() == Boolean.TRUE))
                .exec();
        log.trace("createdContainer: {}", createdContainer);

        dockerClient.startContainerCmd(createdContainer.getId()).exec();

        //InspectContainerResponse inspectedContainer = dockerClient.inspectContainerCmd(createdContainer.getId()).exec();

        TaskDto serverTask = TaskDto.builder()
                .taskIdentifier(createdContainer.getId())
                .publicWebSocketPort(publicWebSocketPort)
                .serverId(server.getId())
                .build();

        return serverTask;
    }

    @Override
    public void stopServerTask(String taskIdentifier) {
        dockerClient()
                .removeContainerCmd(taskIdentifier)
                .withForce(true)
                .exec();
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

    private String multilineEnvironmentVariable(String path) {
        try {
            return Files.readString(Path.of(path)).replaceAll("\n", "\\\\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
