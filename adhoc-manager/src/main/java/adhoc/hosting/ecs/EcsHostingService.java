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

package adhoc.hosting.ecs;

import adhoc.area.AreaEntity;
import adhoc.hosting.HostingService;
import adhoc.hosting.ecs.properties.EcsHostingProperties;
import adhoc.server.ServerEntity;
import adhoc.system.properties.CoreProperties;
import adhoc.system.properties.ManagerProperties;
import adhoc.task.KioskTaskEntity;
import adhoc.task.ManagerTaskEntity;
import adhoc.task.ServerTaskEntity;
import adhoc.task.TaskEntity;
import com.google.common.collect.ImmutableList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeNetworkInterfacesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeNetworkInterfacesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeSecurityGroupsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSecurityGroupsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsResponse;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.NetworkInterface;
import software.amazon.awssdk.services.ec2.model.SecurityGroup;
import software.amazon.awssdk.services.ecs.EcsClient;
import software.amazon.awssdk.services.ecs.model.AssignPublicIp;
import software.amazon.awssdk.services.ecs.model.Attachment;
import software.amazon.awssdk.services.ecs.model.AwsVpcConfiguration;
import software.amazon.awssdk.services.ecs.model.CapacityProviderStrategyItem;
import software.amazon.awssdk.services.ecs.model.Container;
import software.amazon.awssdk.services.ecs.model.ContainerOverride;
import software.amazon.awssdk.services.ecs.model.DescribeTasksRequest;
import software.amazon.awssdk.services.ecs.model.DescribeTasksResponse;
import software.amazon.awssdk.services.ecs.model.KeyValuePair;
import software.amazon.awssdk.services.ecs.model.ListTasksRequest;
import software.amazon.awssdk.services.ecs.model.ListTasksResponse;
import software.amazon.awssdk.services.ecs.model.NetworkConfiguration;
import software.amazon.awssdk.services.ecs.model.RunTaskRequest;
import software.amazon.awssdk.services.ecs.model.RunTaskResponse;
import software.amazon.awssdk.services.ecs.model.StopTaskRequest;
import software.amazon.awssdk.services.ecs.model.TaskOverride;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of hosting service using AWS ECS.
 */
@Service
@Profile("hosting-ecs")
@Slf4j
@RequiredArgsConstructor
public class EcsHostingService implements HostingService {

    private final CoreProperties coreProperties;

    private final ManagerProperties managerProperties;

    private final EcsHostingProperties ecsHostingProperties;

    private Ec2Client ec2Client() {
        return Ec2Client.builder()
                .region(Region.of(ecsHostingProperties.getAwsRegion()))
                .credentialsProvider(credentialsProvider())
                .build();
    }

    private EcsClient ecsClient() {
        return EcsClient.builder()
                .region(Region.of(ecsHostingProperties.getAwsRegion()))
                .credentialsProvider(credentialsProvider())
                .build();
    }

    private AwsCredentialsProvider credentialsProvider() {
        return DefaultCredentialsProvider.builder()
                .profileName(ecsHostingProperties.getAwsProfile())
                .build();
    }

