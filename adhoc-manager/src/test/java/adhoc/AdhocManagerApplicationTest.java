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

package adhoc;

import adhoc.user.UserEntity;
import adhoc.user.UserRepository;
import adhoc.user.register.UserRegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@Disabled // disabled for now
@SpringBootTest
@TestPropertySource("classpath:/application-test.properties")
@AutoConfigureMockMvc
@Slf4j
@Transactional
public class AdhocManagerApplicationTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    // TODO
    @Test
    public void testIndex() {
        assertThat(mvc.get().uri("/")).hasStatusOk().bodyText().contains("<app-root>");
    }

    // TODO
    @Test
    public void testRegister() throws Exception {
        UserRegisterRequest request = UserRegisterRequest.builder()
                .build();

        MvcTestResult result = mvc.post().uri("/adhoc_api/users/register")
                .contentType(MediaType.APPLICATION_JSON).with(csrf())
                .content(objectMapper.writeValueAsBytes(request))
                .exchange();

        assertThat(result)
                .hasStatus(HttpStatus.CREATED)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON);

        assertThat(result).cookies()
                .containsCookie("SESSION");

        List<UserEntity> users = userRepository.findAll(Sort.by(Sort.Order.desc("id")));
        UserEntity user = users.getFirst();

        assertThat(result).bodyJson().satisfies(json -> {
            //log.info(json.getJson());
            assertThat(json).extractingPath("$.id").convertTo(Long.class).isEqualTo(user.getId());
            assertThat(json).extractingPath("$.name").asString().isEqualTo(user.getName());
        });
    }
}
