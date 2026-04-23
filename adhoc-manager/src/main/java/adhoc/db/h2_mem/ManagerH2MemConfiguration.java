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

package adhoc.db.h2_mem;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.h2.tools.Server;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("db-h2-mem")
@Slf4j
@RequiredArgsConstructor
public class ManagerH2MemConfiguration {

    private final DataSourceProperties dataSourceProperties;

    @Bean(initMethod = "start", destroyMethod = "stop")
    Server h2MemServer() throws Exception {

        Server server = Server.createTcpServer(
                "-tcp", "-tcpAllowOthers", "-tcpPort", "9092");

        return server;
    }

    /** Make the JDBC connection details (will be provided to Spring) dependent on the DB server being started first. */
    @Bean
    public JdbcConnectionDetails dataSourceProperties(Server h2MemServer) {
        return new JdbcConnectionDetails() {

            @Override
            public String getJdbcUrl() {
                return dataSourceProperties.getUrl();
            }

            @Override
            public String getUsername() {
                return dataSourceProperties.getUsername();
            }

            @Override
            public String getPassword() {
                return dataSourceProperties.getPassword();
            }
        };
    }
}
