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

import com.google.common.collect.Sets;
import adhoc.AdhocProperties;
import adhoc.area.Area;
import adhoc.area.AreaRepository;
import adhoc.dns.DnsService;
import adhoc.faction.Faction;
import adhoc.faction.FactionRepository;
import adhoc.ManagerProperties;
import adhoc.objective.Objective;
import adhoc.objective.ObjectiveRepository;
import adhoc.pawn.PawnRepository;
import adhoc.region.Region;
import adhoc.region.RegionRepository;
import adhoc.server.Server;
import adhoc.server.ServerRepository;
import adhoc.server.ServerStatus;
import adhoc.user.User;
import adhoc.user.UserRepository;
import adhoc.user.UserRole;
import adhoc.world.event.WorldUpdatedEvent;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Transactional
@Service
@Profile("mode-manager")
@Slf4j
@RequiredArgsConstructor
public class WorldManagerService {

    private final AdhocProperties adhocProperties;
    private final ManagerProperties managerProperties;
    private final WorldRepository worldRepository;
    private final UserRepository userRepository;
    private final FactionRepository factionRepository;
    private final RegionRepository regionRepository;
    private final AreaRepository areaRepository;
    private final ObjectiveRepository objectiveRepository;
    private final ServerRepository serverRepository;
    private final PawnRepository pawnRepository;
    private final PasswordEncoder passwordEncoder;
    private final SimpMessageSendingOperations stomp;
    private final DnsService dnsService;
    private final PlatformTransactionManager platformTransactionManager;

    @Value("${adhoc.feature-flags}")
    private String featureFlags;

    @Value("${adhoc.default-user-password}")
    private String defaultUserPassword;

    @Value("${adhoc.default-admin-password}")
    private String defaultAdminPassword;

