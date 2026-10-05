package com.f1sim.dto;

import java.time.LocalDateTime;
import java.util.List;

/** One row of a user's simulation history, shown on their profile page. */
public record ProfileSimulationDto(
        Long id,
        String grandPrixName,
        Integer season,
        String driverCode,
        LocalDateTime createdAt,
        Double predictedTotalTimeSeconds,
        Double deltaVsActualSeconds,
        List<StintSummaryDto> stints
) {}