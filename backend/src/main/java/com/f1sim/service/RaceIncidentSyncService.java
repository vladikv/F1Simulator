package com.f1sim.service;

import com.f1sim.client.OpenF1Client;
import com.f1sim.client.dto.OpenF1LapDto;
import com.f1sim.client.dto.OpenF1RaceControlDto;
import com.f1sim.entity.Race;
import com.f1sim.entity.RaceIncident;
import com.f1sim.repository.RaceIncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Detects red flag / safety car / VSC windows from OpenF1's race_control
 * feed and estimates how much time each one cost, using the reference
 * driver's lap times as the "clean pace" baseline.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RaceIncidentSyncService {

    private final OpenF1Client openF1Client;
    private final RaceIncidentRepository incidentRepository;
    private final SessionLapTimelineService timeline;

    public void sync(Race race, int sessionKey, int referenceDriverNumber, List<OpenF1LapDto> referenceLaps) {
        List<OpenF1RaceControlDto> messages = openF1Client.getRaceControl(sessionKey).stream()
                .filter(m -> m.date() != null)
                .sorted(Comparator.comparing(m -> Instant.parse(m.date())))
                .toList();

        double cleanLapSeconds = timeline.medianCleanLapSeconds(referenceLaps);

        List<RaceIncident> incidents = new ArrayList<>();
        incidents.addAll(detectRedFlagWindows(messages, referenceLaps, race));

        incidents.addAll(detectWindows(messages, referenceLaps, cleanLapSeconds, race,
                RaceIncident.IncidentType.SAFETY_CAR,
                m -> "SafetyCar".equals(m.category()) && contains(m.message(), "SAFETY CAR DEPLOYED"),
                m -> "SafetyCar".equals(m.category()) && contains(m.message(), "SAFETY CAR IN THIS LAP")));

        incidents.addAll(detectWindows(messages, referenceLaps, cleanLapSeconds, race,
                RaceIncident.IncidentType.VIRTUAL_SAFETY_CAR,
                m -> "SafetyCar".equals(m.category()) && contains(m.message(), "VIRTUAL SAFETY CAR DEPLOYED"),
                m -> "SafetyCar".equals(m.category()) && contains(m.message(), "VIRTUAL SAFETY CAR ENDING")));

        incidentRepository.saveAll(incidents);
        log.info("Race {}: synced {} incidents (red flag/SC/VSC)", race.getId(), incidents.size());
    }

    private List<RaceIncident> detectWindows(
            List<OpenF1RaceControlDto> messages,
            List<OpenF1LapDto> referenceLaps,
            double cleanLapSeconds,
            Race race,
            RaceIncident.IncidentType type,
            java.util.function.Predicate<OpenF1RaceControlDto> isStart,
            java.util.function.Predicate<OpenF1RaceControlDto> isEnd
    ) {
        List<RaceIncident> result = new ArrayList<>();
        Integer pendingStartLap = null;

        for (OpenF1RaceControlDto message : messages) {
            if (pendingStartLap == null && isStart.test(message)) {
                pendingStartLap = resolveLapNumber(message, referenceLaps);
            } else if (pendingStartLap != null && isEnd.test(message)) {
                Integer endLap = resolveLapNumber(message, referenceLaps);
                if (endLap != null && endLap >= pendingStartLap) {
                    double timeLoss = estimateTimeLoss(referenceLaps, pendingStartLap, endLap, cleanLapSeconds);
                    result.add(RaceIncident.builder()
                            .race(race)
                            .type(type)
                            .startLap(pendingStartLap)
                            .endLap(endLap)
                            .timeLossSeconds(timeLoss)
                            .build());
                }
                pendingStartLap = null;
            }
        }
        return result;
    }

    /**
     * Red flags fully stop the race (lap counting freezes), unlike safety cars where
     * laps continue. So instead of comparing lap-by-lap pace, the time lost is just
     * the raw wall-clock gap between the stoppage and the restart.
     */
    private List<RaceIncident> detectRedFlagWindows(
            List<OpenF1RaceControlDto> messages,
            List<OpenF1LapDto> referenceLaps,
            Race race
    ) {
        List<RaceIncident> result = new ArrayList<>();
        Instant pendingStartTime = null;
        Integer pendingStartLap = null;

        for (OpenF1RaceControlDto message : messages) {
            boolean isRedFlag = "Flag".equals(message.category()) && "Red".equalsIgnoreCase(message.flag());
            boolean isRestart = "SessionStatus".equals(message.category())
                    && message.message() != null
                    && message.message().toUpperCase().contains("SESSION STARTED");

            if (pendingStartTime == null && isRedFlag) {
                pendingStartTime = Instant.parse(message.date());
                pendingStartLap = resolveLapNumber(message, referenceLaps);
            } else if (pendingStartTime != null && isRestart) {
                Instant endTime = Instant.parse(message.date());
                double timeLoss = java.time.Duration.between(pendingStartTime, endTime).getSeconds();

                result.add(RaceIncident.builder()
                        .race(race)
                        .type(RaceIncident.IncidentType.RED_FLAG)
                        .startLap(pendingStartLap != null ? pendingStartLap : 1)
                        .endLap(pendingStartLap != null ? pendingStartLap : 1)
                        .timeLossSeconds(timeLoss)
                        .build());

                pendingStartTime = null;
                pendingStartLap = null;
            }
        }
        return result;
    }

    private Integer resolveLapNumber(OpenF1RaceControlDto message, List<OpenF1LapDto> referenceLaps) {
        if (message.lapNumber() != null) return message.lapNumber();
        return timeline.lapNumberAt(referenceLaps, Instant.parse(message.date()));
    }

    /** Actual time spent on the affected laps minus what they would have taken at clean pace. */
    private double estimateTimeLoss(List<OpenF1LapDto> referenceLaps, int startLap, int endLap, double cleanLapSeconds) {
        double actualTime = referenceLaps.stream()
                .filter(lap -> lap.lapNumber() >= startLap && lap.lapNumber() <= endLap)
                .mapToDouble(lap -> lap.lapDurationSeconds() != null ? lap.lapDurationSeconds() : cleanLapSeconds)
                .sum();

        int lapCount = endLap - startLap + 1;
        double expectedCleanTime = cleanLapSeconds * lapCount;

        return Math.max(0.0, actualTime - expectedCleanTime);
    }

    private boolean contains(String message, String needle) {
        return message != null && message.toUpperCase().contains(needle);
    }
}