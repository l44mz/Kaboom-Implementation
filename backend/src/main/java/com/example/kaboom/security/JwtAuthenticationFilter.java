
package com.example.kaboom.security;

import com.example.kaboom.entities.User;
import com.example.kaboom.repositories.UserRepository;
import com.example.kaboom.services.JwtServices;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtServices jwtServices;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(
            JwtServices jwtServices,
            UserRepository userRepository) {
        this.jwtServices = jwtServices;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authorizationHeader =
                request.getHeader("Authorization");

        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(7).trim();

        if (token.isEmpty()
                || !jwtServices.isValidAccessToken(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        String email = jwtServices.extractEmail(token);

        if (SecurityContextHolder.getContext()
                .getAuthentication() == null) {

            User user = userRepository.findByEmail(email)
                    .orElse(null);

            if (user != null && user.getRole() != null) {

                String authority = "ROLE_"
                        + user.getRole().toUpperCase(Locale.ROOT);

                var authentication =
                        new UsernamePasswordAuthenticationToken(
                                user.getEmail(),
                                null,
                                List.of(new SimpleGrantedAuthority(authority))
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}