    public List<TaskEntity> poll() {
        log.trace("Polling ECS container service tasks...");

        // we will do a lookup of public IPs using the network interface IDs after we have looked through all the tasks
        Map<String, ManagerTaskEntity> managerNetworkInterfaceIds = new LinkedHashMap<>();
        Map<String, KioskTaskEntity> kioskNetworkInterfaceIds = new LinkedHashMap<>();
        Map<String, ServerTaskEntity> serverNetworkInterfaceIds = new LinkedHashMap<>();

        try (EcsClient ecsClient = ecsClient();
             Ec2Client ec2Client = ec2Client()) {

            ListTasksResponse listTasksResponse =
                    ecsClient.listTasks(ListTasksRequest.builder()
                            .cluster(ecsHostingProperties.getEcsCluster()).build());
            log.trace("listTasksResponse: {}", listTasksResponse);

            if (!listTasksResponse.hasTaskArns() || listTasksResponse.taskArns().isEmpty()) {
                log.debug("No tasks to examine");
                return Collections.emptyList(); // TODO
            }

            DescribeTasksResponse describeTasksResponse =
                    ecsClient.describeTasks(DescribeTasksRequest.builder()
                            .cluster(ecsHostingProperties.getEcsCluster()).tasks(listTasksResponse.taskArns()).build());
            log.trace("describeTasksResponse: {}", describeTasksResponse);

            for (software.amazon.awssdk.services.ecs.model.Task task : describeTasksResponse.tasks()) {
                log.trace("task: {}", task);

                String networkInterfaceId = null;
                String privateIp = null;

                // get information about the network interface (we want the network interface ID and private IP address)
                for (Attachment attachment : task.attachments()) {
                    log.trace("attachment: {}", attachment);

                    if ("ElasticNetworkInterface".equals(attachment.type())) {
                        for (KeyValuePair detail : attachment.details()) {
                            log.trace("detail: {}", detail);

                            if ("networkInterfaceId".equals(detail.name())) {
                                networkInterfaceId = detail.value();
                                log.trace("networkInterfaceId: {}", networkInterfaceId);

                            } else if ("privateIPv4Address".equals(detail.name())) {
                                privateIp = detail.value();
                                log.trace("privateIp: {}", privateIp);
                            }
                        }
                    }
                }

                // check the type of task
                for (Container container : task.containers()) {
                    log.trace("container: {}", container);
                    String containerName = container.name();
                    log.trace("containerName: {}", containerName);

                    if (managerProperties.getManagerImage().equals(containerName) && networkInterfaceId != null) {
                        log.trace("Found manager. networkInterfaceId={}", networkInterfaceId);

                        ManagerTaskEntity managerTask = new ManagerTaskEntity();
                        managerTask.setTaskIdentifier(task.taskArn());
                        managerTask.setPrivateIp(privateIp);
                        managerNetworkInterfaceIds.put(networkInterfaceId, managerTask);

                    } else if (managerProperties.getKioskImage().equals(containerName) && networkInterfaceId != null) {
                        log.trace("Found kiosk. networkInterfaceId={}", networkInterfaceId);

                        KioskTaskEntity kioskTask = new KioskTaskEntity();
                        kioskTask.setTaskIdentifier(task.taskArn());
                        kioskTask.setPrivateIp(privateIp);
                        kioskNetworkInterfaceIds.put(networkInterfaceId, kioskTask);

                    } else if (managerProperties.getServerImage().equals(containerName)) {
                        log.trace("Found server. networkInterfaceId={}", networkInterfaceId);

                        // get the environment variable for server ID that was set when the container was launched
                        for (ContainerOverride containerOverride : task.overrides().containerOverrides()) {
                            log.trace("containerOverride: {}", containerOverride);

                            for (KeyValuePair env : containerOverride.environment()) {
                                log.trace("env name: {}", env.name());

                                if ("SERVER_ID".equals(env.name())) {
                                    Long serverId;
                                    try {
                                        serverId = Long.valueOf(env.value());
                                    } catch (NumberFormatException e) {
                                        throw new RuntimeException("Failed to parse server ID", e);
                                    }

                                    ServerTaskEntity serverTask = new ServerTaskEntity();
                                    serverTask.setTaskIdentifier(task.taskArn());
                                    serverTask.setPrivateIp(privateIp);
                                    serverTask.setServerId(serverId);
                                    serverNetworkInterfaceIds.put(networkInterfaceId, serverTask);
                                }
                            }
                        }
                    }
                }
            }

            Set<String> networkInterfaceIds = new LinkedHashSet<>();
            networkInterfaceIds.addAll(managerNetworkInterfaceIds.keySet());
            networkInterfaceIds.addAll(kioskNetworkInterfaceIds.keySet());
            networkInterfaceIds.addAll(serverNetworkInterfaceIds.keySet());

            // retrieve public IPs via each network interface ID (we do this in bulk as one call)
            if (!networkInterfaceIds.isEmpty()) {
                DescribeNetworkInterfacesResponse describeNetworkInterfacesResponse =
                        ec2Client.describeNetworkInterfaces(DescribeNetworkInterfacesRequest.builder()
                                .networkInterfaceIds(ImmutableList.copyOf(networkInterfaceIds)).build());
                log.trace("describeNetworkInterfacesResponse: {}", describeNetworkInterfacesResponse);

                for (NetworkInterface networkInterface : describeNetworkInterfacesResponse.networkInterfaces()) {
                    log.trace("networkInterface: {}", networkInterface);

                    String networkInterfaceId = networkInterface.networkInterfaceId();
                    String publicIp = networkInterface.association().publicIp();
                    log.trace("networkInterfaceId: {}", networkInterfaceId);
                    log.trace("publicIp: {}", publicIp);

                    ManagerTaskEntity managerTask = managerNetworkInterfaceIds.get(networkInterfaceId);
                    if (managerTask != null) {
                        log.trace("Found manager: publicIp={}", publicIp);
                        managerTask.setPublicIp(publicIp);
                    }

                    KioskTaskEntity kioskTask = kioskNetworkInterfaceIds.get(networkInterfaceId);
                    if (kioskTask != null) {
                        log.trace("Found kiosk: publicIp={}", publicIp);
                        kioskTask.setPublicIp(publicIp);
                    }

                    ServerTaskEntity serverTask = serverNetworkInterfaceIds.get(networkInterfaceId);
                    if (serverTask != null) {
                        log.trace("Found server: publicIp={}", publicIp);
                        serverTask.setPublicIp(publicIp);
                        serverTask.setPublicWebSocketPort(8889);
                    }
                }
            }
        }

        return Stream.concat(
                Stream.concat(
                        managerNetworkInterfaceIds.values().stream(),
                        kioskNetworkInterfaceIds.values().stream()),
                serverNetworkInterfaceIds.values().stream()).toList();
    }

