package com.example.kaboom.config;

import com.example.kaboom.services.JwtServices;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * Runs before the WebSocket connection is accepted. Only a valid device access token
 * gets through, and the robot id is taken from the signed token, not from the client.
 */
@Component
public class RobotHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtServices jwtServices;

    public RobotHandshakeInterceptor(JwtServices jwtServices) {
        this.jwtServices = jwtServices;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {

        String header = request.getHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        String token = header.substring(7);
        if (!jwtServices.isValidDeviceAccessToken(token)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        attributes.put("robotId", jwtServices.extractRobotId(token));
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // nothing to do after the handshake
    }
}