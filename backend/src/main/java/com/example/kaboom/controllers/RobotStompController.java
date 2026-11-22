package com.example.kaboom.controllers;

import com.example.kaboom.dtos.FocusStateRequest;
import com.example.kaboom.dtos.FocusStateResponse;
import com.example.kaboom.services.FocusSessionService;
import jakarta.validation.Valid;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;
import java.security.Principal;
import java.util.Map;
import java.util.UUID;

@Controller
public class RobotStompController {

    private final FocusSessionService focusSessionService;

    public RobotStompController(FocusSessionService focusSessionService) {
        this.focusSessionService = focusSessionService;
    }

    /** Robot sends to /app/focus-states; the reply goes back to /user/queue/expression. */
    @MessageMapping("/focus-states")
    @SendToUser(value = "/queue/expression", broadcast = false)
    public FocusStateResponse onFocusState(@Valid @Payload FocusStateRequest request,
                                           Principal principal) {
        UUID robotId = UUID.fromString(principal.getName());   // set by StompAuthInterceptor
        return focusSessionService.recordState(robotId, request);
    }

    /** Errors go to /user/queue/errors; the connection stays open. */
    @MessageExceptionHandler
    @SendToUser(value = "/queue/errors", broadcast = false)
    public Map<String, String> onError(Exception e) {
        if (e instanceof ResponseStatusException rse) {
            return Map.of("error", rse.getReason() == null ? "Error" : rse.getReason());
        }
        if (e instanceof MethodArgumentNotValidException) {
            return Map.of("error", "Invalid message");
        }
        if (e instanceof MessageConversionException) {
            return Map.of("error", "Malformed JSON");
        }
        return Map.of("error", "Server error");
    }
}
