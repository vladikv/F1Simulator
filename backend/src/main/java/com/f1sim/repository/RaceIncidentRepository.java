package com.f1sim.repository;

import com.f1sim.entity.RaceIncident;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RaceIncidentRepository extends JpaRepository<RaceIncident, Long> {
    void deleteByRaceId(Long raceId);
}