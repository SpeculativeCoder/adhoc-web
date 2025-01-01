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

package adhoc.system;

import adhoc.system.properties.CoreProperties;
import com.google.common.collect.ImmutableMap;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

/** Serves up the index.html which provides the Angular app. */
@RestController
@RequiredArgsConstructor
public class IndexController {

    private final CoreProperties coreProperties;

    // TODO: better way than this to catch browser refresh in non-root URLs
    @GetMapping(value = {
            "/",
            "/login-or-register/**",
            "/map/**",
            "/servers/**",
            "/tasks/**",
            "/regions/**",
            "/areas/**",
            "/objectives/**",
            "/factions/**",
            "/users/**",
            "/pawns/**",
            "/structures/**",
            "/messages/**",
            "/pages/**",
            "/client/**"
    })
    public ModelAndView getIndex() {
        //noinspection SpringMVCViewInspection
        return new ModelAndView("index",
                ImmutableMap.of("FEATURE_FLAGS", coreProperties.getFeatureFlags()));
    }

    @GetMapping("/favicon.ico")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Void getFavicon() {
        return null;
    }

    //// cut down on log spam a bit for all attempts which are trying to POST to /
    //@PostMapping("/")
    //@ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    //public Void postIndex() {
    //    return null;
    //}
}
