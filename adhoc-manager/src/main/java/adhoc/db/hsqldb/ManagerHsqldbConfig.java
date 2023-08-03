/*
 * Copyright (c) 2022-2023 SpeculativeCoder (https://github.com/SpeculativeCoder)
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

package adhoc.db.hsqldb;

import lombok.extern.slf4j.Slf4j;
import org.hsqldb.server.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.JdbcConnectionDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.IOException;
import java.nio.file.Files;

@Configuration
@Profile("db-hsqldb")
@Slf4j
public class ManagerHsqldbConfig {

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    static {
        System.setProperty("hsqldb.reconfig_logging", "false");
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    Server hsqldbServer() throws IOException {
        Server server = new Server();

        //server.setAddress("localhost"); //"0.0.0.0");
        server.setDatabaseName(0, "adhoc");
        server.setDatabasePath(0, "file:" + Files.createTempFile("adhoc_hsqldb_", ".dat").toString() +
                ";user=" + username + ";password=" + password +
                ";hsqldb.tx=locks" + // locks/mvlocks
                ";check_props=true" +
                ";sql.restrict_exec=true" +
                ";sql.enforce_names=true" +
                ";sql.enforce_types=true");
        server.setNoSystemExit(true);
        server.setSilent(true); // TODO
        //Properties hsqldbProperties = new Properties();
        //server.setProperties(hsqldbProperties);
        server.setPort(9001);

        return server;
    }

    @Bean
    public JdbcConnectionDetails dataSourceProperties(Server hsqldbServer) {
        return new JdbcConnectionDetails() {

            @Override
            public String getJdbcUrl() {
                return !url.isEmpty() ? url : "jdbc:hsqldb:hsql://localhost:9001/adhoc";
            }

            @Override
            public String getUsername() {
                return username;
            }

            @Override
            public String getPassword() {
                return password;
            }
        };
    }
}
