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
import adhoc.region.RegionEntity;
import adhoc.region.RegionRepository;
import adhoc.server.ServerEntity;
import adhoc.server.ServerRepository;
import adhoc.system.auth.AdhocServerUserDetails;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

public class ObjectiveManagerMvcTest extends AbstractManagerMvcTest {

    @Autowired
    private ServerRepository serverRepository;
    @Autowired
    private AreaRepository areaRepository;
    @Autowired
    private RegionRepository regionRepository;
    @Autowired
    private ObjectiveRepository objectiveRepository;

    @Test
    public void testPutServerObjectives() throws Exception {
        RegionEntity region = regionRepository.findById(1L).orElseThrow();
        AreaEntity area = areaRepository.findById(1L).orElseThrow();

        ObjectiveEntity objective1 = objectiveRepository.save(new ObjectiveEntity(region, 1, "Objective 1", 0d, 0d, 0d));

        // add a spurious existing linked objective that we expect to be deleted
        ObjectiveEntity objective10 = new ObjectiveEntity(region, 10, "Objective 10", 10d, 20d, 30d);
        objective10.setLinkedObjectives(Sets.newHashSet(objective1));
        objective10 = objectiveRepository.save(objective10);

        objective1.setLinkedObjectives(Sets.newHashSet(objective10));

        ServerEntity server = serverRepository.save(new ServerEntity(region, List.of(area)));

        MvcTestResult result = mvc.post().uri("/adhoc_api/servers/%d/objectives".formatted(server.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf())
                .with(user(new AdhocServerUserDetails("SERVER", "server")))
                .content("""
                        [ {
                            "regionId": 1, "index": 0,
                            "name": "A1",
                            "x": 100, "y": 101, "z": 102,
                            "initialFactionIndex": 0,
                            "linkedObjectiveIndexes": [ 1, 2 ],
                            "areaIndex": 0
                        }, {
                            "regionId": 1, "index": 1,
                            "name": "A2",
                            "x": -200, "y": -201, "z": -203,
                            "initialFactionIndex": null,
                            "linkedObjectiveIndexes": [ 0 ],
                            "areaIndex": 0
                        }, {
                            "regionId": 1, "index": 2,
                            "name": "B1",
                            "x": 0, "y": 0, "z": 0,
                            "initialFactionIndex": null,
                            "linkedObjectiveIndexes": [ 0 ],
                            "areaIndex": null
                        } ]
                        """)
                .exchange();

        assertThat(result)
                .hasStatusOk()
                .hasContentType(MediaType.APPLICATION_JSON);

        List<ObjectiveEntity> objectives = objectiveRepository.findAll();
        assertThat(objectives).hasSize(3);

        assertThat(result).bodyJson().isEqualTo("""
                 [ {
                     "id": 3, "version": 0,
                     "regionId": 1, "index": 0,
                     "name": "A1",
                     "x": 100, "y": 101, "z": 102,
                     "initialFactionId": 1,
                     "initialFactionIndex": 0,
                     "factionId": 1,
                     "factionIndex": 0,
                     "linkedObjectiveIds": [ 1, 4 ],
                     "linkedObjectiveIndexes": [ 1, 2 ],
                     "areaId": 1,
                     "areaIndex": 0
                 }, {
                     "id": 1, "version": 1,
                     "regionId": 1, "index": 1,
                     "name": "A2",
                     "x": -200, "y": -201, "z": -203,
                     "initialFactionId": null,
                     "initialFactionIndex": null,
                     "factionId": null,
                     "factionIndex": null,
                     "linkedObjectiveIds": [ 3 ],
                     "linkedObjectiveIndexes": [ 0 ],
                     "areaId": 1,
                     "areaIndex": 0
                }, {
                     "id": 4, "version": 0,
                     "regionId": 1, "index": 2,
                     "name": "B1",
                     "x": 0, "y": 0, "z": 0,
                     "initialFactionId": null,
                     "initialFactionIndex": null,
                     "factionId": null,
                     "factionIndex": null,
                     "linkedObjectiveIds": [ 3 ],
                     "linkedObjectiveIndexes": [ 0 ],
                     "areaId": null,
                     "areaIndex": null
                } ]
                """);
    }
}
