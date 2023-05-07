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

package adhoc;

import adhoc.artemis.ArtemisConfig;
import adhoc.user.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties
@EnableScheduling
@EnableCaching
@Slf4j
@RequiredArgsConstructor
public class AdhocApplication {

    public enum Mode {
        /**
         * When running as a {@link #MANAGER}, this application talks to a {@link adhoc.hosting.HostingService} to ensure servers are representing each area in each region (and will start / stop servers accordingly).
         * There will likely only be a few (and typically just 1) {@link #MANAGER} applications running.
         * <p>
         * Servers communicate with the {@link #MANAGER} to let it know about events occurring in the world.
         * Events are handled by the {@link #MANAGER} and then emitted in the {@link ArtemisConfig} cluster for {@link #KIOSK}'s to observe.
         * <p>
         * Typically, only {@link UserRole#SERVER} and {@link UserRole#ADMIN} users access the manager.
         */
        MANAGER,

        /**
         * When running as a {@link #KIOSK} this application is for access by users (i.e. the "public" facing variant of the application).
         * There will likely be many {@link #KIOSK}'s running - enough to handle whatever load is occurring.
         * <p>
         * It receives events via the Artemis cluster to pass on to users.
         * <p>
         * Most of the access to the kiosk will be users with {@link UserRole#USER} role.
         */
        KIOSK
    }

    public static void main(String[] args) {
        SpringApplication.run(AdhocApplication.class, args); //.start();
    }
}
