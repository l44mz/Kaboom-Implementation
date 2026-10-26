package com.example.kaboom.config;

import com.example.kaboom.websocket.RobotWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;


@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final RobotWebSocketHandler robotHandler;
    private final RobotHandshakeInterceptor robotInterceptor;

    public WebSocketConfig(RobotWebSocketHandler robotHandler,
                           RobotHandshakeInterceptor robotInterceptor) {
        this.robotHandler = robotHandler;
        this.robotInterceptor = robotInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(robotHandler, "/ws/robot")
                .addInterceptors(robotInterceptor)
                .setAllowedOriginPatterns("*");   // the Pi isn't a browser; tighten later
    }
}
