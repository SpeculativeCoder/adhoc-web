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

package adhoc.user;

import adhoc.region.Region;
import adhoc.region.RegionRepository;
import adhoc.server.Server;
import adhoc.server.ServerRepository;
import adhoc.user.request_response.UserNavigateRequest;
import com.google.common.base.Preconditions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ThreadLocalRandom;

@Transactional
@Service
@Slf4j
@RequiredArgsConstructor
public class UserNavigateService {

    private final UserRepository userRepository;
    private final ServerRepository serverRepository;
    private final RegionRepository regionRepository;

    private final UserService userService;

    public UserDetailDto navigateUser(Long userId, UserNavigateRequest userNavigateRequest) {
        Preconditions.checkArgument(userNavigateRequest.getRegionId() != null,
                "User navigation must specify a region");

        User user = userRepository.getReferenceById(userId);

        Region oldRegion = user.getRegion();
        Region region;

        if (userNavigateRequest.getServerId() != null) {
            Server server = serverRepository.getReferenceById(userNavigateRequest.getServerId());
            region = server.getRegion();
            user.setRegion(region);
            user.setServer(server);
        } else {
            region = regionRepository.getReferenceById(userNavigateRequest.getRegionId());
            user.setRegion(region);
            // TODO
            user.setServer(region.getServers().get(ThreadLocalRandom.current().nextInt(region.getServers().size())));
        }

        // changing region nukes last location
        if (region != oldRegion) {
            user.setX(null);
            user.setY(null);
            user.setZ(null);
            user.setPitch(null);
            user.setYaw(null);
        }

        return userService.toDetailDto(user);
    }
}
