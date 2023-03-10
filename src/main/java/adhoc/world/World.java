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

package adhoc.world;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

/**
 * Any details about the world e.g. settings can go here.
 * Currently, we can only have one world per database.
 */
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true, includeFieldNames = false)
public class World {

    @Id
    @ToString.Include
    private Long id;

    @Version
    @Basic(optional = false)
    private Long version;

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> managerHosts;

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> kioskHosts;
}

//    @Embeddable
//    @Getter
//    @Setter
//    @ToString(onlyExplicitlyIncluded = true, includeFieldNames = false)
//    private static class WebServer {
//        @ToString.Include
//        private String privateIp;
//        @ToString.Include
//        private String privatePort;
//        @ToString.Include
//        private Long messageBrokerCorePort;
//    }
