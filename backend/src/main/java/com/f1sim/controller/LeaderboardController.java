package com.f1sim.controller;

import com.f1sim.dto.LeaderboardEntryDto;
import com.f1sim.entity.StrategySimulation;
import com.f1sim.repository.StrategySimulationRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
@Tag(name = "Leaderboard", description = "Per-circuit ranking by strategy prediction accuracy")
public class LeaderboardController {

    private final StrategySimulationRepository simulationRepository;

    @GetMapping
    public List<LeaderboardEntryDto> getLeaderboard(@RequestParam Long circuitId) {
        List<StrategySimulation> simulations =
                simulationRepository.findByRace_Circuit_IdAndDeltaVsActualSecondsIsNotNull(circuitId);

        // Group by user, keep only each user's single most accurate simulation on this circuit.
        Map<Long, StrategySimulation> bestPerUser = simulations.stream()
                .collect(Collectors.toMap(
                        s -> s.getUser().getId(),
                        s -> s,
                        (a, b) -> Math.abs(a.getDeltaVsActualSeconds()) <= Math.abs(b.getDeltaVsActualSeconds()) ? a : b
                ));

        return bestPerUser.values().stream()
                .sorted(Comparator.comparingDouble(s -> Math.abs(s.getDeltaVsActualSeconds())))
                .limit(50)
                .map(s -> new LeaderboardEntryDto(
                        s.getUser().getId(),
                        s.getUser().getUsername(),
                        Math.abs(s.getDeltaVsActualSeconds())
                ))
                .toList();
    }
}