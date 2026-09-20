package com.f1sim.service;

import com.f1sim.client.OpenF1Client;
import com.f1sim.client.dto.OpenF1LapDto;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * Shared helper for turning a session's lap timing data into two things
 * both the incident sync and the weather sync need: mapping a wall-clock
 * timestamp to a lap number, and a baseline "clean" lap time to compare
 * against.
 */
@Component
public class SessionLapTimelineService {

    private final OpenF1Client openF1Client;

    public SessionLapTimelineService(OpenF1Client openF1Client) {
        this.openF1Client = openF1Client;
    }

    /** Laps of a single reference driver (the winner), sorted by lap number. */
    public List<OpenF1LapDto> getReferenceLaps(int sessionKey, int driverNumber) {
        return openF1Client.getLaps(sessionKey, driverNumber).stream()
                .filter(lap -> lap.lapNumber() != null && lap.dateStart() != null)
                .sorted(Comparator.comparing(OpenF1LapDto::lapNumber))
                .toList();
    }

    /** Finds which lap was in progress at a given timestamp. Null if before the first lap. */
    public Integer lapNumberAt(List<OpenF1LapDto> laps, Instant timestamp) {
        Integer result = null;
        for (OpenF1LapDto lap : laps) {
            Instant lapStart = Instant.parse(lap.dateStart());
            if (lapStart.isAfter(timestamp)) break;
            result = lap.lapNumber();
        }
        return result;
    }

    /** Median lap duration, ignoring laps with no recorded duration (in/out laps, red flag laps). */
    public double medianCleanLapSeconds(List<OpenF1LapDto> laps) {
        List<Double> durations = laps.stream()
                .map(OpenF1LapDto::lapDurationSeconds)
                .filter(d -> d != null && d > 0)
                .sorted()
                .toList();

        if (durations.isEmpty()) return 0.0;
        int mid = durations.size() / 2;
        return durations.size() % 2 == 0
                ? (durations.get(mid - 1) + durations.get(mid)) / 2.0
                : durations.get(mid);
    }
}