    /**
     * Inserts some initial data to set up the world e.g. factions.
     */
    //@EventListener(ContextRefreshedEvent.class)
    //@Order(Ordered.HIGHEST_PRECEDENCE)
    //@PostConstruct
    @EventListener(ApplicationStartedEvent.class)
    public void initializeDefaultWorld() {
        //TransactionStatus transaction = platformTransactionManager.getTransaction(TransactionDefinition.withDefaults());

        World world = worldRepository.findById(WorldService.WORLD_ID).orElse(null);
        if (world != null) {
            return; // already initialised the world - no need to do anything
        }

        world = new World();
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
        server1.setStatus(ServerStatus.STOPPED);
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
        //    server2.setStatus(Server.Status.STARTED);
        //    server2.setSeen(LocalDateTime.now());
        //} else {
        //    server2.setStatus(Server.Status.STOPPED);
        //}
        //server2.setAreas(Sets.newHashSet());
        //server2 = serverRepository.save(server2);

        // some startup objectives for the map - this will get clobbered once the first server reports in but nice to have something

        Objective objective1 = new Objective();
        objective1.setRegion(region1);
        objective1.setIndex(0);
        objective1.setName("A1");
        objective1.setX(1500F);
        objective1.setY(1500F);
        objective1.setZ(0F);
        objective1.setFaction(team1);
        objective1.setInitialFaction(team1);
        objective1.setArea(area1);
        objective1 = objectiveRepository.save(objective1);

        Objective objective2 = new Objective();
        objective2.setRegion(region1);
        objective2.setIndex(1);
        objective2.setName("A2"); // "Training Yard");
        objective2.setX(1000F);
        objective2.setY(2000F);
        objective2.setZ(0F);
        objective2.setFaction(team3);
        objective2.setInitialFaction(team3);
        objective2.setArea(area1);
        objective2 = objectiveRepository.save(objective2);

        Objective objective3 = new Objective();
        objective3.setRegion(region1);
        objective3.setIndex(2);
        objective3.setName("A3"); // "Supply Building");
        objective3.setX(2000F);
        objective3.setY(2000F);
        objective3.setZ(0F);
        objective3.setFaction(team4);
        objective3.setInitialFaction(team4);
        objective3.setArea(area1);
        objective3 = objectiveRepository.save(objective3);

        Objective objective4 = new Objective();
        objective4.setRegion(region1);
        objective4.setIndex(3);
        objective4.setName("B1"); // "Barracks");
        objective4.setX(1000F);
        objective4.setY(1000F);
        objective4.setZ(0F);
        objective4.setFaction(team2);
        objective4.setInitialFaction(team2);
        objective4.setArea(area2);
        objective4 = objectiveRepository.save(objective4);

        Objective objective5 = new Objective();
        objective5.setRegion(region1);
        objective5.setIndex(4);
        objective5.setName("B2"); // "Bunker");
        objective5.setX(2000F);
        objective5.setY(1000F);
        objective5.setZ(0F);
        objective5.setArea(area2);
        objective5 = objectiveRepository.save(objective5);

        objective1.setLinkedObjectives(Arrays.asList(objective4, objective2, objective3, objective5)
                .stream().map(Objective::getId).map(objectiveRepository::getReferenceById).collect(Collectors.toList()));
        objective4.setLinkedObjectives(Arrays.asList(objective1, objective2)
                .stream().map(Objective::getId).map(objectiveRepository::getReferenceById).collect(Collectors.toList()));
        objective2.setLinkedObjectives(Arrays.asList(objective1, objective4)
                .stream().map(Objective::getId).map(objectiveRepository::getReferenceById).collect(Collectors.toList()));
        objective3.setLinkedObjectives(Arrays.asList(objective1, objective5)
                .stream().map(Objective::getId).map(objectiveRepository::getReferenceById).collect(Collectors.toList()));
        objective5.setLinkedObjectives(Arrays.asList(objective1, objective3)
                .stream().map(Objective::getId).map(objectiveRepository::getReferenceById).collect(Collectors.toList()));

        // admin user and some faction specific users for testing

        if (featureFlags.contains("development")) {
            User u0 = new User();
            u0.setName("admin");
            u0.setEmail("admin@" + adhocProperties.getAdhocDomain());
            u0.setFaction(team1);
            u0.setScore(0F);
            u0.setPassword(passwordEncoder.encode(defaultAdminPassword));
            u0.setCreated(LocalDateTime.now());
            u0.setUpdated(u0.getCreated());
            u0.setRoles(Sets.newHashSet(UserRole.USER)); // TODO: restore User.Role.ADMIN,
            u0 = userRepository.save(u0);

            User u1 = new User();
            u1.setName("AlphaTester");
            u1.setEmail("alphatester@" + adhocProperties.getAdhocDomain());
            u1.setFaction(team1);
            u1.setScore(0F);
            u1.setPassword(passwordEncoder.encode(defaultUserPassword));
            u1.setCreated(LocalDateTime.now());
            u1.setUpdated(u1.getCreated());
            u1.setRoles(Sets.newHashSet(UserRole.USER));
            u1 = userRepository.save(u1);

            User u2 = new User();
            u2.setName("BetaTester");
            u2.setEmail("betatester@" + adhocProperties.getAdhocDomain());
            u2.setFaction(team2);
            u2.setScore(10F);
            u2.setPassword(passwordEncoder.encode(defaultUserPassword));
            u2.setCreated(LocalDateTime.now());
            u2.setUpdated(u2.getCreated());
            u2.setRoles(Sets.newHashSet(UserRole.USER));
            u2 = userRepository.save(u2);

            User u3 = new User();
            u3.setName("DeltaTester");
            u3.setEmail("deltatester@" + adhocProperties.getAdhocDomain());
            u3.setFaction(team3);
            u3.setScore(20F);
            u3.setPassword(passwordEncoder.encode(defaultUserPassword));
            u3.setCreated(LocalDateTime.now());
            u3.setUpdated(u3.getCreated());
            u3.setRoles(Sets.newHashSet(UserRole.USER));
            u3 = userRepository.save(u3);

            User u4 = new User();
            u4.setName("GammaTester");
            u4.setEmail("gammatester@" + adhocProperties.getAdhocDomain());
            u4.setFaction(team4);
            u4.setScore(30F);
            u4.setPassword(passwordEncoder.encode(defaultUserPassword));
            u4.setCreated(LocalDateTime.now());
            u4.setUpdated(u4.getCreated());
            u4.setRoles(Sets.newHashSet(UserRole.USER));
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

        //platformTransactionManager.commit(transaction);
    }

    public void updateManagerAndKioskHosts(Set<String> managerHosts, Set<String> kioskHosts) {
        World world = worldRepository.getReferenceById(WorldService.WORLD_ID);

        boolean emitEvent = false;

        if (!world.getManagerHosts().equals(managerHosts)) {
            world.setManagerHosts(managerHosts);
            dnsService.createOrUpdateDnsRecord(managerProperties.getManagerDomain(), managerHosts);
            emitEvent = true;
        }
        if (!world.getKioskHosts().equals(kioskHosts)) {
            world.setKioskHosts(kioskHosts);
            dnsService.createOrUpdateDnsRecord(managerProperties.getKioskDomain(), kioskHosts);
            emitEvent = true;
        }

        if (emitEvent) {
            emitWorldUpdatedEvent(world);
        }
    }

    private void emitWorldUpdatedEvent(World world) {
        WorldUpdatedEvent event = WorldUpdatedEvent.builder()
                .id(WorldService.WORLD_ID)
                .version(world.getVersion())
                .managerHosts(new ArrayList<>(world.getManagerHosts()))
                .kioskHosts(new ArrayList<>(world.getKioskHosts()))
                .build();

        log.info("Sending: {}", event);
        stomp.convertAndSend("/topic/events", event);
    }
}
