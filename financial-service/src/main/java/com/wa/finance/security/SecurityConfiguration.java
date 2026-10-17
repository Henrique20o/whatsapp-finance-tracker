package com.wa.finance.security;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

@Configuration
public class SecurityConfiguration {

    @Bean
    FilterRegistrationBean<InternalApiKeyFilter> disableAutomaticInternalApiKeyFilterRegistration(
            InternalApiKeyFilter filter
    ) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    @Order(1)
    SecurityFilterChain healthSecurity(HttpSecurity http) throws Exception {
        configureStateless(http)
                .securityMatcher("/actuator/health", "/actuator/health/**")
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());

        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain mobileSecurity(HttpSecurity http, SecurityErrorResponseWriter errorWriter)
            throws Exception {
        configureStateless(http)
                .securityMatcher("/mobile/v1/**")
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST,
                                "/mobile/v1/auth/codes",
                                "/mobile/v1/auth/codes/verify",
                                "/mobile/v1/auth/refresh",
                                "/mobile/v1/auth/logout"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
                            errorWriter.write(
                                    request,
                                    response,
                                    401,
                                    "UNAUTHORIZED",
                                    "Autenticação necessária"
                            );
                        })
                        .accessDeniedHandler((request, response, exception) -> errorWriter.write(
                                request,
                                response,
                                403,
                                "ACCESS_DENIED",
                                "Acesso negado"
                        ))
                )
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> { })
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"invalid_token\"");
                            errorWriter.write(
                                    request,
                                    response,
                                    401,
                                    "INVALID_ACCESS_TOKEN",
                                    "O access token é inválido ou expirou"
                            );
                        })
                );

        return http.build();
    }

    @Bean
    @Order(3)
    SecurityFilterChain internalSecurity(HttpSecurity http, InternalApiKeyFilter internalApiKeyFilter)
            throws Exception {
        configureStateless(http)
                .securityMatcher("/v1/**")
                .authorizeHttpRequests(authorize -> authorize.anyRequest().hasRole("INTERNAL"))
                .addFilterBefore(internalApiKeyFilter, AuthorizationFilter.class);

        return http.build();
    }

    @Bean
    @Order(4)
    SecurityFilterChain fallbackSecurity(HttpSecurity http, SecurityErrorResponseWriter errorWriter)
            throws Exception {
        configureStateless(http)
                .authorizeHttpRequests(authorize -> authorize.anyRequest().denyAll())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> errorWriter.write(
                                request,
                                response,
                                403,
                                "ACCESS_DENIED",
                                "Acesso negado"
                        ))
                        .accessDeniedHandler((request, response, exception) -> errorWriter.write(
                                request,
                                response,
                                403,
                                "ACCESS_DENIED",
                                "Acesso negado"
                        ))
                );

        return http.build();
    }

    private HttpSecurity configureStateless(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .requestCache(cache -> cache.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    }
}
