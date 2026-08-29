package com.f1sim.repository;

import com.f1sim.entity.Race;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RaceRepository extends JpaRepository<Race, Long> {
    Optional<Race> findByExternalSessionKey(String externalSessionKey);
    List<Race> findByStatus(Race.RaceStatus status);
}