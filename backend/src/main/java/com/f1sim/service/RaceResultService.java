package com.f1sim.service;

import com.f1sim.client.OpenF1Client;
import com.f1sim.client.dto.OpenF1SessionResultDto;
import com.f1sim.entity.Driver;
import com.f1sim.entity.Race;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Derives a driver's actual total race time from OpenF1's session_result
 * endpoint (beta). Only the race winner gets an absolute `duration`;
 * every other classified driver's time is winnerDuration + gap_to_leader.
 * Lapped drivers only get a "+N LAP(S)" string instead of a numeric gap,
 * so their exact time can't be recovered from this endpoint — those,
 * plus DNF/DNS/DSQ entries, simply aren't scoreable.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RaceResultService {

    private final OpenF1Client openF1Client;

    public Optional<Double> getActualTotalTimeSeconds(Race race, Driver driver) {
        if (race.getExternalSessionKey() == null || driver.getPermanentNumber() == null) {
            return Optional.empty();
        }

        int sessionKey;
        try {
            sessionKey = Integer.parseInt(race.getExternalSessionKey());
        } catch (NumberFormatException e) {
            log.warn("Race {} has a non-numeric externalSessionKey: {}", race.getId(), race.getExternalSessionKey());
            return Optional.empty();
        }

        List<OpenF1SessionResultDto> results = openF1Client.getSessionResult(sessionKey);

        Optional<OpenF1SessionResultDto> winner = results.stream()
                .filter(r -> Integer.valueOf(1).equals(r.position()))
                .filter(r -> r.durationSeconds() != null)
                .findFirst();

        if (winner.isEmpty()) {
            log.warn("No classified winner with a duration found for session {}", sessionKey);
            return Optional.empty();
        }

        double winnerTime = winner.get().durationSeconds();

        return results.stream()
                .filter(r -> driver.getPermanentNumber().equals(r.driverNumber()))
                .findFirst()
                .flatMap(entry -> toActualTime(entry, winnerTime));
    }

    private Optional<Double> toActualTime(OpenF1SessionResultDto entry, double winnerTime) {
        if (Boolean.TRUE.equals(entry.dnf()) || Boolean.TRUE.equals(entry.dns()) || Boolean.TRUE.equals(entry.dsq())) {
            return Optional.empty();
        }
        if (entry.durationSeconds() != null) {
            return Optional.of(entry.durationSeconds()); // the winner's own row
        }
        if (entry.hasNumericGap()) {
            return Optional.of(winnerTime + entry.gapToLeaderSeconds());
        }
        return Optional.empty(); // lapped driver — only "+N LAP" available, no exact seconds
    }
}