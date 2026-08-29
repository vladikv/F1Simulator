package com.f1sim.repository;

import com.f1sim.entity.Race;
import com.f1sim.entity.StrategySimulation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StrategySimulationRepository extends JpaRepository<StrategySimulation, Long> {
    List<StrategySimulation> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<StrategySimulation> findByRaceAndDeltaVsActualSecondsIsNull(Race race);
}
