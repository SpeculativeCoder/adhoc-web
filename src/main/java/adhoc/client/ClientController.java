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

package adhoc.client;

import adhoc.AdhocProperties;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for serving up the UnrealEngine HTML5 client.
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class ClientController {

    private static final String MAP_NAME = "{mapName:Region[0-9]{4}}";
    private static final String PROJECT_NAME = "{unrealProjectName:[A-Za-z0-9]+}";
    private static final String VARIANT = "{variant:|-HTML5-Test|-HTML5-Shipping}";

    private final AdhocProperties adhocProperties;

    // the Angular app sends user to e.g. /Client.html (for now) so try to give them Shipping/Test variant if available
    @GetMapping(value = "/" + MAP_NAME + "/Client.html", produces = MimeTypeUtils.TEXT_HTML_VALUE)
    public Object getClientHtml(@PathVariable(value = "mapName") String mapName, HttpServletResponse response) {

        // TODO: would be better to have available variant set via property rather than checking for existence of each

        ClassPathResource resource = new ClassPathResource(String.format("/Client/%s/HTML5/%s%s.html", mapName, adhocProperties.getUnrealProjectName(), "-HTML5-Shipping"));
        if (resource.exists()) {
            return resource;
        }

        resource = new ClassPathResource(String.format("/Client/%s/HTML5/%s%s.html", mapName, adhocProperties.getUnrealProjectName(), "-HTML5-Test"));
        if (resource.exists()) {
            return resource;
        }

        resource = new ClassPathResource(String.format("/Client/%s/HTML5/%s%s.html", mapName, adhocProperties.getUnrealProjectName(), ""));
        if (resource.exists()) {
            return resource;
        }

        // TODO: nicer notification / instructions on how to fix
        return "<div style=\"text-align:center; color:black; background:white\">CLIENT NOT AVAILABLE</div>";
    }

    @GetMapping(value = "/" + MAP_NAME + "/" + PROJECT_NAME + VARIANT + ".css", produces = "text/css")
    public ClassPathResource getClientCss(@PathVariable(value = "mapName") String mapName, @PathVariable(value = "variant") String variant, HttpServletResponse response) {
        ClassPathResource resource = new ClassPathResource(String.format("/Client/%s/HTML5/%s%s.css.gz", mapName, adhocProperties.getUnrealProjectName(), variant));
        response.setHeader("Content-Encoding", "gzip");
        return resource;
    }

    @GetMapping(value = "/" + MAP_NAME + "/" + PROJECT_NAME + VARIANT + ".UE4.js", produces = "application/javascript")
    public ClassPathResource getClientUE4Js(@PathVariable(value = "mapName") String mapName, @PathVariable(value = "variant") String variant, HttpServletResponse response) {
        ClassPathResource resource = new ClassPathResource(String.format("/Client/%s/HTML5/%s%s.UE4.js.gz", mapName, adhocProperties.getUnrealProjectName(), variant));
        response.setHeader("Content-Encoding", "gzip");
        return resource;
    }

    @GetMapping(value = "/" + MAP_NAME + "/" + PROJECT_NAME + VARIANT + ".data.js.gz", produces = "application/javascript")
    public ClassPathResource getClientDataJsGz(@PathVariable(value = "mapName") String mapName, @PathVariable(value = "variant") String variant, HttpServletResponse response) {
        ClassPathResource resource = new ClassPathResource(String.format("/Client/%s/HTML5/%s%s.data.js.gz", mapName, adhocProperties.getUnrealProjectName(), variant));
        response.setHeader("Content-Encoding", "gzip");
        return resource;
    }

    @GetMapping(value = "/" + MAP_NAME + "/" + PROJECT_NAME + VARIANT + ".data.gz", produces = MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE)
    public ClassPathResource getClientDataGz(@PathVariable(value = "mapName") String mapName, @PathVariable(value = "variant") String variant, HttpServletResponse response) {
        ClassPathResource resource = new ClassPathResource(String.format("/Client/%s/HTML5/%s%s.data.gz", mapName, adhocProperties.getUnrealProjectName(), variant));
        response.setHeader("Content-Encoding", "gzip");
        return resource;
    }

    @GetMapping(value = "/" + MAP_NAME + "/" + PROJECT_NAME + "Client" + VARIANT + ".js.gz", produces = "application/javascript")
    public ClassPathResource getClientClientJsGz(@PathVariable(value = "mapName") String mapName, @PathVariable(value = "variant") String variant, HttpServletResponse response) {
        ClassPathResource resource = new ClassPathResource(String.format("/Client/%s/HTML5/%sClient%s.js.gz", mapName, adhocProperties.getUnrealProjectName(), variant));
        response.setHeader("Content-Encoding", "gzip");
        return resource;
    }

    @GetMapping(value = "/" + MAP_NAME + "/" + PROJECT_NAME + "Client" + VARIANT + ".js.symbols.gz", produces = "application/javascript")
    public ClassPathResource getClientClientJsSymbolsGz(@PathVariable(value = "mapName") String mapName, @PathVariable(value = "variant") String variant, HttpServletResponse response) {
        ClassPathResource resource = new ClassPathResource(String.format("/Client/%s/HTML5/%sClient%s.js.symbols.gz", mapName, adhocProperties.getUnrealProjectName(), variant));
        response.setHeader("Content-Encoding", "gzip");
        return resource;
    }

    @GetMapping(value = "/" + MAP_NAME + "/" + PROJECT_NAME + "Client" + VARIANT + ".wasm.gz", produces = "application/wasm")
    public ClassPathResource getClientClientWasmGz(@PathVariable(value = "mapName") String mapName, @PathVariable(value = "variant") String variant, HttpServletResponse response) {
        ClassPathResource resource = new ClassPathResource(String.format("/Client/%s/HTML5/%sClient%s.wasm.gz", mapName, adhocProperties.getUnrealProjectName(), variant));
        response.setHeader("Content-Encoding", "gzip");
        return resource;
    }

    // worker js for multithreading builds only
    @GetMapping(value = "/" + MAP_NAME + "/" + PROJECT_NAME + "Client" + VARIANT + ".worker.js.gz", produces = "application/javascript")
    public ClassPathResource getClientClientWorkerJsGz(@PathVariable(value = "mapName") String mapName, @PathVariable(value = "variant") String variant, HttpServletResponse response) {
        ClassPathResource resource = new ClassPathResource(String.format("/Client/%s/HTML5/%sClient%s.worker.js.gz", mapName, adhocProperties.getUnrealProjectName(), variant));
        response.setHeader("Content-Encoding", "gzip");
        return resource;
    }

    @GetMapping(value = "/" + MAP_NAME + "/Utility.js.gz", produces = "application/javascript")
    public ClassPathResource getUtilityJsGz(@PathVariable(value = "mapName") String mapName, HttpServletResponse response) {
        ClassPathResource resource = new ClassPathResource(String.format("/Client/%s/HTML5/Utility.js.gz", mapName));
        response.setHeader("Content-Encoding", "gzip");
        return resource;
    }

    @GetMapping(value = "/" + MAP_NAME + "/jquery-2.1.3.min.js", produces = "application/javascript")
    public ClassPathResource getJQueryJs(@PathVariable(value = "mapName") String mapName, HttpServletResponse response) {
        ClassPathResource resource = new ClassPathResource(String.format("/Client/%s/HTML5/jquery-2.1.3.min.js", mapName));
        return resource;
    }

    @GetMapping(value = "/" + MAP_NAME + "/bootstrap.min.css", produces = "text/css")
    public ClassPathResource getBootstrapCss(@PathVariable(value = "mapName") String mapName, HttpServletResponse response) {
        ClassPathResource resource = new ClassPathResource(String.format("/Client/%s/HTML5/bootstrap.min.css", mapName));
        return resource;
    }

    @GetMapping(value = "/" + MAP_NAME + "/bootstrap.min.js", produces = "application/javascript")
    public ClassPathResource getBootstrapJs(@PathVariable(value = "mapName") String mapName, HttpServletResponse response) {
        ClassPathResource resource = new ClassPathResource(String.format("/Client/%s/HTML5/bootstrap.min.js", mapName));
        return resource;
    }

    // the font access is from the root of the app - so just get the fonts from first mapName as they are all the same
    @GetMapping(value = "/fonts/glyphicons-halflings-regular.ttf")
    public ClassPathResource getFontsGlyphiconsHalflingsRegularTtf(HttpServletResponse response) {
        ClassPathResource resource = new ClassPathResource("/Client/Region0001/HTML5/fonts/glyphicons-halflings-regular.ttf");
        return resource;
    }

    @GetMapping(value = "/fonts/glyphicons-halflings-regular.woff")
    public ClassPathResource getFontsGlyphiconsHalflingsRegularWoff(HttpServletResponse response) {
        ClassPathResource resource = new ClassPathResource("/Client/Region0001/HTML5/fonts/glyphicons-halflings-regular.woff");
        return resource;
    }

    @GetMapping(value = "/fonts/glyphicons-halflings-regular.woff2")
    public ClassPathResource getFontsGlyphiconsHalflingsRegularWoff2(HttpServletResponse response) {
        ClassPathResource resource = new ClassPathResource("/Client/Region0001/HTML5/fonts/glyphicons-halflings-regular.woff2");
        return resource;
    }


}
