package com.example.kaboom.config;

import com.example.kaboom.services.JwtServices;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Runs before every request. A valid user access token authenticates the user
 * (principal = email, ROLE_USER). A valid device access token authenticates a
 * robot (principal = robot id, ROLE_DEVICE).
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtServices jwtServices;

    public JwtAuthFilter(JwtServices jwtServices) {
        this.jwtServices = jwtServices;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            if (jwtServices.isValidAccessToken(token)) {
                String email = jwtServices.extractEmail(token);
                var auth = new UsernamePasswordAuthenticationToken(
                        email, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
                SecurityContextHolder.getContext().setAuthentication(auth);

            } else if (jwtServices.isValidDeviceAccessToken(token)) {
                String robotId = jwtServices.extractRobotId(token).toString();
                var auth = new UsernamePasswordAuthenticationToken(
                        robotId, null, List.of(new SimpleGrantedAuthority("ROLE_DEVICE")));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(request, response);
    }
}
