package com.f1sim.dto;

import java.time.LocalDateTime;

/** Summary shown in the races list — enough to pick a race, no lap-by-lap detail. */
public record RaceSummaryDto(
        Long id,
        String grandPrixName,
        Integer season,
        String circuitName,
        String country,
        LocalDateTime raceDateTime,
        String status,
        Integer totalLaps
) {}