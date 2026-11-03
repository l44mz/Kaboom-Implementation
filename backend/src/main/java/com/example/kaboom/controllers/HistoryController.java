package com.example.kaboom.controllers;

import com.example.kaboom.dtos.HistoryResponse;
import com.example.kaboom.entities.User;
import com.example.kaboom.repositories.UserRepository;
import com.example.kaboom.services.HistoryService;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/history")
public class HistoryController {

    private final HistoryService historyService;
    private final UserRepository userRepository;

    public HistoryController(
            HistoryService historyService,
            UserRepository userRepository
    ) {
        this.historyService = historyService;
        this.userRepository = userRepository;
    }

    /**
     * Returns the authenticated user's focus history.
     *
     * Supported periods:
     * DAY, WEEK, MONTH, ALL_TIME
     */
    @GetMapping
    public HistoryResponse getHistory(
            @RequestParam(defaultValue = "WEEK") String period,
            Authentication authentication
    ) {

        if (authentication == null
                || authentication.getName() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required"
            );
        }

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        return historyService.getHistory(user.getId(), period);
    }
}