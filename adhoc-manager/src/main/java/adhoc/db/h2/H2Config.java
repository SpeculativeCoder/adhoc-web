/*
 * Copyright (c) 2022-2024 SpeculativeCoder (https://github.com/SpeculativeCoder)
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

package adhoc.db.h2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.h2.tools.Server;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.jdbc.JdbcConnectionDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;

@Configuration
@Profile("db-h2")
@Slf4j
@RequiredArgsConstructor
public class H2Config {

    private final DataSourceProperties dataSourceProperties;
    private Path h2Dir;

    @Bean(initMethod = "start", destroyMethod = "stop")
    Server h2Server() throws SQLException, IOException {
        h2Dir = Files.createTempDirectory("adhoc_h2_");
        log.info("h2Dir={}", h2Dir);

        Server server = Server.createTcpServer(
                "-baseDir", h2Dir.toString(),
                //"-ifNotExists",
                "-tcp", "-tcpAllowOthers", "-tcpPort", "9092");

        return server;
    }

    @Bean
    public JdbcConnectionDetails dataSourceProperties(Server h2Server) {
        return new JdbcConnectionDetails() {

            @Override
            public String getJdbcUrl() {
                return !dataSourceProperties.getUrl().isEmpty() ?
                        // TODO
                        dataSourceProperties.getUrl() : "jdbc:h2:file:" + h2Dir.toString() + "/adhoc;MODE=strict;MV_STORE=true;DEFAULT_LOCK_TIMEOUT=5000";
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
