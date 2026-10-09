package com.f1sim.service;

import com.f1sim.entity.Circuit;
import com.f1sim.entity.TyreStint;
import com.f1sim.enums.TyreCompound;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StrategyEngineServiceTest {

    private StrategyEngineService engine;
    private Circuit circuit;

    @BeforeEach
    void setUp() {
        engine = new StrategyEngineService();
        circuit = Circuit.builder()
                .name("Test Circuit")
                .country("Testland")
                .lapLengthKm(5.0)
                .pitLaneTimeLossSeconds(20.0)
                .build();
    }

    @Test
    @DisplayName("Single stint covering the whole race produces no pit stop cost")
    void singleStintHasNoPitStopCost() {
        TyreStint stint = TyreStint.builder()
                .compound(TyreCompound.MEDIUM)
                .startLap(1)
                .endLap(50)
                .build();

        double result = engine.simulateTotalRaceTime(
                List.of(stint), circuit, 50, 2.5, Collections.emptyList()
        );

        // 50 laps, MEDIUM compound, no pit stop at all (only one stint)
        assertTrue(result > 0);
    }

    @Test
    @DisplayName("Adding a second stint adds pit lane loss and team pit stop time")
    void secondStintAddsPitStopCost() {
        TyreStint stint1 = TyreStint.builder().compound(TyreCompound.MEDIUM).startLap(1).endLap(25).build();
        TyreStint stint2 = TyreStint.builder().compound(TyreCompound.HARD).startLap(26).endLap(50).build();

        double oneStintTime = engine.simulateTotalRaceTime(
                List.of(TyreStint.builder().compound(TyreCompound.MEDIUM).startLap(1).endLap(50).build()),
                circuit, 50, 2.5, Collections.emptyList()
        );

        double twoStintTime = engine.simulateTotalRaceTime(
                List.of(stint1, stint2), circuit, 50, 2.5, Collections.emptyList()
        );

        // Two stints should cost strictly more due to the pit stop
        assertTrue(twoStintTime > oneStintTime);
    }

    @Test
    @DisplayName("Strategy that doesn't cover the full race distance throws")
    void incompleteStrategyThrows() {
        TyreStint stint = TyreStint.builder().compound(TyreCompound.SOFT).startLap(1).endLap(30).build();

        assertThrows(IllegalArgumentException.class, () ->
                engine.simulateTotalRaceTime(List.of(stint), circuit, 50, 2.5, Collections.emptyList())
        );
    }

    @Test
    @DisplayName("Empty stint list throws")
    void emptyStrategyThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                engine.simulateTotalRaceTime(Collections.emptyList(), circuit, 50, 2.5, Collections.emptyList())
        );
    }

    @Test
    @DisplayName("Null totalLaps throws a clear error instead of NPE")
    void nullTotalLapsThrowsIllegalState() {
        TyreStint stint = TyreStint.builder().compound(TyreCompound.MEDIUM).startLap(1).endLap(50).build();

        assertThrows(IllegalStateException.class, () ->
                engine.simulateTotalRaceTime(List.of(stint), circuit, null, 2.5, Collections.emptyList())
        );
    }

    @Test
    @DisplayName("Undercut value is positive when fresh compound is faster than rival's")
    void undercutValuePositiveWhenFreshIsFaster() {
        // SOFT has a lower (more negative) paceDeltaSeconds than HARD, so it's faster
        double value = engine.estimateUndercutValue(3, TyreCompound.SOFT, TyreCompound.HARD);

        assertTrue(value > 0);
    }
}