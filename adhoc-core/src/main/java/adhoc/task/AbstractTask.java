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

package adhoc.task;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * TODO: WIP
 * Information about a task in the hosting service e.g. a task in an AWS ECS cluster.
 * There will be at least one task for the manager, one or more tasks for the kiosk (to handle load),
 * and then many server tasks to run the Unreal servers (which will be spun up and spun down as needed by population).
 */
@Entity(name = "Task")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@ToString
public abstract class AbstractTask {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "TaskIdSequence")
    @SequenceGenerator(name = "TaskIdSequence", initialValue = 1, allocationSize = 50)
    @ToString.Include
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskType type;

    /** Identifier of the task within the hosting service (e.g. task ARN of AWS ECS task). */
    @Column(nullable = false)
    private String identifier;

    /** IP that is reachable within the hosting service but not externally. */
    @Column(nullable = false)
    private String privateIp;

    /** Public IP visible to users. */
    @Column(nullable = false)
    private String publicIp;

    /** Web socket port visible to users (for server tasks this is typically 8898) */
    @Column(nullable = false)
    private Integer publicWebSocketPort;
}
