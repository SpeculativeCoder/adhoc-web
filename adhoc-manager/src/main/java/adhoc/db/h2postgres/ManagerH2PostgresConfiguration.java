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

package adhoc.db.h2postgres;

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
@Profile("db-h2postgres")
@Slf4j
@RequiredArgsConstructor
public class ManagerH2PostgresConfiguration {

    private final DataSourceProperties dataSourceProperties;
    private Path h2PostgresDir;

    @Bean(initMethod = "start", destroyMethod = "stop")
    Server h2PostgresServer() throws SQLException, IOException {
        h2PostgresDir = Files.createTempDirectory("adhoc_h2postgres_");
        log.info("h2PostgresDir={}", h2PostgresDir);

        Server server = Server.createTcpServer(
                "-baseDir", h2PostgresDir.toString(),
                //"-ifNotExists",
                "-tcp", "-tcpAllowOthers", "-tcpPort", "9092");

        return server;
    }

    @Bean
    public JdbcConnectionDetails dataSourceProperties(Server h2PostgresServer) {
        return new JdbcConnectionDetails() {

            @Override
            public String getJdbcUrl() {
                return !dataSourceProperties.getUrl().isEmpty() ?
                        // TODO
                        dataSourceProperties.getUrl() : "jdbc:h2:file:" + h2PostgresDir.toString() + "/adhoc;MODE=PostgreSQL;DATABASE_TO_LOWER=true;DEFAULT_NULL_ORDERING=HIGH;MV_STORE=true;DEFAULT_LOCK_TIMEOUT=10000;LOCK_TIMEOUT=10000;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false";
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
