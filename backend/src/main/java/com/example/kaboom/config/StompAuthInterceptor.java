package com.example.kaboom.config;

import com.example.kaboom.services.JwtServices;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Authenticates the STOMP CONNECT frame. Only a valid device access token is accepted,
 * and the robot id comes from the signed token. Later frames reuse this user.
 */
@Component
public class StompAuthInterceptor implements ChannelInterceptor {

    private final JwtServices jwtServices;

    public StompAuthInterceptor(JwtServices jwtServices) {
        this.jwtServices = jwtServices;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String header = accessor.getFirstNativeHeader("Authorization");
            if (header == null || !header.startsWith("Bearer ")) {
                throw new MessagingException("Missing token");
            }

            String token = header.substring(7);
            if (!jwtServices.isValidDeviceAccessToken(token)) {
                throw new MessagingException("Invalid device token");
            }

            String robotId = jwtServices.extractRobotId(token).toString();
            accessor.setUser(new UsernamePasswordAuthenticationToken(
                    robotId, null, List.of(new SimpleGrantedAuthority("ROLE_DEVICE"))));
        }
        return message;
    }
}