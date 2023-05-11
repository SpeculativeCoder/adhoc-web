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

package adhoc.structure;

import adhoc.structure.event.StructureCreatedEvent;
import adhoc.structure.dto.StructureDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class StructureController {

    private final StructureService structureService;

    @GetMapping("/structures")
    public List<StructureDto> getStructures() {
        // TODO: sorting
        return structureService.getStructures();
    }

    @GetMapping("/structures/{structureId}")
    public StructureDto getStructure(@PathVariable("structureId") Long structureId) {
        return structureService.getStructure(structureId);
    }

    @PutMapping("/structures/{structureId}")
    @PreAuthorize("hasRole('ADMIN')")
    public StructureDto postStructure(@PathVariable("structureId") Long structureId, @Valid @RequestBody StructureDto structureDto) {
        structureDto.setId(structureId);

        return structureService.updateStructure(structureDto);
    }

    @PutMapping("/servers/{serverId}/structures")
    @PreAuthorize("hasRole('SERVER')")
    public List<StructureDto> putServerStructures(@PathVariable("serverId") Long serverId, @Valid @RequestBody List<StructureDto> structureDtos) {
        return structureService.updateServerStructures(serverId, structureDtos);
    }

    @MessageMapping("StructureCreated")
    @SendTo("/topic/events")
    @PreAuthorize("hasRole('SERVER')")
    public StructureCreatedEvent handledStructureCreated(@Valid @RequestBody StructureCreatedEvent structureCreatedEvent) {
        log.debug("Handling: {}", structureCreatedEvent);

        return structureService.processStructureCreated(structureCreatedEvent);
    }

}
