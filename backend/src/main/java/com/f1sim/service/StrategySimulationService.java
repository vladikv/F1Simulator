package com.f1sim.service;

import com.f1sim.dto.StrategySimulationRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import com.f1sim.dto.LeaderboardEntryDto;
import com.f1sim.dto.StrategySimulationResponse;
import com.f1sim.dto.StintRequest;
import com.f1sim.entity.*;
import com.f1sim.repository.DriverRepository;
import com.f1sim.repository.RaceRepository;
import com.f1sim.repository.StrategySimulationRepository;
import com.f1sim.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Orchestrates a strategy simulation request: loads the race and
 * driver, delegates the actual math to StrategyEngineService, then
 * persists both the plan and its predicted result.
 */
@Service
@RequiredArgsConstructor
public class StrategySimulationService {

    private final StrategyEngineService engine;
    private final RaceRepository raceRepository;
    private final DriverRepository driverRepository;
    private final StrategySimulationRepository simulationRepository;
    private final RaceResultService raceResultService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public StrategySimulationResponse simulate(StrategySimulationRequest request, User currentUser) {
        Race race = raceRepository.findById(request.raceId())
                .orElseThrow(() -> new EntityNotFoundException("Race not found: " + request.raceId()));
        Driver driver = driverRepository.findById(request.driverId())
                .orElseThrow(() -> new EntityNotFoundException("Driver not found: " + request.driverId()));

        StrategySimulation simulation = StrategySimulation.builder()
                .user(currentUser)
                .race(race)
                .driver(driver)
                .createdAt(LocalDateTime.now())
                .build();

        List<TyreStint> stints = toStintEntities(request.stints(), simulation);
        simulation.setStints(stints);

        double teamPitStopTime = driver.getTeam() != null && driver.getTeam().getAvgPitStopSeconds() != null
                ? driver.getTeam().getAvgPitStopSeconds()
                : 2.5; // fallback average pit stop time

        double predictedTime = engine.simulateTotalRaceTime(
                stints, race.getCircuit(), race.getTotalLaps(), teamPitStopTime, race.getWeatherWindows()
        );
        simulation.setPredictedTotalTimeSeconds(predictedTime);

        if (race.getStatus() == Race.RaceStatus.FINISHED) {
            scoreAgainstActualResult(simulation, race, driver, currentUser);
        }

        StrategySimulation saved = simulationRepository.save(simulation);

        return new StrategySimulationResponse(
                saved.getId(),
                predictedTime,
                formatRaceTime(predictedTime),
                saved.getDeltaVsActualSeconds()
        );
    }

    private List<TyreStint> toStintEntities(List<StintRequest> requests, StrategySimulation simulation) {
        int order = 1;
        List<TyreStint> result = requests.stream().map(r -> TyreStint.builder()
                .simulation(simulation)
                .compound(r.compound())
                .startLap(r.startLap())
                .endLap(r.endLap())
                .build()
        ).collect(Collectors.toList());

        for (TyreStint stint : result) {
            stint.setStintOrder(order++);
        }
        return result;
    }

    private String formatRaceTime(double totalSeconds) {
        long hours = (long) totalSeconds / 3600;
        long minutes = ((long) totalSeconds % 3600) / 60;
        double seconds = totalSeconds % 60;
        return String.format("%d:%02d:%05.2f", hours, minutes, seconds);
    }

    private static final double MAX_ACCURACY_POINTS = 100.0;

    private void scoreAgainstActualResult(StrategySimulation simulation, Race race, Driver driver, User user) {
        raceResultService.getActualTotalTimeSeconds(race, driver).ifPresent(actualTime -> {
            double incidentTimeLoss = race.getIncidents().stream()
                    .mapToDouble(i -> i.getTimeLossSeconds() != null ? i.getTimeLossSeconds() : 0.0)
                    .sum();

            double correctedActualTime = actualTime - incidentTimeLoss;
            double delta = simulation.getPredictedTotalTimeSeconds() - correctedActualTime;
            simulation.setDeltaVsActualSeconds(delta);
            awardPoints(user, delta);
        });
    }

    private void awardPoints(User user, double delta) {
        double points = Math.max(0.0, MAX_ACCURACY_POINTS - Math.abs(delta));
        user.setRatingScore(user.getRatingScore() + points);
        userRepository.save(user);

        messagingTemplate.convertAndSend(
                "/topic/leaderboard",
                new LeaderboardEntryDto(user.getId(), user.getUsername(), user.getRatingScore())
        );
    }
}
