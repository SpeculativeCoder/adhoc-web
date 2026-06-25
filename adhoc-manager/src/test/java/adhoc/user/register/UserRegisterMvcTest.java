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

package adhoc.user.register;

import adhoc.AbstractManagerMvcTest;
import adhoc.system.properties.CoreProperties;
import adhoc.user.UserEntity;
import adhoc.user.UserRepository;
import adhoc.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.domain.Sort.Order.desc;
import static org.springframework.data.domain.Sort.by;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

public class UserRegisterMvcTest extends AbstractManagerMvcTest {

    @Autowired
    private CoreProperties coreProperties;
    @Autowired
    private UserRepository userRepository;

    @Test
    public void testRegister() throws Exception {
        long numUsersBefore = userRepository.count();

        MvcTestResult result = mvc.post().uri("/adhoc_api/users/register")
                .contentType(MediaType.APPLICATION_JSON).with(csrf())
                //.content(jsonMapper.writeValueAsBytes(request))
                .content("""
                        {
                            "human": true
                        }
                        """)
                .exchange();

        long numUsersAfter = userRepository.count();
        assertThat(numUsersAfter).isEqualTo(numUsersBefore + 1);

        List<UserEntity> users = userRepository.findAll(by(desc("id")));
        UserEntity user = users.getFirst();

        assertThat(user.getId()).isEqualTo(numUsersAfter);
        assertThat(user.getName()).isAlphanumeric();
        assertThat(user.getEmail()).isNull();
        assertThat(user.getPassword()).isNull();
        assertThat(user.getQuickLoginPassword(coreProperties.getQuickLoginPasswordEncryptionKey())).isAlphanumeric();
        assertThat(user.isHuman()).isTrue();
        assertThat(user.getScore()).isEqualTo(BigDecimal.ZERO);
        assertThat(user.getUserRoles()).isEqualTo(Set.of(UserRole.USER));
        // TODO

        assertThat(result)
                .hasStatus(HttpStatus.CREATED)
                .hasContentType(MediaType.APPLICATION_JSON) // TODO
                .hasHeader("Location", "/adhoc_api/users/current");

        assertThat(result).cookies()
                .containsCookie("SESSION");

        assertThat(result).bodyJson().isEqualTo("""
                {
                    "id": %d,
                    "version": 1,
                    "name": "%s",
                    "quickLoginCode": "%s",
                    "human": true,
                    "factionId": %d,
                    "score": 0.0,
                    "regionId": null,
                    "roles": ["USER"],
                    "destinationServerId": null,
                    "serverId": null
                }
                """.formatted(
                user.getId(),
                user.getName(),
                user.getName() + "-" + user.getQuickLoginPassword(coreProperties.getQuickLoginPasswordEncryptionKey()),
                user.getFaction().getId()));
    }
}
