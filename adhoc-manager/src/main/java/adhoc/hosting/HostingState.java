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

package adhoc.hosting;

import adhoc.task.ServerTask;
import lombok.Builder;
import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * State of a hosting service e.g. details about all the tasks running in our AWS ECS cluster.
 */
@Data
@Builder(toBuilder = true)
public class HostingState {

    private final Set<String> managerHosts;

    private final Set<String> kioskHosts;

    private final List<ServerTask> serverTasks;

    public HostingState() {
        this.managerHosts = Collections.emptySet();
        this.kioskHosts = Collections.emptySet();
        this.serverTasks = Collections.emptyList();
    }

    public HostingState(Set<String> managerHosts, Set<String> kioskHosts, List<ServerTask> serverTasks) {
        this.managerHosts = Collections.unmodifiableSet(managerHosts);
        this.kioskHosts = Collections.unmodifiableSet(kioskHosts);
        this.serverTasks = Collections.unmodifiableList(serverTasks);
    }

}
