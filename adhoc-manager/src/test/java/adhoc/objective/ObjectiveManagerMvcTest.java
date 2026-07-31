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

package adhoc.objective;

import adhoc.AbstractManagerMvcTest;
import adhoc.area.AreaEntity;
import adhoc.area.AreaRepository;
import adhoc.faction.FactionEntity;
import adhoc.faction.FactionRepository;
import adhoc.region.RegionEntity;
import adhoc.region.RegionRepository;
import adhoc.server.ServerEntity;
import adhoc.server.ServerRepository;
import adhoc.system.auth.AdhocServerUserDetails;
import com.google.common.collect.Sets;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

public class ObjectiveManagerMvcTest extends AbstractManagerMvcTest {

    @Autowired
    private FactionRepository factionRepository;
    @Autowired
    private ServerRepository serverRepository;
    @Autowired
    private AreaRepository areaRepository;
    @Autowired
    private RegionRepository regionRepository;
    @Autowired
    private ObjectiveRepository objectiveRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testPutServerObjectives() throws Exception {

        // ARRANGE

        int faction1Index = entityManager.createQuery(
                        "SELECT f.index FROM Faction f ORDER BY f.index DESC LIMIT 1", Integer.class)
                .getSingleResult() + 1;
        FactionEntity faction1 = new FactionEntity(faction1Index, "Faction 1", "#0000FF", 0);
        faction1 = factionRepository.save(faction1);

        RegionEntity region = new RegionEntity("Region 1", "Region0001", 10, -22, 0);
        region = regionRepository.save(region);

        AreaEntity area = new AreaEntity(region, 0, "Area 1", 33, -44, 55, 10, 20, 30);
        area = areaRepository.save(area);

        ObjectiveEntity objective1 = new ObjectiveEntity(region, 0, "Existing Objective 1", 0, 0, 0);
        objective1 = objectiveRepository.save(objective1);

        // add a spurious existing linked objective that we expect to be deleted
        ObjectiveEntity spuriousObjective = new ObjectiveEntity(region, 10, "Spurious Objective", 10, 20, 30);
        spuriousObjective.setLinkedObjectives(Sets.newHashSet(objective1));
        objective1.setLinkedObjectives(Sets.newHashSet(spuriousObjective));
        spuriousObjective = objectiveRepository.save(spuriousObjective);

        ServerEntity server = new ServerEntity(region, List.of(area), true, true);
        server = serverRepository.save(server);

        // ACT

        MvcTestResult result = mvc.post().uri("/adhoc_api/servers/%d/objectives".formatted(server.getId()))
                .with(user(new AdhocServerUserDetails("SERVER", "server")))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(
                        ObjectiveDto.builder()
                                .regionId(region.getId())
                                .index(0)
                                .name("Objective 1")
                                .x(BigDecimal.valueOf(100))
                                .y(BigDecimal.valueOf(101))
                                .z(BigDecimal.valueOf(102))
                                .initialFactionIndex(faction1Index)
                                .linkedObjectiveIndexes(List.of(1, 2))
                                .areaIndex(0)
                                .build(),
                        ObjectiveDto.builder()
                                .regionId(region.getId())
                                .index(1)
                                .name("Objective 2")
                                .x(BigDecimal.valueOf(-200))
                                .y(BigDecimal.valueOf(-201))
                                .z(BigDecimal.valueOf(-202))
                                .initialFactionIndex(faction1Index)
                                .linkedObjectiveIndexes(List.of(0))
                                .areaIndex(0)
                                .build(),
                        ObjectiveDto.builder()
                                .regionId(region.getId())
                                .index(2)
                                .name("Objective 3")
                                .x(BigDecimal.valueOf(300.400))
                                .y(BigDecimal.valueOf(-301.401))
                                .z(BigDecimal.valueOf(302.402))
                                .initialFactionIndex(null)
                                .linkedObjectiveIndexes(List.of(0))
                                .areaIndex(null)
                                .build())))
                .exchange();

        // ASSERT

        List<ObjectiveEntity> objectives = entityManager.createQuery(
                        "SELECT o FROM Objective o WHERE o.region.id = ?1 ORDER BY o.index ASC", ObjectiveEntity.class)
                .setParameter(1, region.getId())
                .getResultList();
        assertThat(objectives.size()).isEqualTo(3);
        objective1 = objectives.get(0);
        ObjectiveEntity objective2 = objectives.get(1);
        ObjectiveEntity objective3 = objectives.get(2);
        // TODO: check objectives

        assertThat(result)
                .hasStatusOk()
                .hasContentType(MediaType.APPLICATION_JSON)
                .bodyJson().isEqualTo(objectMapper.writeValueAsString(List.of(
                        ObjectiveDto.builder()
                                .id(objective1.getId())
                                .version(1L)
                                .regionId(region.getId())
                                .index(0)
                                .name("Objective 1")
                                .x(BigDecimal.valueOf(100))
                                .y(BigDecimal.valueOf(101))
                                .z(BigDecimal.valueOf(102))
                                .initialFactionId(faction1.getId())
                                .initialFactionIndex(faction1Index)
                                .factionId(null)
                                .factionIndex(null)
                                .linkedObjectiveIds(List.of(objective2.getId(), objective3.getId()))
                                .linkedObjectiveIndexes(List.of(1, 2))
                                .areaId(area.getId())
                                .areaIndex(0)
                                .build(),
                        ObjectiveDto.builder()
                                .id(objective2.getId())
                                .version(0L)
                                .regionId(region.getId())
                                .index(1)
                                .name("Objective 2")
                                .x(BigDecimal.valueOf(-200))
                                .y(BigDecimal.valueOf(-201))
                                .z(BigDecimal.valueOf(-202))
                                .initialFactionId(faction1.getId())
                                .initialFactionIndex(faction1Index)
                                .factionId(faction1.getId())
                                .factionIndex(faction1Index)
                                .linkedObjectiveIds(List.of(objective1.getId()))
                                .linkedObjectiveIndexes(List.of(0))
                                .areaId(area.getId())
                                .areaIndex(0)
                                .build(),
                        ObjectiveDto.builder()
                                .id(objective3.getId())
                                .version(0L)
                                .regionId(region.getId())
                                .index(2)
                                .name("Objective 3")
                                .x(BigDecimal.valueOf(300.400))
                                .y(BigDecimal.valueOf(-301.401))
                                .z(BigDecimal.valueOf(302.402))
                                .initialFactionId(null)
                                .initialFactionIndex(null)
                                .factionId(null)
                                .factionIndex(null)
                                .linkedObjectiveIds(List.of(objective1.getId()))
                                .linkedObjectiveIndexes(List.of(0))
                                .areaId(null)
                                .areaIndex(null)
                                .build())));
    }
}
