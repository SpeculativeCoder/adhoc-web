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

import adhoc.system.artemis.AdhocArtemisConfiguration;
import adhoc.user.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * When running as a manager, this application talks to a {@link adhoc.hosting.HostingService}
 * to ensure servers are representing each area in each region (and will start / stop servers accordingly).
 * There will likely only be a few (and typically just 1) manager application(s) running.
 * <p>
 * Servers communicate with the manager to let it know about events occurring in the world.
 * Events are handled by the manager and then emitted in the {@link AdhocArtemisConfiguration} cluster for kiosks to observe.
 * <p>
 * Typically, only {@link UserRole#SERVER} and {@link UserRole#ADMIN} users access the manager.
 */
@SpringBootApplication
@Slf4j
@RequiredArgsConstructor
public class AdhocManagerApplication extends AbstractAdhocApplication {

    /**
     * Some valid combinations of Spring profiles for the manager are:
     * <ul>
     * <li><tt>db-h2postgres,hosting-local,dns-local</tt> - this is the default, used for local testing where you can also run the Unreal server in the editor</li>
     * <li><tt>db-h2postgres,hosting-docker,dns-local</tt> - this will use Docker to run the Unreal servers for local testing</li>
     * <li><tt>db-h2,hosting-docker,dns-local</tt> - as above but uses H2 database without postgres dialect</li>
     * <li><tt>db-hsqldb,hosting-docker,dns-local</tt> - as above but uses HSQLDB database</li>
     * <li><tt>db-postgres,hosting-docker,dns-local</tt> - as above but uses a real persistent Postgres database on the local machine (good for testing DB changelog)</li>
     * <li><tt>db-h2postgres,hosting-ecs,dns-route53</tt> - this is what runs in AWS and makes use of ECS to run unreal servers, and Route53 to manage DNS entries</li>
     * </ul>
     */
    public static void main(String[] args) {
        // rather than rely on spring.profiles.default we will just pick default profiles as needed
        //ConfigurableEnvironment environment = new StandardEnvironment();
        //List<String> activeProfiles = Lists.newArrayList(environment.getActiveProfiles());
        //
        //if (activeProfiles.stream().noneMatch(profile -> profile.startsWith("db-"))) {
        //    activeProfiles.add("db-h2");
        //}
        //if (activeProfiles.stream().noneMatch(profile -> profile.startsWith("hosting-"))) {
        //    activeProfiles.add("hosting-local");
        //}
        //if (activeProfiles.stream().noneMatch(profile -> profile.startsWith("dns-"))) {
        //    activeProfiles.add("dns-local");
        //}
        //
        //environment.setActiveProfiles(activeProfiles.toArray(new String[0]));
        //
        //SpringApplication application = new SpringApplication(AdhocManagerApplication.class);
        //application.setEnvironment(environment);
        //application.run(args);

        SpringApplication.run(AdhocManagerApplication.class, args);
    }
}
