package com.f1sim.repository;

import com.f1sim.entity.RaceWeatherWindow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RaceWeatherWindowRepository extends JpaRepository<RaceWeatherWindow, Long> {
    List<RaceWeatherWindow> findByRaceId(Long raceId);
    void deleteByRaceId(Long raceId);
}