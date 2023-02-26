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
import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AdhocProperties adhocProperties;

    @Value("${server.http.port}")
    private Integer httpPort;

    @EventListener
    public void contextRefreshed(ContextRefreshedEvent event) {
        log.info("httpPort={}", httpPort);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // add a cors mapping to allow local access when using "ng serve" to host the
        // frontend locally on port 4200
        registry.addMapping("/**")
                // TODO: guard this for dev/local environment only
                //.allowedOrigins("http://127.0.0.1:4200", "http://localhost:4200")
                .allowedOriginPatterns(
                        "https://" + adhocProperties.getAdhocDomain(),
                        "http://" + adhocProperties.getAdhocDomain(),
                        "https://*." + adhocProperties.getAdhocDomain(),
                        "http://*." + adhocProperties.getAdhocDomain())
                .allowedMethods("*")
                .allowedHeaders("*")
                .exposedHeaders("*")
                .allowCredentials(true);
    }

    /** Also allow HTTP access on port 80 for now. */
    @Bean
    WebServerFactoryCustomizer<TomcatServletWebServerFactory> adhocTomcatCustomizer() {
        return (TomcatServletWebServerFactory factory) -> {
            final Connector connector = new Connector();
            connector.setPort(httpPort);
            factory.addAdditionalTomcatConnectors(connector);
        };
    }
}

//@Slf4j
//@Configuration
//@RequiredArgsConstructor
//public class ErrorConfig {
//
//	private final ApplicationContext applicationContext;
//
//	private final WebProperties webProperties;
//
//	@Bean
//	public ErrorViewResolver errorViewResolver() {
//		return new DefaultErrorViewResolver(this.applicationContext, this.webProperties.getResources()) {
//			@Override
//			public ModelAndView resolveErrorView(HttpServletRequest request, HttpStatus status, Map<String, Object> model) {
//				// any 404 just send them to the main page for now
//				if (status == HttpStatus.NOT_FOUND) {
//					return new ModelAndView("forward:/", HttpStatus.PERMANENT_REDIRECT);
//				} else {
//					return super.resolveErrorView(request, status, model);
//				}
//			}
//		};
//	}
//}
