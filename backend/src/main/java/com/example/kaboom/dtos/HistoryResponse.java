package com.example.kaboom.dtos;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Contains all response DTOs required by the History page.
 */
public record HistoryResponse(
        String period,
        HistorySummaryDto summary,
        List<DailyFocusDto> weeklyFocus,
        List<HistorySessionDto> sessions
) {

    /**
     * Summary statistics displayed at the top of History.
     */
    public record HistorySummaryDto(
            long totalFocusSeconds,
            long totalSessions,
            long averageFocusSeconds
    ) {}

    /**
     * Daily focus duration displayed in the weekly chart.
     */
    public record DailyFocusDto(
            LocalDate date,
            long focusSeconds
    ) {}

    /**
     * Details of one past focus session.
     */
    public record HistorySessionDto(
            UUID sessionId,
            LocalDateTime startTime,
            int durationSeconds,
            Integer distractions,
            Float focusedPercentage,
            int coinsEarned
    ) {}
}