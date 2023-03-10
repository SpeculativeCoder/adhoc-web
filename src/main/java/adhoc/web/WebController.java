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

import adhoc.AdhocProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves up the index.html which provides the Angular app.
 */
@Controller
@Slf4j
@RequiredArgsConstructor
public class WebController {

    private final AdhocProperties adhocProperties;

    // TODO: better way than this to catch browser refresh in non-root URLs
    @GetMapping(value = {
            "/",
            "/login-or-register/**",
            "/map/**",
            "/servers/**",
            "/regions/**",
            "/areas/**",
            "/objectives/**",
            "/factions/**",
            "/users/**",
            "/pawns/**",
            "/structures/**",
            "/pages/**",
            "/client/**"
    })
    public String getIndex(Model model) {
        model.addAttribute("MODE", adhocProperties.getMode());
        model.addAttribute("FEATURE_FLAGS", adhocProperties.getFeatureFlags());
        return "index.html";
    }
}

//@GetMapping("/robots.txt")
//public String getRobotsTxt() {
//    return "robots.txt";
//}
