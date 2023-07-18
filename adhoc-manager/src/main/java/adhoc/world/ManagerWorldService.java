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

package adhoc.world;

import adhoc.area.Area;
import adhoc.area.AreaRepository;
import adhoc.dns.DnsService;
import adhoc.faction.Faction;
import adhoc.faction.FactionRepository;
import adhoc.objective.Objective;
import adhoc.objective.ObjectiveRepository;
import adhoc.properties.ManagerProperties;
import adhoc.web.properties.WebProperties;
import adhoc.region.Region;
import adhoc.region.RegionRepository;
import adhoc.server.Server;
import adhoc.server.ServerRepository;
import adhoc.user.User;
import adhoc.user.UserRepository;
import adhoc.world.event.WorldUpdatedEvent;
import com.google.common.collect.Sets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Transactional
@Service
@Slf4j
@RequiredArgsConstructor
public class ManagerWorldService {

    private final WebProperties webProperties;

    private final WorldService worldService;

    private final ManagerProperties managerProperties;
    private final WorldRepository worldRepository;
    private final UserRepository userRepository;
    private final FactionRepository factionRepository;
    private final RegionRepository regionRepository;
    private final AreaRepository areaRepository;
    private final ObjectiveRepository objectiveRepository;
    private final ServerRepository serverRepository;

    private final PasswordEncoder passwordEncoder;
    private final SimpMessageSendingOperations stomp;
    private final DnsService dnsService;

