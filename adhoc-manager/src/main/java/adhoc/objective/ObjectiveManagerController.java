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

package adhoc.objective;

import adhoc.objective.taken.ObjectiveTakenEvent;
import adhoc.objective.taken.ObjectiveTakenEventService;
import adhoc.objective.taken.ServerObjectiveTakenEvent;
import com.google.common.base.Preconditions;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class ObjectiveManagerController {

    private final ObjectiveManagerService objectiveManagerService;
    private final ObjectiveTakenEventService objectiveTakenEventService;

    @PutMapping("/objectives/{objectiveId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ObjectiveDto putObjective(
            @PathVariable("objectiveId") Long objectiveId,
            @Valid @RequestBody ObjectiveDto objectiveDto) {

        Preconditions.checkArgument(Objects.equals(objectiveId, objectiveDto.getId()),
                "Objective ID mismatch: %s != %s", objectiveId, objectiveDto.getId());

        return objectiveManagerService.updateObjective(objectiveDto);
    }

    @PostMapping("/servers/{serverId}/objectives")
    @PreAuthorize("hasRole('SERVER')")
    public List<ObjectiveDto> postServerObjectives(
            @PathVariable Long serverId,
            @Valid @RequestBody List<ObjectiveDto> objectiveDtos) {

        return objectiveManagerService.updateServerObjectives(serverId, objectiveDtos);
    }

    @MessageMapping("ObjectiveTaken")
    @SendTo("/topic/events")
    @PreAuthorize("hasRole('SERVER') or hasRole('ADMIN')")
    public ObjectiveTakenEvent handleObjectiveTake(
            @Valid @RequestBody ServerObjectiveTakenEvent event) {

        log.debug("Handling: {}", event);

        return objectiveTakenEventService.handleObjectiveTaken(event);
    }

}
