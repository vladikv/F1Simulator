package com.f1sim.service;

import com.f1sim.entity.Race;
import com.f1sim.entity.StrategySimulation;
import com.f1sim.entity.User;
import com.f1sim.repository.RaceRepository;
import com.f1sim.repository.StrategySimulationRepository;
import com.f1sim.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Catches up simulations submitted before their race finished. At submit
 * time there's no actual result to compare against yet — this job
 * periodically re-checks races that have since become FINISHED and
 * scores any of their simulations still missing a delta.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RaceResultSyncJob {

    private static final double MAX_ACCURACY_POINTS = 100.0;

    private final RaceRepository raceRepository;
    private final StrategySimulationRepository simulationRepository;
    private final UserRepository userRepository;
    private final RaceResultService raceResultService;

    // 30 min is plenty for a portfolio project — results don't change
    // once a race is FINISHED, so faster polling wouldn't help.
    @Scheduled(fixedRate = 30 * 60 * 1000)
    @Transactional
    public void scoreFinishedRaceSimulations() {
        List<Race> finishedRaces = raceRepository.findByStatus(Race.RaceStatus.FINISHED);

        for (Race race : finishedRaces) {
            List<StrategySimulation> pending =
                    simulationRepository.findByRaceAndDeltaVsActualSecondsIsNull(race);

            for (StrategySimulation simulation : pending) {
                raceResultService.getActualTotalTimeSeconds(race, simulation.getDriver())
                        .ifPresent(actualTime -> {
                            double delta = simulation.getPredictedTotalTimeSeconds() - actualTime;
                            simulation.setDeltaVsActualSeconds(delta);
                            simulationRepository.save(simulation);

                            User user = simulation.getUser();
                            userRepository.save(user);
                        });
            }
        }

        if (!finishedRaces.isEmpty()) {
            log.info("Race result sync job checked {} finished races for pending simulations", finishedRaces.size());
        }
    }
}