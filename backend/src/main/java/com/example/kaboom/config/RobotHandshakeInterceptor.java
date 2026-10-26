package com.example.kaboom.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.UUID;

@Component
public class RobotHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler handler, Map<String, Object> attributes) {
        String robotId = UriComponentsBuilder.fromUri(request.getURI())
                .build().getQueryParams().getFirst("robotId");
        try {
            attributes.put("robotId", UUID.fromString(robotId));
            return true;
        } catch (Exception e) {
            return false;   // rejects the handshake
        }
        // TODO: replace with device-JWT validation; take robotId from the token claim
    }

    @Override
    public void afterHandshake(ServerHttpRequest req, ServerHttpResponse res,
                               WebSocketHandler handler, Exception ex) {}
}
