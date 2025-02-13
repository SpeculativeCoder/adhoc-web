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

package adhoc.universe;

import adhoc.area.Area;
import adhoc.area.AreaRepository;
import adhoc.faction.Faction;
import adhoc.faction.FactionRepository;
import adhoc.message.MessageService;
import adhoc.objective.Objective;
import adhoc.objective.ObjectiveRepository;
import adhoc.region.Region;
import adhoc.region.RegionRepository;
import adhoc.server.ServerRepository;
import adhoc.system.properties.CoreProperties;
import adhoc.system.properties.ManagerProperties;
import adhoc.universe.event.UniverseUpdatedEvent;
import adhoc.user.User;
import adhoc.user.UserRepository;
import adhoc.user.UserRole;
import com.google.common.base.Verify;
import com.google.common.collect.Sets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class UniverseInitializeService {

    private final CoreProperties coreProperties;
    private final ManagerProperties managerProperties;

    private final UniverseRepository universeRepository;
    private final UserRepository userRepository;
    private final FactionRepository factionRepository;
    private final RegionRepository regionRepository;
    private final AreaRepository areaRepository;
    private final ObjectiveRepository objectiveRepository;
    private final ServerRepository serverRepository;

    private final UniverseService universeService;
    private final MessageService messageService;

    private final Environment environment;

    private final PasswordEncoder passwordEncoder;

    /**
     * Inserts some initial data to set up the universe e.g. factions.
     */
    @EventListener(ApplicationStartedEvent.class)
    public void initializeDefaultUniverse() {

        if (universeRepository.existsById(UniverseService.UNIVERSE_ID)) {
            return; // already initialised the universe - no need to do anything
        }

        Universe universe = new Universe();
        universe = universeRepository.save(universe);

        Verify.verify(universe.getId().equals(UniverseService.UNIVERSE_ID)); // TODO

        // insert factions

        Faction team1 = new Faction();
        //team1.setId(1L);
        team1.setIndex(0);
        team1.setName("Alpha");
        team1.setColor("#FFFF00");
        team1.setScore(BigDecimal.valueOf(0.0));
        team1 = factionRepository.save(team1);

        Faction team2 = new Faction();
        //team2.setId(2L);
        team2.setIndex(1);
        team2.setName("Beta");
        team2.setColor("#00AAFF");
        team2.setScore(BigDecimal.valueOf(0.0));
        team2 = factionRepository.save(team2);

        Faction team3 = new Faction();
        //team3.setId(3L);
        team3.setIndex(2);
        team3.setName("Gamma");
        team3.setColor("#AA00FF");
        team3.setScore(BigDecimal.valueOf(0.0));
        team3 = factionRepository.save(team3);

        //Faction team4 = new Faction();
        ////team4.setId(4L);
        //team4.setIndex(3);
        //team4.setName("Delta");
        //team4.setColor("#FF2200");
        //team4.setScore(BigDecimal.valueOf(0.0));
        //team4 = factionRepository.save(team4);

        Region region1 = new Region();
        //region1.setId(1L);
        region1.setName("Region 1");
        // TODO: handle multiple regions
        region1.setMapName(coreProperties.getUnrealProjectRegionMaps().get(0));
        region1.setX(BigDecimal.valueOf(1750.0));
        region1.setY(BigDecimal.valueOf(1000.0));
        region1.setZ(BigDecimal.valueOf(50.0));
        region1.setAreas(Collections.emptyList());
        region1 = regionRepository.save(region1);

        Area area1 = new Area();
        area1.setIndex(0);
        area1.setName("A");
        area1.setX(BigDecimal.valueOf(1250.0));
        area1.setY(BigDecimal.valueOf(1800.0));
        area1.setZ(BigDecimal.valueOf(0.0));
        area1.setSizeX(BigDecimal.valueOf(2000.0));
        area1.setSizeY(BigDecimal.valueOf(750.0));
        area1.setSizeZ(BigDecimal.valueOf(1000.0));
        area1.setRegion(region1);
        area1 = areaRepository.save(area1);

        Area area2 = new Area();
        area2.setIndex(1);
        area2.setName("B");
        area2.setX(BigDecimal.valueOf(1500.0));
        area2.setY(BigDecimal.valueOf(750.0));
        area2.setZ(BigDecimal.valueOf(0.0));
        area2.setSizeX(BigDecimal.valueOf(1500.0));
        area2.setSizeY(BigDecimal.valueOf(1250.0));
        area2.setSizeZ(BigDecimal.valueOf(1000.0));
        area2.setRegion(region1);
        area2 = areaRepository.save(area2);

        region1.setAreas(Arrays.asList(area1, area2));

        //Server server1 = new Server();
        //server1.setRegion(region1);
        //server1.setAreas(Arrays.asList(area1));
        //server1.setMapName(region1.getMapName());
        //server1.setX(region1.getX());
        //server1.setY(region1.getY());
        //server1.setZ(region1.getZ());
        //server1.setEnabled(true);
        //server1.setActive(false);
        //server1 = serverRepository.save(server1);
        //
        //area1.setServer(server1);

        //Server server2 = new Server();
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
        objectiveA1.setX(BigDecimal.valueOf(1500.0));
        objectiveA1.setY(BigDecimal.valueOf(1500.0));
        objectiveA1.setZ(BigDecimal.valueOf(0.0));
        objectiveA1.setFaction(team1);
        objectiveA1.setInitialFaction(team1);
        objectiveA1.setArea(area1);
        objectiveA1 = objectiveRepository.save(objectiveA1);

        Objective objectiveA2 = new Objective();
        objectiveA2.setRegion(region1);
        objectiveA2.setIndex(1);
        objectiveA2.setName("A2");
        objectiveA2.setX(BigDecimal.valueOf(500.0));
        objectiveA2.setY(BigDecimal.valueOf(2000.0));
        objectiveA2.setZ(BigDecimal.valueOf(0.0));
        objectiveA2.setFaction(team3);
        objectiveA2.setInitialFaction(team3);
        objectiveA2.setArea(area1);
        objectiveA2 = objectiveRepository.save(objectiveA2);

        Objective objectiveA3 = new Objective();
        objectiveA3.setRegion(region1);
        objectiveA3.setIndex(2);
        objectiveA3.setName("A3");
        objectiveA3.setX(BigDecimal.valueOf(2000.0));
        objectiveA3.setY(BigDecimal.valueOf(1800.0));
        objectiveA3.setZ(BigDecimal.valueOf(0.0));
        //objectiveA3.setFaction(team4);
        //objectiveA3.setInitialFaction(team4);
        objectiveA3.setArea(area1);
        objectiveA3 = objectiveRepository.save(objectiveA3);

        Objective objectiveB1 = new Objective();
        objectiveB1.setRegion(region1);
        objectiveB1.setIndex(3);
        objectiveB1.setName("B1");
        objectiveB1.setX(BigDecimal.valueOf(1200.0));
        objectiveB1.setY(BigDecimal.valueOf(1000.0));
        objectiveB1.setZ(BigDecimal.valueOf(0.0));
        objectiveB1.setFaction(team2);
        objectiveB1.setInitialFaction(team2);
        objectiveB1.setArea(area2);
        objectiveB1 = objectiveRepository.save(objectiveB1);

        Objective objectiveB2 = new Objective();
        objectiveB2.setRegion(region1);
        objectiveB2.setIndex(4);
        objectiveB2.setName("B2");
        objectiveB2.setX(BigDecimal.valueOf(1800.0));
        objectiveB2.setY(BigDecimal.valueOf(500.0));
        objectiveB2.setZ(BigDecimal.valueOf(0.0));
        objectiveB2.setArea(area2);
        objectiveB2 = objectiveRepository.save(objectiveB2);

        objectiveA1.setLinkedObjectives(Arrays.asList(objectiveB1, objectiveA2, objectiveA3, objectiveB2)
                .stream().map(Objective::getId).map(objectiveRepository::getReferenceById).collect(Collectors.toSet()));
        objectiveA2.setLinkedObjectives(Arrays.asList(objectiveA1, objectiveB1)
                .stream().map(Objective::getId).map(objectiveRepository::getReferenceById).collect(Collectors.toSet()));
        objectiveA3.setLinkedObjectives(Arrays.asList(objectiveA1, objectiveB2)
                .stream().map(Objective::getId).map(objectiveRepository::getReferenceById).collect(Collectors.toSet()));
        objectiveB1.setLinkedObjectives(Arrays.asList(objectiveA1, objectiveA2, objectiveB2)
                .stream().map(Objective::getId).map(objectiveRepository::getReferenceById).collect(Collectors.toSet()));
        objectiveB2.setLinkedObjectives(Arrays.asList(objectiveA1, objectiveA3, objectiveB1)
                .stream().map(Objective::getId).map(objectiveRepository::getReferenceById).collect(Collectors.toSet()));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime userCreated = now.minusMinutes(30);
        LocalDateTime userSeen = now.minusMinutes(5);

        // admin user and some faction specific users for testing

        if (coreProperties.getFeatureFlags().contains("development")) {
            User adminUser = new User();
            adminUser.setName("admin");
            adminUser.setEmail("admin@" + coreProperties.getAdhocDomain());
            adminUser.setFaction(team1);
            adminUser.setHuman(true);
            adminUser.setScore(BigDecimal.valueOf(0.0));
            adminUser.setPassword(passwordEncoder.encode(managerProperties.getDefaultAdminPassword()));
            adminUser.setCreated(userCreated);
            adminUser.setUpdated(userCreated);
            adminUser.setSeen(userSeen);
            adminUser.setRoles(Sets.newHashSet(UserRole.USER, UserRole.DEBUG)); // TODO: restore User.Role.ADMIN,
            adminUser = userRepository.save(adminUser);

            User alphaUser = new User();
            alphaUser.setName("TestAlpha");
            alphaUser.setEmail("testalpha@" + coreProperties.getAdhocDomain());
            alphaUser.setFaction(team1);
            alphaUser.setHuman(true);
            alphaUser.setScore(BigDecimal.valueOf(0.0));
            alphaUser.setPassword(passwordEncoder.encode(managerProperties.getDefaultUserPassword()));
            alphaUser.setCreated(userCreated);
            alphaUser.setUpdated(userCreated);
            alphaUser.setSeen(userSeen);
            alphaUser.setRoles(Sets.newHashSet(UserRole.USER, UserRole.DEBUG));
            alphaUser = userRepository.save(alphaUser);

            User betaUser = new User();
            betaUser.setName("TestBeta");
            betaUser.setEmail("testbeta@" + coreProperties.getAdhocDomain());
            betaUser.setFaction(team2);
            betaUser.setHuman(true);
            betaUser.setScore(BigDecimal.valueOf(10.0));
            betaUser.setPassword(passwordEncoder.encode(managerProperties.getDefaultUserPassword()));
            betaUser.setCreated(userCreated);
            betaUser.setUpdated(userCreated);
            betaUser.setSeen(userSeen);
            betaUser.setRoles(Sets.newHashSet(UserRole.USER, UserRole.DEBUG));
            betaUser = userRepository.save(betaUser);

            User gammaUser = new User();
            gammaUser.setName("TestGamma");
            gammaUser.setEmail("testgamma@" + coreProperties.getAdhocDomain());
            gammaUser.setFaction(team3);
            gammaUser.setHuman(true);
            gammaUser.setScore(BigDecimal.valueOf(20.0));
            gammaUser.setPassword(passwordEncoder.encode(managerProperties.getDefaultUserPassword()));
            gammaUser.setCreated(userCreated);
            gammaUser.setUpdated(userCreated);
            //gammaUser.setSeen(seen);
            gammaUser.setRoles(Sets.newHashSet(UserRole.USER, UserRole.DEBUG));
            gammaUser = userRepository.save(gammaUser);

            //User deltaUser = new User();
            //deltaUser.setName("TestDelta");
            //deltaUser.setEmail("testdelta@" + coreProperties.getAdhocDomain());
            //deltaUser.setFaction(team4);
            //deltaUser.setHuman(true);
            //deltaUser.setScore(BigDecimal.valueOf(30.0));
            //deltaUser.setPassword(passwordEncoder.encode(managerProperties.getDefaultUserPassword()));
            //deltaUser.setCreated(userCreated);
            //deltaUser.setUpdated(userCreated);
            ////deltaUser.setSeen(seen);
            //deltaUser.setRoles(Sets.newHashSet(UserRole.USER, UserRole.DEBUG));
            //deltaUser = userRepository.save(deltaUser);
        }

        //Pawn pawn1 = new Pawn();
        //pawn1.setName("Pawn 1");
        //pawn1.setServer(server1);
        //pawn1.setIndex(0);
        //pawn1.setFaction(blueTeam);
        //pawn1.setX(200.0);
        //pawn1.setY(200.0);
        //pawn1.setZ(0.0);
        //pawn1.setSeen(LocalDateTime.now());
        //pawn1.setUser(alphaUser);
        //pawn1 = pawnRepository.save(pawn1);

        messageService.addGlobalMessage(String.format("Universe %d initialized", universe.getId()));
    }

    UniverseUpdatedEvent toUniverseUpdatedEvent(Universe universe) {
        return new UniverseUpdatedEvent(universeService.toDto(universe));
    }
}
