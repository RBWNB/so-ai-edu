package com.gdou.marine.config;

import com.gdou.marine.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        // Public auth endpoints
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register").permitAll()

                        // Public read endpoints (species / ecosystem / visualization)
                        .requestMatchers(HttpMethod.GET, "/species/**", "/ecosystem/**", "/visual/**").permitAll()

                        // Authenticated user self-service
                        .requestMatchers("/sys-user/profile", "/sys-user/password", "/sys-user/upload/avatar").authenticated()

                        // Admin-only management endpoints
                        .requestMatchers("/sys-user/**").hasRole("ADMIN")
                        .requestMatchers("/sys-role/**").hasRole("ADMIN")
                        .requestMatchers("/sys-operation-log/**").hasRole("ADMIN")

                        // Species / Ecosystem write operations → ADMIN or MANAGER
                        .requestMatchers(HttpMethod.POST, "/species/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/species/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/species/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.POST, "/ecosystem/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/ecosystem/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/ecosystem/**").hasAnyRole("ADMIN", "MANAGER")

                        // AI endpoints → any authenticated user (C-end)
                        .requestMatchers("/ai/**").authenticated()

                        // RAG intelligent Q&A endpoints → any authenticated user (C-end)
                        .requestMatchers("/rag/**").authenticated()

                        // Knowledge base management → ADMIN / MANAGER
                        .requestMatchers(HttpMethod.GET, "/kb/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/kb/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/kb/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/kb/**").hasAnyRole("ADMIN", "MANAGER")

                        // Public static resources (uploads, images, etc.)
                        .requestMatchers("/uploads/**").permitAll()

                        // Logout requires a valid login context
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated()

                        // Everything else must be authenticated
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