    /**
     * Inserts some initial data to set up the world e.g. factions.
     */
    @EventListener(ApplicationStartedEvent.class)
    public void initializeDefaultWorld() {

        if (worldRepository.existsById(WorldService.WORLD_ID)) {
            return; // already initialised the world - no need to do anything
        }

        World world = new World();
        world.setId(WorldService.WORLD_ID);
        world = worldRepository.save(world);

        // insert factions

        Faction team1 = new Faction();
        team1.setId(1L);
        team1.setIndex(0);
        team1.setName("Alpha");
        team1.setColor("#0088FF");
        team1.setScore(0F);
        team1 = factionRepository.save(team1);

        Faction team2 = new Faction();
        team2.setId(2L);
        team2.setIndex(1);
        team2.setName("Beta");
        team2.setColor("#FF2200");
        team2.setScore(0F);
        team2 = factionRepository.save(team2);

        Faction team3 = new Faction();
        team3.setId(3L);
        team3.setIndex(2);
        team3.setName("Gamma");
        team3.setColor("#FFFF00");
        team3.setScore(0F);
        team3 = factionRepository.save(team3);

        Faction team4 = new Faction();
        team4.setId(4L);
        team4.setIndex(3);
        team4.setName("Delta");
        team4.setColor("#8800FF");
        team4.setScore(0F);
        team4 = factionRepository.save(team4);

        Region region1 = new Region();
        region1.setId(1L);
        region1.setName("1");
        region1.setMapName("Region0001");
        region1.setX(1750F);
        region1.setY(1000F);
        region1.setZ(50F);
        region1.setAreas(Collections.emptyList());
        region1 = regionRepository.save(region1);

        Area area1 = new Area();
        area1.setIndex(0);
        area1.setName("A");
        area1.setX(1250F);
        area1.setY(1800F);
        area1.setZ(0F);
        area1.setSizeX(2000F);
        area1.setSizeY(750F);
        area1.setSizeZ(1000F);
        area1.setRegion(region1);
        area1 = areaRepository.save(area1);

        Area area2 = new Area();
        area2.setIndex(1);
        area2.setName("B");
        area2.setX(1500F);
        area2.setY(750F);
        area2.setZ(0F);
        area2.setSizeX(1500F);
        area2.setSizeY(1250F);
        area2.setSizeZ(1000F);
        area2.setRegion(region1);
        area2 = areaRepository.save(area2);

        region1.setAreas(Arrays.asList(area1, area2));

        Server server1 = new Server();
        server1.setName("1");
        server1.setMapName(region1.getMapName());
        server1.setX(region1.getX());
        server1.setY(region1.getY());
        server1.setZ(region1.getZ());
        server1.setRegion(region1);
        server1.setAreas(Arrays.asList(area1));
        server1.setStatus(Server.Status.INACTIVE);
        server1 = serverRepository.save(server1);

        area1.setServer(server1);

        //Server server2 = new Server();
        //server2.setName("2");
        //server2.setX(area2.getX());
        //server2.setY(area2.getY());
        //server2.setZ(area2.getZ());
        //server2.setHostingType(AdhocApplication.hostingType);
        //if (AdhocApplication.serverPublicIps.size() >= 2) {
        //    server2.setPublicIp(AdhocApplication.serverPublicIps.get(1));
        //    server2.setStatus(Server.Status.ACTIVE);
        //    server2.setSeen(LocalDateTime.now());
        //} else {
        //    server2.setStatus(Server.Status.INACTIVE);
        //}
        //server2.setAreas(Sets.newHashSet());
        //server2 = serverRepository.save(server2);

        // some startup objectives for the map - this will get clobbered once the first server reports in but nice to have something

        Objective objectiveA1 = new Objective();
        objectiveA1.setRegion(region1);
        objectiveA1.setIndex(0);
        objectiveA1.setName("A1");
        objectiveA1.setX(1500F);
        objectiveA1.setY(1500F);
        objectiveA1.setZ(0F);
        objectiveA1.setFaction(team1);
        objectiveA1.setInitialFaction(team1);
        objectiveA1.setArea(area1);
        objectiveA1 = objectiveRepository.save(objectiveA1);

        Objective objectiveA2 = new Objective();
        objectiveA2.setRegion(region1);
        objectiveA2.setIndex(1);
        objectiveA2.setName("A2");
        objectiveA2.setX(500F);
        objectiveA2.setY(2000F);
        objectiveA2.setZ(0F);
        objectiveA2.setFaction(team3);
        objectiveA2.setInitialFaction(team3);
        objectiveA2.setArea(area1);
        objectiveA2 = objectiveRepository.save(objectiveA2);

        Objective objectiveA3 = new Objective();
        objectiveA3.setRegion(region1);
        objectiveA3.setIndex(2);
        objectiveA3.setName("A3");
        objectiveA3.setX(2000F);
        objectiveA3.setY(1800F);
        objectiveA3.setZ(0F);
        objectiveA3.setFaction(team4);
        objectiveA3.setInitialFaction(team4);
        objectiveA3.setArea(area1);
        objectiveA3 = objectiveRepository.save(objectiveA3);

        Objective objectiveB1 = new Objective();
        objectiveB1.setRegion(region1);
        objectiveB1.setIndex(3);
        objectiveB1.setName("B1");
        objectiveB1.setX(1200F);
        objectiveB1.setY(1000F);
        objectiveB1.setZ(0F);
        objectiveB1.setFaction(team2);
        objectiveB1.setInitialFaction(team2);
        objectiveB1.setArea(area2);
        objectiveB1 = objectiveRepository.save(objectiveB1);

        Objective objectiveB2 = new Objective();
        objectiveB2.setRegion(region1);
        objectiveB2.setIndex(4);
        objectiveB2.setName("B2");
        objectiveB2.setX(1800F);
        objectiveB2.setY(500F);
        objectiveB2.setZ(0F);
        objectiveB2.setArea(area2);
        objectiveB2 = objectiveRepository.save(objectiveB2);

        objectiveA1.setLinkedObjectives(Arrays.asList(objectiveB1, objectiveA2, objectiveA3, objectiveB2)
                .stream().map(Objective::getId).map(objectiveRepository::getReferenceById).collect(Collectors.toList()));
        objectiveA2.setLinkedObjectives(Arrays.asList(objectiveA1, objectiveB1)
                .stream().map(Objective::getId).map(objectiveRepository::getReferenceById).collect(Collectors.toList()));
        objectiveA3.setLinkedObjectives(Arrays.asList(objectiveA1, objectiveB2)
                .stream().map(Objective::getId).map(objectiveRepository::getReferenceById).collect(Collectors.toList()));
        objectiveB1.setLinkedObjectives(Arrays.asList(objectiveA1, objectiveA2, objectiveB2)
                .stream().map(Objective::getId).map(objectiveRepository::getReferenceById).collect(Collectors.toList()));
        objectiveB2.setLinkedObjectives(Arrays.asList(objectiveA1, objectiveA3, objectiveB1)
                .stream().map(Objective::getId).map(objectiveRepository::getReferenceById).collect(Collectors.toList()));

        // admin user and some faction specific users for testing

        if (userRepository.count() < 1) {

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime created = now.minusMinutes(30);
            LocalDateTime seen = now.minusMinutes(5);

            User u0 = new User();
            u0.setName("admin");
            u0.setEmail("admin@" + webProperties.getAdhocDomain());
            u0.setFaction(team1);
            u0.setScore(0F);
            u0.setPassword(passwordEncoder.encode(managerProperties.getDefaultAdminPassword()));
            u0.setCreated(created);
            u0.setUpdated(created);
            u0.setSeen(seen);
            //if (webProperties.getFeatureFlags().contains("development")) {
            u0.setRoles(Sets.newHashSet(User.Role.USER)); // TODO: restore User.Role.ADMIN,
            //}
            u0 = userRepository.save(u0);

            User u1 = new User();
            u1.setName("AlphaTester");
            u1.setEmail("alphatester@" + webProperties.getAdhocDomain());
            u1.setFaction(team1);
            u1.setScore(0F);
            u1.setPassword(passwordEncoder.encode(managerProperties.getDefaultUserPassword()));
            u1.setCreated(created);
            u1.setUpdated(created);
            u1.setSeen(seen);
            u1.setRoles(Sets.newHashSet(User.Role.USER));
            u1 = userRepository.save(u1);

            User u2 = new User();
            u2.setName("BetaTester");
            u2.setEmail("betatester@" + webProperties.getAdhocDomain());
            u2.setFaction(team2);
            u2.setScore(10F);
            u2.setPassword(passwordEncoder.encode(managerProperties.getDefaultUserPassword()));
            u2.setCreated(created);
            u2.setUpdated(created);
            u2.setSeen(seen);
            u2.setRoles(Sets.newHashSet(User.Role.USER));
            u2 = userRepository.save(u2);

            User u3 = new User();
            u3.setName("DeltaTester");
            u3.setEmail("deltatester@" + webProperties.getAdhocDomain());
            u3.setFaction(team3);
            u3.setScore(20F);
            u3.setPassword(passwordEncoder.encode(managerProperties.getDefaultUserPassword()));
            u3.setCreated(created);
            u3.setUpdated(created);
            //u3.setSeen(seen);
            u3.setRoles(Sets.newHashSet(User.Role.USER));
            u3 = userRepository.save(u3);

            User u4 = new User();
            u4.setName("GammaTester");
            u4.setEmail("gammatester@" + webProperties.getAdhocDomain());
            u4.setFaction(team4);
            u4.setScore(30F);
            u4.setPassword(passwordEncoder.encode(managerProperties.getDefaultUserPassword()));
            u4.setCreated(created);
            u4.setUpdated(created);
            //u4.setSeen(seen);
            u4.setRoles(Sets.newHashSet(User.Role.USER));
            u4 = userRepository.save(u4);

            //Pawn pawn1 = new Pawn();
            //pawn1.setName("Pawn 1");
            //pawn1.setServer(server1);
            //pawn1.setIndex(0);
            //pawn1.setFaction(blueTeam);
            //pawn1.setX(200F);
            //pawn1.setY(200F);
            //pawn1.setZ(0F);
            //pawn1.setSeen(LocalDateTime.now());
            //pawn1.setUser(u1);
            //pawn1 = pawnRepository.save(pawn1);
        }
    }

    public void updateManagerAndKioskHosts(Set<String> managerHosts, Set<String> kioskHosts) {
        World world = worldRepository.getWorldById(WorldService.WORLD_ID);

        boolean emitEvent = false;

        if (!world.getManagerHosts().equals(managerHosts)) {
            world.setManagerHosts(managerHosts);
            // TODO: move to some dns service
            dnsService.createOrUpdateDnsRecord(managerProperties.getManagerDomain(), managerHosts);
            emitEvent = true;
        }
        if (!world.getKioskHosts().equals(kioskHosts)) {
            world.setKioskHosts(kioskHosts);
            // TODO: move to some dns service
            dnsService.createOrUpdateDnsRecord(managerProperties.getKioskDomain(), kioskHosts);
            emitEvent = true;
        }

        if (emitEvent) {
            sendWorldUpdatedEvent(world);
        }
    }

    private void sendWorldUpdatedEvent(World world) {
        WorldUpdatedEvent event = WorldUpdatedEvent.builder()
                .world(worldService.toDto(world))
                .build();

        log.info("Sending: {}", event);
        stomp.convertAndSend("/topic/events", event);
    }
}
