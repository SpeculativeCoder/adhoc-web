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

package adhoc;

import adhoc.system.artemis.AdhocArtemisConfiguration;
import adhoc.user.UserRole;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;

import java.util.List;

/**
 * When running as a Manager, this application manages the Unreal servers.
 * There will likely only be a few (and typically just 1) Manager application(s) running.
 * <p>
 * Unreal servers communicate with the Manager to let it know about events occurring.
 * Events are handled by the Manager and then emitted in the Artemis cluster (see {@link AdhocArtemisConfiguration})
 * for Kiosks to observe.
 * <p>
 * The Manager also runs all the same web-facing functionality of the Kiosk, particularly for administrator access.
 * Typically, only {@link UserRole#SERVER} and {@link UserRole#ADMIN} users access the Manager.
 */
@SpringBootApplication
@Slf4j
@RequiredArgsConstructor
public class AdhocManagerApplication extends AbstractAdhocApplication {

    /**
     * Recommended combinations of Spring profiles (defaults will be chosen if you don't specify):
     * <ul>
     * <li><tt>db-h2,hosting-local,dns-local</tt> - used for local testing. This is the default if you don't specify any profiles. It will assume you are running an Unreal server on the localhost manually via the editor.</li>
     * <li><tt>db-h2,hosting-docker,dns-local</tt> - used for local testing. It will use a local Docker to run the Unreal servers.</li>
     * <li><tt>db-h2,hosting-ecs,dns-route53</tt> - this is what runs in AWS. Makes use of ECS to run unreal servers, and Route53 to manage DNS entries</li>
     * </ul>
     */
    public static void main(String[] args) {
        // rather than rely on spring.profiles.default we will just pick default profiles as needed
        ConfigurableEnvironment environment = new StandardEnvironment();
        List<String> activeProfiles = Lists.newArrayList(environment.getActiveProfiles());

        if (activeProfiles.stream().noneMatch(profile -> profile.startsWith("db-"))) {
            activeProfiles.add("db-h2");
        }
        if (activeProfiles.stream().noneMatch(profile -> profile.startsWith("hosting-"))) {
            activeProfiles.add("hosting-local");
        }
        if (activeProfiles.stream().noneMatch(profile -> profile.startsWith("dns-"))) {
            activeProfiles.add("dns-local");
        }

        environment.setActiveProfiles(activeProfiles.toArray(new String[0]));

        SpringApplication application = new SpringApplication(AdhocManagerApplication.class);
        application.setEnvironment(environment);
        application.run(args);

        //SpringApplication.run(AdhocManagerApplication.class, args);
    }
}
