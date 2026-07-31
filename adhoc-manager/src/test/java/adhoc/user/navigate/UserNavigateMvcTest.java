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

package adhoc.user.navigate;

import adhoc.AbstractManagerMvcTest;
import adhoc.area.AreaEntity;
import adhoc.area.AreaRepository;
import adhoc.faction.FactionEntity;
import adhoc.faction.FactionRepository;
import adhoc.region.RegionEntity;
import adhoc.region.RegionRepository;
import adhoc.server.ServerEntity;
import adhoc.server.ServerRepository;
import adhoc.system.auth.AdhocUserDetails;
import adhoc.user.UserEntity;
import adhoc.user.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

public class UserNavigateMvcTest extends AbstractManagerMvcTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FactionRepository factionRepository;
    @Autowired
    private ServerRepository serverRepository;
    @Autowired
    private AreaRepository areaRepository;
    @Autowired
    private RegionRepository regionRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testNavigate() throws Exception {

        // ARRANGE

        LocalDateTime priorTime = LocalDateTime.now();

        int factionIndex = entityManager.createQuery(
                        "SELECT f.index FROM Faction f ORDER BY f.index DESC LIMIT 1", Integer.class)
                .getSingleResult() + 1;
        FactionEntity faction = new FactionEntity(factionIndex, "Faction 1", "#0000FF", 0);
        faction = factionRepository.save(faction);

        RegionEntity region = new RegionEntity("Region 1", "Region0001", 10, -20, 0);
        region = regionRepository.save(region);

        AreaEntity area = new AreaEntity(region, 0, "Area 1", 0, 0, 0, 10, 10, 10);
        area = areaRepository.save(area);

        UserEntity user = new UserEntity("TestUser", "USER", faction, 0d);
        UUID priorToken = UUID.randomUUID();
        user.getState().setToken(priorToken);
        user = userRepository.save(user);

        ServerEntity server = new ServerEntity(region, List.of(area), true, true);
        server.setPublicIp("127.0.0.1");
        server.setPublicWebSocketPort(8889);
        server.setWebSocketUrl("wss://server.localhost:8889");
        server = serverRepository.save(server);

        // ACT

        MvcTestResult result = mvc.post().uri("/adhoc_api/users/navigate")
                .with(user(new AdhocUserDetails(user.getName(), user.getPassword(), true, user.getAuthorities(), user.getId())))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(UserNavigateRequest.builder()
                        .destinationServerId(server.getId())
                        .build()))
                .exchange();

        // ASSERT

        user = userRepository.findById(user.getId()).orElseThrow();
        assertThat(user.getState().getDestinationServer().getId()).isEqualTo(server.getId());
        assertThat(user.getState().getNavigated()).isAfterOrEqualTo(priorTime);
        assertThat(user.getState().getToken()).isNotNull().isNotEqualTo(priorToken);

        assertThat(result)
                .hasStatusOk()
                .hasContentType(MediaType.APPLICATION_JSON)
                .bodyJson().isEqualTo(objectMapper.writeValueAsString(UserNavigateResponse.builder()
                        .ip("127.0.0.1")
                        .port(8889)
                        .webSocketUrl("wss://server.localhost:8889")
                        .mapName("Region0001")
                        .userId(user.getId())
                        .factionId(faction.getId())
                        .token(user.getState().getToken().toString())
                        .build()));
    }
}
