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

package adhoc;

import adhoc.user.UserRole;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.retry.annotation.EnableRetry;

import java.util.List;

/**
 * When running as a kiosk this application is for access by users (i.e. the "public" facing variant of the application).
 * There will likely be many kiosks running - enough to handle whatever load is occurring.
 * <p>
 * It receives events via the Artemis cluster to pass on to users.
 * <p>
 * Most of the access to the kiosk will be users with {@link UserRole#USER} role.
 */
@SpringBootApplication
//@EnableConfigurationProperties
//@EnableScheduling
//@EnableCaching
@EnableRetry
@Slf4j
@RequiredArgsConstructor
public class AdhocKioskApplication extends AbstractAdhocApplication {

    public static void main(String[] args) {
        // rather than rely on spring.profiles.default we will just pick some extra default profiles as needed
        ConfigurableEnvironment environment = new StandardEnvironment();
        List<String> activeProfiles = Lists.newArrayList(environment.getActiveProfiles());
        if (activeProfiles.stream().noneMatch(profile -> profile.startsWith("db-"))) {
            activeProfiles.add("db-h2postgres");
        }
        environment.setActiveProfiles(activeProfiles.toArray(new String[activeProfiles.size()]));

        SpringApplication application = new SpringApplication(AdhocKioskApplication.class);
        application.setEnvironment(environment);
        application.run(args);
    }
}
