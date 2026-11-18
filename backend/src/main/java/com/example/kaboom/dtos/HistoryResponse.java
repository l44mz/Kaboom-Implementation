package com.example.kaboom.dtos;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Everything the History screen shows for one range (day, week, month or all).
 *
 * @param range              the range asked for: day, week, month or all
 * @param totalFocusSeconds  focused time in all finished sessions of the range
 * @param sessionCount       number of finished sessions in the range
 * @param avgFocusPercentage average focus % of those sessions (0 when there are none)
 * @param bars               the "Focus minutes" chart, oldest bar first
 * @param sessions           the finished sessions, newest first
 */
public record HistoryResponse(
        String range,
        int totalFocusSeconds,
        int sessionCount,
        float avgFocusPercentage,
        List<Bar> bars,
        List<PastSession> sessions) {

    /**
     * One bar of the chart.
     *
     * @param label        e.g. "M", "W2", "Oct", "8a"
     * @param focusMinutes focused minutes in that part of the range
     * @param current      true for the bar that holds "now" (today, this week, ...)
     */
    public record Bar(String label, int focusMinutes, boolean current) {}

    /** One finished session in the "Past sessions" list. */
    public record PastSession(
            UUID id,
            LocalDateTime startTime,
            int activeSeconds,
            int focusedSeconds,
            float focusedPercentage,
            int distractions,
            int rewardEarned) {}
}
