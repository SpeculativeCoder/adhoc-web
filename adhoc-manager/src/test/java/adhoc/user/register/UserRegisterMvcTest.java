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
import adhoc.user.current.CurrentUserDto;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

public class UserRegisterMvcTest extends AbstractManagerMvcTest {

    @Autowired
    private CoreProperties coreProperties;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private Clock clock;

    @Test
    public void testRegister() throws Exception {

        // ARRANGE

        long priorUserCount = userRepository.count();

        // ACT

        UserRegisterRequest request = UserRegisterRequest.builder()
                .human(true)
                .build();
        MvcTestResult result = mvc.post().uri("/adhoc_api/users/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .exchange();

        // ASSERT

        LocalDateTime now = LocalDateTime.now(clock);

        long userCount = userRepository.count();
        assertThat(userCount).isEqualTo(priorUserCount + 1);

        UserEntity user = entityManager.createQuery(
                        "SELECT u FROM User u ORDER BY u.id DESC LIMIT 1", UserEntity.class)
                .getSingleResult();
        assertThat(user.getVersion()).isEqualTo(2); // NOTE: programmatic login sets last login time
        assertThat(user.getName()).isAlphanumeric();
        assertThat(user.getEmail()).isNull();
        assertThat(user.getPassword()).isNull();
        assertThat(user.getQuickLoginPassword(coreProperties.getQuickLoginPasswordEncryptionKey())).isAlphanumeric();
        assertThat(user.isHuman()).isTrue();
        assertThat(user.getScore()).isEqualTo(BigDecimal.ZERO);
        assertThat(user.getUserRoles()).isEqualTo(Set.of(UserRole.USER));
        assertThat(user.getCreated()).isCloseTo(now, within(1, ChronoUnit.MICROS));
        assertThat(user.getUpdated()).isCloseTo(now, within(1, ChronoUnit.MICROS));

        assertThat(result)
                .hasStatus(HttpStatus.CREATED)
                .hasHeader("Location", "/adhoc_api/users/current")
                .hasContentType(MediaType.APPLICATION_JSON)
                .bodyJson().isEqualTo(objectMapper.writeValueAsString(CurrentUserDto.builder()
                        .id(user.getId()).version(1L) // NOTE: programmatic login sets last login time
                        .name(user.getName())
                        .quickLoginCode(user.getName() + "-" + user.getQuickLoginPassword(coreProperties.getQuickLoginPasswordEncryptionKey()))
                        .human(true)
                        .factionId(user.getFaction().getId())
                        .score(BigDecimal.ZERO)
                        .roles(List.of("USER"))
                        .build()));
        assertThat(result).cookies()
                .containsCookie("SESSION");
    }
}
