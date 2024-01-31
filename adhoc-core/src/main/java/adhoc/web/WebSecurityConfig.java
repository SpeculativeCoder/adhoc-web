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

package adhoc.web;

import adhoc.user.AdhocAuthenticationSuccessHandler;
import adhoc.web.ignore_server_csrf.ServerRequestMatcher;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.session.security.SpringSessionBackedSessionRegistry;
import org.springframework.session.security.web.authentication.SpringSessionRememberMeServices;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class WebSecurityConfig<S extends Session> {

    private final AdhocAuthenticationSuccessHandler adhocAuthenticationSuccessHandler;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private final FindByIndexNameSessionRepository<S> jdbcIndexedSessionRepository;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   ServerRequestMatcher serverRequestMatcher,
                                                   RememberMeServices rememberMeServices,
                                                   SpringSessionBackedSessionRegistry<S> sessionRegistry) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        // TODO: ideally have a separate one for anonymous?
                        .requestMatchers("/ws/stomp/user_sockjs/**").permitAll()
                        //.requestMatchers("/ws/stomp/user/**").permitAll()
                        .requestMatchers("/ws/stomp/server/**").hasAnyRole("SERVER")
                        .requestMatchers("/ws/stomp/**").denyAll()
                        .requestMatchers("/HTML5Client/**").hasAnyRole("USER") // TODO
                        .requestMatchers("/api/users/login").permitAll()
                        .requestMatchers("/api/users/register").permitAll()
                        .requestMatchers("/api/**").permitAll() // TODO: some should be for logged in only
                        .requestMatchers("*.js").permitAll() // TODO
                        .requestMatchers("*.ico").permitAll() // TODO
                        .requestMatchers("/**").permitAll() // TODO
                        .anyRequest().denyAll())
                .headers(headers ->
                        // for sockjs
                        headers.frameOptions(frameOptions ->
                                frameOptions.sameOrigin()))
                .csrf(csrf -> csrf
                        // ignore CSRF for sockjs as protected by Stomp headers
                        .ignoringRequestMatchers("/ws/stomp/user_sockjs/**")
                        //.ignoringRequestMatchers("/ws/stomp/user/**")
                        //.ignoringRequestMatchers("/ws/stomp/server/**")
                        // we don't want CSRF on requests from Unreal server
                        .ignoringRequestMatchers(serverRequestMatcher))
                .cors(cors -> Customizer.withDefaults())
                .sessionManagement(session -> session
                        .sessionAuthenticationStrategy(sessionAuthenticationStrategy())
                        .sessionConcurrency(concurrency -> concurrency
                                //.maximumSessions(1)
                                .sessionRegistry(sessionRegistry)))
                // allow form login - used by users
                .formLogin(form -> form
                        .loginPage("/login")
                        .failureHandler((request, response, exception) ->
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED))
                        .successHandler(adhocAuthenticationSuccessHandler))
                // allow basic auth (Authorization header) - used by the server
                .httpBasic(Customizer.withDefaults())
                .rememberMe(remember -> remember
                        .rememberMeServices(rememberMeServices))
                .build();
    }

    @Bean
    public ServerRequestMatcher serverRequestMatcher() {
        return new ServerRequestMatcher();
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder);

        ProviderManager providerManager = new ProviderManager(authenticationProvider);
        return providerManager;
    }

    @Bean
    public SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new ChangeSessionIdAuthenticationStrategy();
    }

    @Bean
    public SpringSessionBackedSessionRegistry<S> sessionRegistry() {
        return new SpringSessionBackedSessionRegistry<>(jdbcIndexedSessionRepository);
    }

    @Bean
    public RememberMeServices rememberMeServices() {
        SpringSessionRememberMeServices rememberMeServices = new SpringSessionRememberMeServices();
        //rememberMeServices.setAlwaysRemember(true);
        return rememberMeServices;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    //@Bean
    //public LoggerListener loggerListener() {
    //    LoggerListener loggerListener = new LoggerListener();
    //    //loggerListener.setLogInteractiveAuthenticationSuccessEvents(false);
    //    return loggerListener;
    //}
}

//	@Bean
//	public UserDetailsManager userDetailsManager(PasswordEncoder passwordEncoder) {
//		return new InMemoryUserDetailsManager(
//				User.withUsername("user").password(passwordEncoder.encode("password")).roles("USER").build(),
//				User.withUsername("admin").password(passwordEncoder.encode("password")).roles("ADMIN").build()) {
//		};
//	}

//@Component
//public class AdhocCsrfTokenRepository implements CsrfTokenRepository {
//
//    private static final String SERVER_CSRF_TOKEN = "SERVER";
//
//    private HttpSessionCsrfTokenRepository delegate = new HttpSessionCsrfTokenRepository();
//
//    @Override
//    public CsrfToken generateToken(HttpServletRequest request) {
//        CsrfToken csrfToken = delegate.generateToken(request);
//
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        System.err.println(authentication);
//        if (authentication instanceof UsernamePasswordAuthenticationToken authenticationToken) {
//            if (authenticationToken.getAuthorities()
//                    .stream().anyMatch(authority -> "ROLE_SERVER".equals(authority.getAuthority()))) {
//
//                csrfToken = new DefaultCsrfToken(csrfToken.getHeaderName(), csrfToken.getParameterName(), SERVER_CSRF_TOKEN);
//            }
//        }
//
//        System.err.println(csrfToken == null ? null : ReflectionToStringBuilder.toString(csrfToken));
//        return csrfToken;
//    }
//
//    @Override
//    public void saveToken(CsrfToken token, HttpServletRequest request, HttpServletResponse response) {
//        delegate.saveToken(token, request, response);
//    }
//
//    @Override
//    public CsrfToken loadToken(HttpServletRequest request) {
//        CsrfToken csrfToken = delegate.loadToken(request);
//
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        System.err.println(authentication);
//        if (authentication instanceof UsernamePasswordAuthenticationToken authenticationToken) {
//            if (authenticationToken.getAuthorities()
//                    .stream().anyMatch(authority -> "ROLE_SERVER".equals(authority.getAuthority()))) {
//                csrfToken = new DefaultCsrfToken(csrfToken.getHeaderName(), csrfToken.getParameterName(), SERVER_CSRF_TOKEN);
//            }
//        }
//
//        System.err.println(csrfToken == null ? null : ReflectionToStringBuilder.toString(csrfToken));
//        return csrfToken;
//    }
//}
