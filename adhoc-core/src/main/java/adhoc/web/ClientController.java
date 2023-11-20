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

package adhoc.web;

import adhoc.core.properties.CoreProperties;
import com.google.common.base.Verify;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * Controller for serving up the UnrealEngine HTML5 client.
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class ClientController {

    private static final String MAP_NAME = "{mapName:[A-Za-z0-9_]{1,50}}";
    private static final String PROJECT_NAME = "{unrealProjectName:[A-Za-z0-9_]{1,50}}";
    private static final String VARIANT = "{variant:|-HTML5-Test|-HTML5-Shipping}";

    private final CoreProperties coreProperties;

    private String firstRegionMap;

    @PostConstruct
    private void postConstruct() {
        // TODO: nicer error handling
        firstRegionMap = coreProperties.getUnrealProjectRegionMaps().get(0);
    }

    // the Angular app sends user to e.g. /HTML5Client.html (for now) so try to give them Shipping/Test variant if available
    @GetMapping(value = "/HTML5Client/" + MAP_NAME + "/HTML5Client.html", produces = MimeTypeUtils.TEXT_HTML_VALUE)
    public Object getClientHtml(
            @PathVariable(value = "mapName") String mapName,
            HttpServletResponse response) {

        // TODO: would be better to have available variant set via property rather than checking for existence of each

        ClassPathResource resource = classPathResource(String.format("/HTML5Client/%s/HTML5/%s%s.html", mapName, coreProperties.getUnrealProjectName(), "-HTML5-Shipping"));
        if (resource.exists()) {
            return resource;
        }

        resource = classPathResource(String.format("/HTML5Client/%s/HTML5/%s%s.html", mapName, coreProperties.getUnrealProjectName(), "-HTML5-Test"));
        if (resource.exists()) {
            return resource;
        }

        resource = classPathResource(String.format("/HTML5Client/%s/HTML5/%s%s.html", mapName, coreProperties.getUnrealProjectName(), ""));
        if (resource.exists()) {
            return resource;
        }

        // TODO: nicer notification / instructions on how to fix
        return "<div style=\"text-align:center; color:black; background:white\">CLIENT NOT AVAILABLE</div>";
    }

    @GetMapping(value = "/HTML5Client/" + MAP_NAME + "/" + PROJECT_NAME + VARIANT + ".css", produces = "text/css")
    public ClassPathResource getClientCss(
            @PathVariable(value = "mapName") String mapName,
            @PathVariable(value = "variant") String variant,
            HttpServletResponse response) {

        ClassPathResource resource = classPathResource(String.format("/HTML5Client/%s/HTML5/%s%s.css.gz", mapName, coreProperties.getUnrealProjectName(), variant));
        response.setHeader("Content-Encoding", "gzip");

        return resource;
    }

    @GetMapping(value = "/HTML5Client/" + MAP_NAME + "/" + PROJECT_NAME + VARIANT + ".UE4.js", produces = "text/javascript")
    public ClassPathResource getClientUE4Js(@PathVariable(value = "mapName") String mapName,
                                            @PathVariable(value = "variant") String variant,
                                            HttpServletResponse response) {

        ClassPathResource resource = classPathResource(String.format("/HTML5Client/%s/HTML5/%s%s.UE4.js.gz", mapName, coreProperties.getUnrealProjectName(), variant));
        response.setHeader("Content-Encoding", "gzip");

        return resource;
    }

    @GetMapping(value = "/HTML5Client/" + MAP_NAME + "/" + PROJECT_NAME + VARIANT + ".data.js.gz", produces = "text/javascript")
    public ClassPathResource getClientDataJsGz(
            @PathVariable(value = "mapName") String mapName,
            @PathVariable(value = "variant") String variant,
            HttpServletResponse response) {

        ClassPathResource resource = classPathResource(String.format("/HTML5Client/%s/HTML5/%s%s.data.js.gz", mapName, coreProperties.getUnrealProjectName(), variant));
        response.setHeader("Content-Encoding", "gzip");

        return resource;
    }

    @GetMapping(value = "/HTML5Client/" + MAP_NAME + "/" + PROJECT_NAME + VARIANT + ".data.gz", produces = MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE)
    public ClassPathResource getClientDataGz(
            @PathVariable(value = "mapName") String mapName,
            @PathVariable(value = "variant") String variant,
            HttpServletResponse response) {

        ClassPathResource resource = classPathResource(String.format("/HTML5Client/%s/HTML5/%s%s.data.gz", mapName, coreProperties.getUnrealProjectName(), variant));
        response.setHeader("Content-Encoding", "gzip");

        return resource;
    }

    @GetMapping(value = "/HTML5Client/" + MAP_NAME + "/" + PROJECT_NAME + "Client" + VARIANT + ".js.gz", produces = "text/javascript")
    public ClassPathResource getClientClientJsGz(
            @PathVariable(value = "mapName") String mapName,
            @PathVariable(value = "variant") String variant,
            HttpServletResponse response) {

        ClassPathResource resource = classPathResource(String.format("/HTML5Client/%s/HTML5/%sClient%s.js.gz", mapName, coreProperties.getUnrealProjectName(), variant));
        response.setHeader("Content-Encoding", "gzip");

        return resource;
    }

    @GetMapping(value = "/HTML5Client/" + MAP_NAME + "/" + PROJECT_NAME + "Client" + VARIANT + ".js.symbols.gz", produces = "text/javascript")
    public ClassPathResource getClientClientJsSymbolsGz(
            @PathVariable(value = "mapName") String mapName,
            @PathVariable(value = "variant") String variant,
            HttpServletResponse response) {

        ClassPathResource resource = classPathResource(String.format("/HTML5Client/%s/HTML5/%sClient%s.js.symbols.gz", mapName, coreProperties.getUnrealProjectName(), variant));
        response.setHeader("Content-Encoding", "gzip");

        return resource;
    }

    @GetMapping(value = "/HTML5Client/" + MAP_NAME + "/" + PROJECT_NAME + "Client" + VARIANT + ".wasm.gz", produces = "application/wasm")
    public ClassPathResource getClientClientWasmGz(
            @PathVariable(value = "mapName") String mapName,
            @PathVariable(value = "variant") String variant,
            HttpServletResponse response) {

        ClassPathResource resource = classPathResource(String.format("/HTML5Client/%s/HTML5/%sClient%s.wasm.gz", mapName, coreProperties.getUnrealProjectName(), variant));
        response.setHeader("Content-Encoding", "gzip");

        return resource;
    }

    // worker js for multithreading builds only
    @GetMapping(value = "/HTML5Client/" + MAP_NAME + "/" + PROJECT_NAME + "Client" + VARIANT + ".worker.js.gz", produces = "text/javascript")
    public ClassPathResource getClientClientWorkerJsGz(
            @PathVariable(value = "mapName") String mapName,
            @PathVariable(value = "variant") String variant,
            HttpServletResponse response) {

        ClassPathResource resource = classPathResource(String.format("/HTML5Client/%s/HTML5/%sClient%s.worker.js.gz", mapName, coreProperties.getUnrealProjectName(), variant));
        response.setHeader("Content-Encoding", "gzip");

        return resource;
    }

    @GetMapping(value = "/HTML5Client/" + MAP_NAME + "/Utility.js.gz", produces = "text/javascript")
    public ClassPathResource getUtilityJsGz(
            @PathVariable(value = "mapName") String mapName,
            HttpServletResponse response) {

        ClassPathResource resource = classPathResource(String.format("/HTML5Client/%s/HTML5/Utility.js.gz", mapName));
        response.setHeader("Content-Encoding", "gzip");

        return resource;
    }

    @GetMapping(value = "/HTML5Client/" + MAP_NAME + "/jquery-2.1.3.min.js", produces = "text/javascript")
    public ClassPathResource getJQueryJs(
            @PathVariable(value = "mapName") String mapName,
            HttpServletResponse response) {

        ClassPathResource resource = classPathResource(String.format("/HTML5Client/%s/HTML5/jquery-2.1.3.min.js", mapName));

        return resource;
    }

    @GetMapping(value = "/HTML5Client/" + MAP_NAME + "/bootstrap.min.css", produces = "text/css")
    public ClassPathResource getBootstrapCss(
            @PathVariable(value = "mapName") String mapName,
            HttpServletResponse response) {

        ClassPathResource resource = classPathResource(String.format("/HTML5Client/%s/HTML5/bootstrap.min.css", mapName));

        return resource;
    }

    @GetMapping(value = "/HTML5Client/" + MAP_NAME + "/bootstrap.min.js", produces = "text/javascript")
    public ClassPathResource getBootstrapJs(
            @PathVariable(value = "mapName") String mapName,
            HttpServletResponse response) {

        ClassPathResource resource = classPathResource(String.format("/HTML5Client/%s/HTML5/bootstrap.min.js", mapName));

        return resource;
    }

    // the font access is from the root of the app - so just get the fonts from first mapName as they are all the same
    @GetMapping(value = "/HTML5Client/fonts/glyphicons-halflings-regular.ttf", produces = "font/ttf")
    public ClassPathResource getFontsGlyphiconsHalflingsRegularTtf(
            HttpServletResponse response) {

        ClassPathResource resource = classPathResource("/HTML5Client/" + firstRegionMap + "/HTML5/fonts/glyphicons-halflings-regular.ttf");

        return resource;
    }

    @GetMapping(value = "/HTML5Client/fonts/glyphicons-halflings-regular.woff", produces = "font/woff")
    public ClassPathResource getFontsGlyphiconsHalflingsRegularWoff(
            HttpServletResponse response) {

        ClassPathResource resource = classPathResource("/HTML5Client/" + firstRegionMap + "/HTML5/fonts/glyphicons-halflings-regular.woff");

        return resource;
    }

    @GetMapping(value = "/HTML5Client/fonts/glyphicons-halflings-regular.woff2", produces = "font/woff2")
    public ClassPathResource getFontsGlyphiconsHalflingsRegularWoff2(
            HttpServletResponse response) {

        ClassPathResource resource = classPathResource("/HTML5Client/" + firstRegionMap + "/HTML5/fonts/glyphicons-halflings-regular.woff2");

        return resource;
    }

    private ClassPathResource classPathResource(String path) {
        ClassPathResource resource = new ClassPathResource(path);
        // ensure nothing unexpected due to path normalization etc.
        Verify.verify(Objects.equals("/" + resource.getPath(), path));
        Verify.verify(resource.getPath().startsWith("HTML5Client/"));
        return resource;
    }
}
