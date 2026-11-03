package com.example.kaboom.services;

import com.example.kaboom.dtos.HistoryResponse;
import com.example.kaboom.entities.FocusSession;
import com.example.kaboom.entities.StateLog;
import com.example.kaboom.repositories.HistoryRepository;
import com.example.kaboom.repositories.StateLogRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class HistoryService {

    private final HistoryRepository historyRepository;
    private final StateLogRepository stateLogRepository;

    public HistoryService(
            HistoryRepository historyRepository,
            StateLogRepository stateLogRepository
    ) {
        this.historyRepository = historyRepository;
        this.stateLogRepository = stateLogRepository;
    }

    @Transactional(readOnly = true)
    public HistoryResponse getHistory(UUID userId, String period) {

        String selectedPeriod = period == null
                ? "WEEK"
                : period.trim().toUpperCase(Locale.ROOT);

        LocalDate today = LocalDate.now();
        LocalDateTime start;
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        List<FocusSession> sessions;

        switch (selectedPeriod) {
            case "DAY":
                start = today.atStartOfDay();
                break;

            case "WEEK":
                start = today.minusDays(6).atStartOfDay();
                break;

            case "MONTH":
                start = today.withDayOfMonth(1).atStartOfDay();
                break;

            case "ALL_TIME":
                start = null;
                break;

            default:
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid period. Use DAY, WEEK, MONTH, or ALL_TIME"
                );
        }

        if (start == null) {
            sessions = historyRepository
                    .findByUser_IdOrderByStartTimeDesc(userId);
        } else {
            sessions = historyRepository
                    .findByUser_IdAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeDesc(
                            userId, start, end
                    );
        }

        // History contains completed sessions only.
        sessions = sessions.stream()
                .filter(s -> s.getStatus()
                        == FocusSession.SessionStatus.completed)
                .toList();

        long totalFocusSeconds = sessions.stream()
                .mapToLong(FocusSession::getFocusedSeconds)
                .sum();

        long totalSessions = sessions.size();

        long averageFocusSeconds = totalSessions > 0
                ? totalFocusSeconds / totalSessions
                : 0;

        HistoryResponse.HistorySummaryDto summary =
                new HistoryResponse.HistorySummaryDto(
                        totalFocusSeconds,
                        totalSessions,
                        averageFocusSeconds
                );

        List<HistoryResponse.HistorySessionDto> pastSessions =
                sessions.stream()
                        .map(session -> {

                            long distractions = stateLogRepository
                                    .countByFocusSession_IdAndState(
                                            session.getId(),
                                            StateLog.State.distracted
                                    );

                            int activeDuration = Math.max(
                                    0,
                                    session.getDurationSeconds()
                                            - session.getPausedSeconds()
                            );

                            return new HistoryResponse.HistorySessionDto(
                                    session.getId(),
                                    session.getStartTime(),
                                    activeDuration,
                                    Math.toIntExact(distractions),
                                    session.getFocusedPercentage(),
                                    session.getRewardEarned()
                            );
                        })
                        .toList();

        // Weekly chart always shows the most recent seven calendar days.
        LocalDate weekStart = today.minusDays(6);

        List<FocusSession> weeklySessions = historyRepository
                .findByUser_IdAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeDesc(
                        userId,
                        weekStart.atStartOfDay(),
                        today.plusDays(1).atStartOfDay()
                )
                .stream()
                .filter(s -> s.getStatus()
                        == FocusSession.SessionStatus.completed)
                .toList();

        Map<LocalDate, Long> focusByDay = weeklySessions.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getStartTime().toLocalDate(),
                        Collectors.summingLong(
                                FocusSession::getFocusedSeconds
                        )
                ));

        List<HistoryResponse.DailyFocusDto> weeklyFocus =
                new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            LocalDate date = weekStart.plusDays(i);

            weeklyFocus.add(
                    new HistoryResponse.DailyFocusDto(
                            date,
                            focusByDay.getOrDefault(date, 0L)
                    )
            );
        }

        return new HistoryResponse(
                selectedPeriod,
                summary,
                weeklyFocus,
                pastSessions
        );
    }
}