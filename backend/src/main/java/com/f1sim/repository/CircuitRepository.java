package com.f1sim.repository;

import com.f1sim.entity.Circuit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CircuitRepository extends JpaRepository<Circuit, Long> {
    Optional<Circuit> findByExternalCircuitKey(Integer externalCircuitKey);
}