    public ServerTaskEntity startServerTask(ServerEntity server) {
        log.debug("Starting task for {}", server);

        try (EcsClient ecsClient = ecsClient();
             Ec2Client ec2Client = ec2Client()) {

            DescribeSecurityGroupsRequest describeSecurityGroupsRequest = DescribeSecurityGroupsRequest.builder()
                    .filters(
                            Filter.builder().name("group-name").values(ecsHostingProperties.getAwsSecurityGroupName()).build())
                    .build();
            log.debug("describeSecurityGroupsRequest: {}", describeSecurityGroupsRequest);

            DescribeSecurityGroupsResponse describeSecurityGroupsResponse =
                    ec2Client.describeSecurityGroups(describeSecurityGroupsRequest);
            log.debug("describeSecurityGroupsResponse: {}", describeSecurityGroupsResponse);

            if (describeSecurityGroupsResponse.securityGroups().size() != 1) {
                throw new IllegalStateException("expected 1 security group but got: " + describeSecurityGroupsResponse.securityGroups());
            }

            SecurityGroup securityGroup = describeSecurityGroupsResponse.securityGroups().get(0);

            if (!securityGroup.groupName().equals(ecsHostingProperties.getAwsSecurityGroupName())) {
                throw new IllegalStateException("expected security group with name " + ecsHostingProperties.getAwsSecurityGroupName() + " but got: " +
                        securityGroup.groupName());
            }

            String securityGroupId = securityGroup.groupId();
            log.debug("securityGroupId: {}", securityGroupId);

            // TODO: better way to have the vpc-id / availability-zone chosen
            DescribeSubnetsRequest describeSubnetsRequest = DescribeSubnetsRequest.builder()
                    .filters(
                            Filter.builder().name("vpc-id").values(securityGroup.vpcId()).build(),
                            Filter.builder().name("availability-zone").values(ecsHostingProperties.getAwsAvailabilityZone()).build())
                    .build();
            log.debug("describeSubnetsRequest: {}", describeSubnetsRequest);

            // TODO: should be random subnet of those available for use
            DescribeSubnetsResponse describeSubnetsResponse =
                    ec2Client.describeSubnets(describeSubnetsRequest);
            log.debug("describeSubnetsResponse: {}", describeSubnetsResponse);

            if (describeSubnetsResponse.subnets().size() != 1) {
                throw new IllegalStateException("expected 1 subnet but got: " + describeSubnetsResponse.subnets());
            }

            if (!describeSubnetsResponse.subnets().get(0).availabilityZone().equals(ecsHostingProperties.getAwsAvailabilityZone())) {
                throw new IllegalStateException("expected subnet with availability zone " + ecsHostingProperties.getAwsAvailabilityZone() + " but got: " +
                        describeSubnetsResponse.subnets().get(0).availabilityZone());
            }

            String subnetId = describeSubnetsResponse.subnets().get(0).subnetId();
            log.debug("subnetId: {}", subnetId);

            RunTaskRequest runTaskRequest = RunTaskRequest.builder()
                    //.launchType(LaunchType.FARGATE)
                    .capacityProviderStrategy(CapacityProviderStrategyItem.builder().capacityProvider("FARGATE_SPOT").build())
                    .taskDefinition(managerProperties.getServerImage())
                    .overrides(TaskOverride.builder().containerOverrides(ContainerOverride.builder()
                            .name(managerProperties.getServerImage())
                            .environment(
                                    KeyValuePair.builder().name("MAP_NAME").value(server.getMapName()).build(),
                                    KeyValuePair.builder().name("SERVER_ID").value(server.getId().toString()).build(),
                                    //KeyValuePair.builder().name("MANAGER_HOST").value(managerHosts.iterator().next()).build(),
                                    //KeyValuePair.builder().name("INITIAL_MANAGER_HOSTS")
                                    //       .value(String.join(",", managerHosts)).build(),
                                    KeyValuePair.builder().name("REGION_ID").value(server.getRegion().getId().toString()).build(),
                                    //KeyValuePair.builder().name("INITIAL_AREA_IDS")
                                    //        .value(server.getAreas().stream()
                                    //                .map(Area::getId)
                                    //                .map(Object::toString)
                                    //                .collect(Collectors.joining(","))).build(),
                                    KeyValuePair.builder().name("INITIAL_AREA_INDEXES")
                                            .value(server.getAreas().stream()
                                                    .map(AreaEntity::getIndex)
                                                    .map(Object::toString)
                                                    .collect(Collectors.joining(","))).build(),
                                    KeyValuePair.builder().name("MAX_CONTROLLERS").value(managerProperties.getMaxControllers().toString()).build(),
                                    KeyValuePair.builder().name("MAX_PLAYERS").value(managerProperties.getMaxPlayers().toString()).build(),
                                    KeyValuePair.builder().name("MAX_BOTS").value(managerProperties.getMaxBots().toString()).build(),
                                    KeyValuePair.builder().name("FEATURE_FLAGS").value(coreProperties.getFeatureFlags()).build())
                            .build()).build())
                    .cluster(ecsHostingProperties.getEcsCluster())
                    .count(1)
                    .networkConfiguration(
                            NetworkConfiguration.builder()
                                    .awsvpcConfiguration(
                                            AwsVpcConfiguration.builder()
                                                    .subnets(subnetId)
                                                    .assignPublicIp(AssignPublicIp.ENABLED)
                                                    .securityGroups(securityGroupId)
                                                    .build())
                                    .build())
                    .build();
            log.debug("runTaskRequest: {}", runTaskRequest);

            RunTaskResponse runTaskResponse = ecsClient.runTask(runTaskRequest);
            log.debug("runTaskResponse: {}", runTaskResponse);

            if (runTaskResponse.hasFailures() && !runTaskResponse.failures().isEmpty()) {
                log.error("Run task failure: {}", runTaskResponse.failures());
                // TODO: hosting exception
                throw new RuntimeException("Run task failure");
            }

            //log.info("tasks={}", runTaskResponse.tasks());
            String taskArn = runTaskResponse.tasks().getFirst().taskArn();
            log.debug("taskArn={}", taskArn);

            ServerTaskEntity serverTask = new ServerTaskEntity();
            serverTask.setTaskIdentifier(taskArn);
            serverTask.setPublicWebSocketPort(8889);
            serverTask.setServerId(server.getId());

            return serverTask;
        }
    }

    @Override
    public void stopServerTask(String taskIdentifier) {
        log.debug("Stopping {}", taskIdentifier);

        try (EcsClient ecsClient = ecsClient()) {
            ecsClient.stopTask(StopTaskRequest.builder().task(taskIdentifier).cluster(ecsHostingProperties.getEcsCluster()).build());
        }
    }
}

//@Value("${adhoc.server-container-service.aws-access-key-id}")
//private String awsAccessKeyId;
//@Value("${adhoc.server-container-service.aws-secret-access-key}")
//private String awsSecretAccessKey;

//    @Value("${adhoc.server-container-service.ecs-task-definition.web}")
//    private String webTaskDefinition;
//    @Value("${adhoc.server-container-service.ecs-task-definition.server}")
//    private String serverTaskDefinition;

//StaticCredentialsProvider
//return AwsBasicCredentials.create(awsAccessKeyId, awsSecretAccessKey);
//return AwsCredentialsProviderChain.builder().credentialsProviders(
//        ContainerCredentialsProvider.builder().build(),
//        ProfileCredentialsProvider.builder().profileName("adhoc").build()).build();
