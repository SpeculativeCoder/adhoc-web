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

package adhoc.db.h2;

import adhoc.db.h2.properties.ManagerH2Properties;
import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.h2.tools.Server;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;

@Configuration
@Profile("db-h2")
@Slf4j
@RequiredArgsConstructor
public class ManagerH2Configuration {

    private final ManagerH2Properties managerH2Properties;

    private final DataSourceProperties dataSourceProperties;

    @Bean(initMethod = "start", destroyMethod = "stop")
    Server h2Server() throws SQLException, IOException {

        String h2Path = managerH2Properties.getH2Path();

        if (Strings.isNullOrEmpty(h2Path)) {
            try {
                h2Path = Files.createTempDirectory("adhoc_h2_").toString() + "/adhoc";
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        h2Path = Paths.get(h2Path).toAbsolutePath().normalize().toString();

        log.info("h2Path={}", h2Path);

        Server server = Server.createTcpServer(
                "-baseDir", h2Path,
                "-ifNotExists",
                "-tcp", "-tcpAllowOthers", "-tcpPort", "9092");

        return server;
    }

    /** Make the JDBC connection details (will be provided to Spring) dependent on the DB server being started first. */
    @Bean
    public JdbcConnectionDetails dataSourceProperties(Server h2Server) {
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
