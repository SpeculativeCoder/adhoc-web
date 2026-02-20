/*
 * Copyright (c) 2022-2026 SpeculativeCoder (https://github.com/SpeculativeCoder)
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

import adhoc.shared.properties.CoreProperties;
import com.google.common.base.Verify;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
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
    private static final String DATA_FLAVOR = "{dataFlavor:|.astc|.etc2}";

    private final CoreProperties coreProperties;

    //private String firstRegionMap;

    @PostConstruct
    private void postConstruct() {
        // TODO: nicer error handling
        //firstRegionMap = coreProperties.getUnrealProjectRegionMaps().get(0);
    }

    // the Angular app sends user to e.g. /HTML5Client.html (for now) so try to give them Shipping/Test variant if available
    @GetMapping(value = "/HTML5Client/" + MAP_NAME + "/HTML5Client.html", produces = MimeTypeUtils.TEXT_HTML_VALUE)
    public ResponseEntity<?> getClientHtml(
            @PathVariable String mapName,
            HttpServletResponse response) {

        // TODO: would be better to have available variant set via property rather than checking for existence of each

        ClassPathResource resource = classPathResource(String.format("/HTML5Client/%s/HTML5/%s%s.html", mapName, coreProperties.getUnrealProjectName(), "-HTML5-Shipping"));
        if (resource.exists()) {
            return ResponseEntity.ok(resource);
        }

        resource = classPathResource(String.format("/HTML5Client/%s/HTML5/%s%s.html", mapName, coreProperties.getUnrealProjectName(), "-HTML5-Test"));
        if (resource.exists()) {
            return ResponseEntity.ok(resource);
        }

        resource = classPathResource(String.format("/HTML5Client/%s/HTML5/%s%s.html", mapName, coreProperties.getUnrealProjectName(), ""));
        if (resource.exists()) {
            return ResponseEntity.ok(resource);
        }

        // TODO: nicer notification / instructions on how to fix
        return ResponseEntity.ok("<div style=\"text-align:center; color:black; background:white\">CLIENT NOT AVAILABLE</div>");
    }

    @GetMapping(value = "/HTML5Client/" + MAP_NAME + "/" + PROJECT_NAME + VARIANT + ".css", produces = "text/css")
    public ResponseEntity<ClassPathResource> getClientCss(
            @PathVariable String mapName,
            @PathVariable String variant,
            HttpServletResponse response) {

        ClassPathResource resource = classPathResource(String.format("/HTML5Client/%s/HTML5/%s%s.css.gz", mapName, coreProperties.getUnrealProjectName(), variant));

        return ResponseEntity.ok()
                .header("Content-Encoding", "gzip")
                .body(resource);
    }

    @GetMapping(value = "/HTML5Client/" + MAP_NAME + "/" + PROJECT_NAME + VARIANT + ".UE4.js", produces = "text/javascript")
    public ResponseEntity<ClassPathResource> getClientUE4Js(
            @PathVariable String mapName,
            @PathVariable String variant,
            HttpServletResponse response) {

        ClassPathResource resource = classPathResource(String.format("/HTML5Client/%s/HTML5/%s%s.UE4.js.gz", mapName, coreProperties.getUnrealProjectName(), variant));

        return ResponseEntity.ok()
                .header("Content-Encoding", "gzip")
                .body(resource);
    }

    @GetMapping(value = "/HTML5Client/" + MAP_NAME + "/" + PROJECT_NAME + VARIANT + DATA_FLAVOR + ".data.js.gz", produces = "text/javascript")
    public ResponseEntity<ClassPathResource> getClientDataJsGz(
            @PathVariable String mapName,
            @PathVariable String variant,
            @PathVariable String dataFlavor,
            HttpServletResponse response) {

        ClassPathResource resource = classPathResource(String.format("/HTML5Client/%s/HTML5/%s%s%s.data.js.gz", mapName, coreProperties.getUnrealProjectName(), variant, dataFlavor));

        return ResponseEntity.ok()
                .header("Content-Encoding", "gzip")
                .body(resource);
    }

    @GetMapping(value = "/HTML5Client/" + MAP_NAME + "/" + PROJECT_NAME + VARIANT + DATA_FLAVOR + ".data.gz", produces = MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<ClassPathResource> getClientDataGz(
            @PathVariable String mapName,
            @PathVariable String variant,
            @PathVariable String dataFlavor,
            HttpServletResponse response) {

        ClassPathResource resource = classPathResource(String.format("/HTML5Client/%s/HTML5/%s%s%s.data.gz", mapName, coreProperties.getUnrealProjectName(), variant, dataFlavor));

        return ResponseEntity.ok()
                .header("Content-Encoding", "gzip")
                .body(resource);
    }

    @GetMapping(value = "/HTML5Client/" + MAP_NAME + "/" + PROJECT_NAME + "Client" + VARIANT + ".js.gz", produces = "text/javascript")
    public ResponseEntity<ClassPathResource> getClientClientJsGz(
            @PathVariable String mapName,
            @PathVariable String variant,
            HttpServletResponse response) {

        ClassPathResource resource = classPathResource(String.format("/HTML5Client/%s/HTML5/%sClient%s.js.gz", mapName, coreProperties.getUnrealProjectName(), variant));

        return ResponseEntity.ok()
                .header("Content-Encoding", "gzip")
                .body(resource);
    }

    @GetMapping(value = "/HTML5Client/" + MAP_NAME + "/" + PROJECT_NAME + "Client" + VARIANT + ".js.symbols.gz", produces = "text/javascript")
    public ResponseEntity<ClassPathResource> getClientClientJsSymbolsGz(
            @PathVariable String mapName,
            @PathVariable String variant,
            HttpServletResponse response) {

        ClassPathResource resource = classPathResource(String.format("/HTML5Client/%s/HTML5/%sClient%s.js.symbols.gz", mapName, coreProperties.getUnrealProjectName(), variant));

        return ResponseEntity.ok()
                .header("Content-Encoding", "gzip")
                .body(resource);
    }

    @GetMapping(value = "/HTML5Client/" + MAP_NAME + "/" + PROJECT_NAME + "Client" + VARIANT + ".wasm.gz", produces = "application/wasm")
    public ResponseEntity<ClassPathResource> getClientClientWasmGz(
            @PathVariable String mapName,
            @PathVariable String variant,
            HttpServletResponse response) {

        ClassPathResource resource = classPathResource(String.format("/HTML5Client/%s/HTML5/%sClient%s.wasm.gz", mapName, coreProperties.getUnrealProjectName(), variant));

        return ResponseEntity.ok()
                .header("Content-Encoding", "gzip")
                .body(resource);
    }

    @GetMapping(value = "/HTML5Client/" + MAP_NAME + "/Utility.js.gz", produces = "text/javascript")
    public ResponseEntity<ClassPathResource> getUtilityJsGz(
            @PathVariable String mapName,
            HttpServletResponse response) {

        ClassPathResource resource = classPathResource(String.format("/HTML5Client/%s/HTML5/Utility.js.gz", mapName));

        return ResponseEntity.ok()
                .header("Content-Encoding", "gzip")
                .body(resource);
    }

    @GetMapping(value = "/HTML5Client/" + MAP_NAME + "/jquery/jquery-2.1.3.min.js", produces = "text/javascript")
    public ResponseEntity<ClassPathResource> getJQueryJs(
            @PathVariable String mapName,
            HttpServletResponse response) {

        ClassPathResource resource = classPathResource(String.format("/HTML5Client/%s/HTML5/jquery/jquery-2.1.3.min.js", mapName));

        return ResponseEntity.ok(resource);
    }

    @GetMapping(value = "/HTML5Client/" + MAP_NAME + "/bootstrap/css/bootstrap.min.css", produces = "text/css")
    public ResponseEntity<ClassPathResource> getBootstrapCss(
            @PathVariable String mapName,
            HttpServletResponse response) {

        ClassPathResource resource = classPathResource(String.format("/HTML5Client/%s/HTML5/bootstrap/css/bootstrap.min.css", mapName));

        return ResponseEntity.ok(resource);
    }

    @GetMapping(value = "/HTML5Client/" + MAP_NAME + "/bootstrap/js/bootstrap.min.js", produces = "text/javascript")
    public ResponseEntity<ClassPathResource> getBootstrapJs(
            @PathVariable String mapName,
            HttpServletResponse response) {

        ClassPathResource resource = classPathResource(String.format("/HTML5Client/%s/HTML5/bootstrap/js/bootstrap.min.js", mapName));

        return ResponseEntity.ok(resource);
    }

    // the font access is from the root of the app - so just get the fonts from first mapName as they are all the same
    @GetMapping(value = "/HTML5Client/" + MAP_NAME + "/bootstrap/fonts/glyphicons-halflings-regular.ttf", produces = "font/ttf")
    public ResponseEntity<ClassPathResource> getFontsGlyphiconsHalflingsRegularTtf(
            @PathVariable String mapName,
            HttpServletResponse response) {

        ClassPathResource resource = classPathResource(String.format("/HTML5Client/%s/HTML5/bootstrap/fonts/glyphicons-halflings-regular.ttf", mapName));

        return ResponseEntity.ok(resource);
    }

    @GetMapping(value = "/HTML5Client/" + MAP_NAME + "/bootstrap/fonts/glyphicons-halflings-regular.woff", produces = "font/woff")
    public ResponseEntity<ClassPathResource> getFontsGlyphiconsHalflingsRegularWoff(
            @PathVariable String mapName,
            HttpServletResponse response) {

        ClassPathResource resource = classPathResource(String.format("/HTML5Client/%s/HTML5/bootstrap/fonts/glyphicons-halflings-regular.woff", mapName));

        return ResponseEntity.ok(resource);
    }

    @GetMapping(value = "/HTML5Client/" + MAP_NAME + "/bootstrap/fonts/glyphicons-halflings-regular.woff2", produces = "font/woff2")
    public ResponseEntity<ClassPathResource> getFontsGlyphiconsHalflingsRegularWoff2(
            @PathVariable String mapName,
            HttpServletResponse response) {

        ClassPathResource resource = classPathResource(String.format("/HTML5Client/%s/HTML5/bootstrap/fonts/glyphicons-halflings-regular.woff2", mapName));

        return ResponseEntity.ok(resource);
    }

    private ClassPathResource classPathResource(String path) {
        ClassPathResource resource = new ClassPathResource(path);

        // ensure nothing unexpected due to path normalization etc.
        Verify.verify(Objects.equals("/" + resource.getPath(), path));
        Verify.verify(resource.getPath().startsWith("HTML5Client/"));

        return resource;
    }
}
