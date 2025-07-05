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

import adhoc.system.auth.AdhocAccessDeniedHandler;
import adhoc.system.auth.AdhocAuthenticationFailureHandler;
import adhoc.system.auth.AdhocAuthenticationSuccessHandler;
import adhoc.system.auth.AdhocServerBasicAuthRequestMatcher;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.session.security.SpringSessionBackedSessionRegistry;
import org.springframework.session.security.web.authentication.SpringSessionRememberMeServices;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("LombokGetterMayBeUsed")
public class WebSecurityConfiguration<S extends Session> {

    @Getter
    private SessionAuthenticationStrategy sessionAuthenticationStrategy;

    @Bean
    @SuppressWarnings("Convert2MethodRef")
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CsrfTokenRepository csrfTokenRepository,
            RememberMeServices rememberMeServices,
            SpringSessionBackedSessionRegistry<S> sessionRegistry,
            AdhocServerBasicAuthRequestMatcher adhocServerBasicAuthRequestMatcher,
            AdhocAuthenticationSuccessHandler adhocAuthenticationSuccessHandler,
            AdhocAuthenticationFailureHandler adhocAuthenticationFailureHandler,
            AdhocAccessDeniedHandler adhocAccessDeniedHandler) throws Exception {

        return http
                .authorizeHttpRequests(auth -> auth
                        // TODO: ideally have a separate one for anonymous?
                        .requestMatchers("/ws/stomp/user_sockjs/**").permitAll()
                        //.requestMatchers("/ws/stomp/user/**").permitAll()
                        .requestMatchers("/ws/stomp/server/**").hasAnyRole("SERVER")
                        .requestMatchers("/ws/stomp/**").denyAll()

                        .requestMatchers("/HTML5Client/**").hasAnyRole("USER")

                        .requestMatchers("/api/users/login").permitAll()
                        .requestMatchers("/api/users/register").permitAll()
                        .requestMatchers("/api/users/current").permitAll()
                        .requestMatchers("/api/**").permitAll() // TODO: some should be for logged in only

                        .requestMatchers("/csrf").permitAll()

                        .requestMatchers("/*.css").permitAll() // TODO
                        .requestMatchers("/*.js").permitAll() // TODO
                        .requestMatchers("/*.ico").permitAll() // TODO

                        .requestMatchers("/**").permitAll() // TODO

                        .anyRequest().denyAll())

                .headers(headers -> headers
                        // for sockjs
                        .frameOptions(frameOptions -> frameOptions
                                .sameOrigin()))

                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        // ignore CSRF for sockjs as protected by Stomp headers
                        .ignoringRequestMatchers("/ws/stomp/user_sockjs/**")
                        // we don't want CSRF on requests from Unreal server
                        .ignoringRequestMatchers(adhocServerBasicAuthRequestMatcher))

                .cors(withDefaults())

                .sessionManagement(session -> session
                        .sessionFixation(fixation -> fixation
                                .changeSessionId()
                                .withObjectPostProcessor(sessionAuthenticationStrategyPostProcessor()))
                        .sessionConcurrency(concurrency -> concurrency
                                //.maximumSessions(1)
                                .sessionRegistry(sessionRegistry)))

                // allow form login - used by users
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/api/login")
                        .failureHandler(adhocAuthenticationFailureHandler)
                        .successHandler(adhocAuthenticationSuccessHandler))

                // allow basic auth (Authorization header) - used by the server
                .httpBasic(withDefaults())

                .rememberMe(remember -> remember
                        .rememberMeServices(rememberMeServices))

                .anonymous(anonymous -> anonymous
                        .authorities(anonymousAuthorities().toArray(new String[0])))

                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .accessDeniedHandler(adhocAccessDeniedHandler))

                .build();
    }

    @Bean
    public HttpSessionCsrfTokenRepository csrfTokenRepository() {
        return new HttpSessionCsrfTokenRepository();
    }

    @Bean
    public SpringSessionBackedSessionRegistry<S> sessionRegistry(
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
            FindByIndexNameSessionRepository<S> jdbcIndexedSessionRepository) {
        return new SpringSessionBackedSessionRegistry<>(jdbcIndexedSessionRepository);
    }

    @Bean
    @SuppressWarnings("UnnecessaryLocalVariable")
    public RememberMeServices rememberMeServices() {
        SpringSessionRememberMeServices rememberMeServices = new SpringSessionRememberMeServices();
        //rememberMeServices.setAlwaysRemember(true);
        return rememberMeServices;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    private Set<String> anonymousAuthorities() {
        Set<String> anonymousAuthorities = new LinkedHashSet<>();
        anonymousAuthorities.add("ROLE_ANONYMOUS");
        //if (coreProperties.getFeatureFlags().contains("development")) {
        //    anonymousAuthorities.add("ROLE_" + UserRole.DEBUG.name());
        //}
        return anonymousAuthorities;
    }

    /** Post processor to keep a reference to the session authentication strategy (used when doing a programmatic login) */
    private ObjectPostProcessor<SessionAuthenticationStrategy> sessionAuthenticationStrategyPostProcessor() {
        return new ObjectPostProcessor<>() {
            @Override
            public <O extends SessionAuthenticationStrategy> O postProcess(O sessionAuthenticationStrategy) {
                // keep a reference to this so we can use it for our programmatic login code
                // TODO: would prefer it as a bean in the context
                WebSecurityConfiguration.this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
                return sessionAuthenticationStrategy;
            }
        };
    }

    //@Bean
    //public LoggerListener loggerListener() {
    //    LoggerListener loggerListener = new LoggerListener();
    //    //loggerListener.setLogInteractiveAuthenticationSuccessEvents(false);
    //    return loggerListener;
    //}

    // as of Spring Boot 3.4.1 / Spring Security 6.4.2, try to ignore processor from WebSocketObservationConfiguration for now
    //@Bean
    //@Primary
    //public ObjectPostProcessor<Object> primaryObjectPostProcessor(@Qualifier("objectPostProcessor") ObjectPostProcessor<Object> objectPostProcessor) {
    //    return objectPostProcessor;
    //}

    //@Bean
    //public UserDetailsManager userDetailsManager(PasswordEncoder passwordEncoder) {
    //    return new InMemoryUserDetailsManager(
    //            User.withUsername("user").password(passwordEncoder.encode("password")).roles("USER").build(),
    //            User.withUsername("admin").password(passwordEncoder.encode("password")).roles("ADMIN").build()) {
    //    };
    //}
}


