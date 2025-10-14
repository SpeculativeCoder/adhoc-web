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

package adhoc.task;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Information about a task in the hosting service e.g. a task in an AWS ECS cluster.
 * There will be at least one task for the manager, one or more tasks for the kiosk (to handle load),
 * and then many server tasks to run the Unreal servers (which will be spun up and spun down as needed by population).
 */
@Entity(name = "Task")
//@DynamicInsert
//@DynamicUpdate
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@NoArgsConstructor
@Getter
@Setter
@ToString
public abstract class TaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "TaskIdSequence")
    @SequenceGenerator(name = "TaskIdSequence", initialValue = 1, allocationSize = 50)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    /** Identifier of the task within the hosting service (e.g. task ARN of AWS ECS task). */
    @Column(nullable = false, unique = true)
    private String taskIdentifier;

    /** IP that is reachable within the hosting service but not externally. */
    private String privateIp;

    /** Public IP visible to users. */
    private String publicIp;

    /** Web socket port visible to users (for server tasks this is typically 8898) */
    private Integer publicWebSocketPort;

    /** DNS domain name that is mapped to the public IP. */
    private String domain;

    private LocalDateTime initiated;

    private LocalDateTime seen;

    public abstract Type getType();

    @RequiredArgsConstructor
    public enum Type {
        MANAGER("Manager"),
        KIOSK("Kiosk"),
        SERVER("Server");

        @Getter
        private final String text;
    }
}
