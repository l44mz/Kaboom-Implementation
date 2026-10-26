package com.example.kaboom.websocket;

import com.example.kaboom.dtos.FocusStateRequest;
import com.example.kaboom.dtos.FocusStateResponse;
import com.example.kaboom.services.FocusSessionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * Receives focus-state events from the robot over WebSocket and replies
 * with the same response the HTTP endpoint would return.
 */
@Component
public class RobotWebSocketHandler extends TextWebSocketHandler {

    private final FocusSessionService focusSessionService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public RobotWebSocketHandler(FocusSessionService focusSessionService,
                                 ObjectMapper objectMapper,
                                 Validator validator) {
        this.focusSessionService = focusSessionService;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @Override
    protected void handleTextMessage(WebSocketSession ws, TextMessage message) throws IOException {
        UUID robotId = (UUID) ws.getAttributes().get("robotId");

        try {
            FocusStateRequest request =
                    objectMapper.readValue(message.getPayload(), FocusStateRequest.class);

            if (!validator.validate(request).isEmpty()) {
                reply(ws, Map.of("error", "Invalid message"));
                return;
            }

            FocusStateResponse response = focusSessionService.recordState(robotId, request);
            reply(ws, response);

        } catch (ResponseStatusException e) {
            reply(ws, Map.of("error", e.getReason() == null ? "Error" : e.getReason()));
        } catch (JsonProcessingException e) {
            reply(ws, Map.of("error", "Malformed JSON"));
        } catch (Exception e) {
            reply(ws, Map.of("error", "Server error"));
        }
    }

    private void reply(WebSocketSession ws, Object body) throws IOException {
        ws.sendMessage(new TextMessage(objectMapper.writeValueAsString(body)));
    }
}
