
package com.example.kaboom.config;

import com.example.kaboom.repositories.UserRepository;
import com.example.kaboom.security.JwtAuthenticationFilter;
import com.example.kaboom.services.JwtServices;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
// Enables method-level authorization annotations such as @PreAuthorize.
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtServices jwtServices,
            UserRepository userRepository) throws Exception {

        // Creates the JWT filter using the existing JWT service and user repository.
        JwtAuthenticationFilter jwtFilter =
                new JwtAuthenticationFilter(jwtServices, userRepository);

        http
                // Disables CSRF protection because the API uses stateless Bearer JWT authentication.
                .csrf(AbstractHttpConfigurer::disable)

                .cors(Customizer.withDefaults())

                // Each request must provide its own authentication; no server-side login session is used.
                .sessionManagement(s -> s
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth

                        // Registration, login, token refresh, and logout remain publicly accessible.
                        .requestMatchers("/api/v1/auth/**", "/error").permitAll()

                        // Keeps Swagger UI and the OpenAPI specification publicly accessible.
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // Only ADMIN users can create products through either product endpoint.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/products",
                                "/api/v1/admin/products"
                        ).hasRole("ADMIN")

                        // All remaining endpoints require an authenticated user.
                        .anyRequest().authenticated())

                // Disables HTTP Basic; API requests use the Bearer JWT checked by our filter.
                .httpBasic(AbstractHttpConfigurer::disable)

                // Prevents REST requests from being redirected to an HTML login form.
                .formLogin(AbstractHttpConfigurer::disable)

                // Runs JWT authentication before Spring Security's username/password authentication filter.
                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOriginPatterns(List.of("http://localhost:*"));
        config.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));
        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
