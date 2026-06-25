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
import adhoc.faction.FactionRepository;
import adhoc.region.RegionEntity;
import adhoc.region.RegionRepository;
import adhoc.server.ServerEntity;
import adhoc.server.ServerRepository;
import adhoc.system.auth.AdhocUserDetails;
import adhoc.user.UserEntity;
import adhoc.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

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

    @Test
    public void testNavigate() throws Exception {
        LocalDateTime startTime = LocalDateTime.now();

        RegionEntity region = regionRepository.findById(1L).orElseThrow();
        AreaEntity area = areaRepository.findById(1L).orElseThrow();

        UserEntity user = new UserEntity("TestUser", "USER");
        user.setFaction(factionRepository.findById(1L).orElseThrow());
        user = userRepository.save(user);
        UUID originalToken = user.getState().getToken();

        ServerEntity server = new ServerEntity(region, List.of(area));
        server.setEnabled(true);
        server.setActive(true);
        server.setPublicIp("127.0.0.1");
        server.setWebSocketUrl("wss://server.localhost:8889");
        server.setPublicWebSocketPort(8889);
        server = serverRepository.save(server);

        MvcTestResult result = mvc.post().uri("/adhoc_api/users/navigate")
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf())
                .with(user(new AdhocUserDetails(user.getName(), user.getPassword(), true, user.getAuthorities(), user.getId())))
                .content("""
                        {
                            "destinationServerId": %d
                        }
                        """.formatted(
                        server.getId()))
                .exchange();

        user = userRepository.findById(user.getId()).orElseThrow();

        assertThat(user.getState().getDestinationServer().getId()).isEqualTo(server.getId());
        assertThat(user.getState().getNavigated()).isAfterOrEqualTo(startTime);
        assertThat(user.getState().getUpdated()).isAfterOrEqualTo(startTime);
        assertThat(user.getState().getToken()).isNotNull().isNotEqualTo(originalToken);

        assertThat(result)
                .hasStatusOk()
                .hasContentType(MediaType.APPLICATION_JSON);

        assertThat(result).bodyJson().isEqualTo("""
                {
                    "ip": "127.0.0.1",
                    "port": 8889,
                    "webSocketUrl": "wss://server.localhost:8889",
                    "mapName": "Region0001",
                    "userId": %d,
                    "factionId": 1,
                    "token": "%s",
                    "x": null,
                    "y": null,
                    "z": null,
                    "yaw": null,
                    "pitch": null
                }
                """.formatted(user.getId(), user.getState().getToken()));
    }
}
