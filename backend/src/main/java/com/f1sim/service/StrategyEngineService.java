package com.f1sim.service;

import com.f1sim.entity.Circuit;
import com.f1sim.entity.TyreStint;
import com.f1sim.enums.TyreCompound;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Core simulation engine that turns a proposed strategy (a sequence
 * of tyre stints) into a predicted total race time.
 *
 * Model overview:
 *  - Each compound has a base pace delta relative to the Hard tyre.
 *  - Degradation accumulates linearly per lap within a stint (a
 *    simplification of the real quadratic degradation curves, kept
 *    linear so it stays transparent and tunable from the UI).
 *  - Every pit stop costs: pit lane time loss (circuit-specific) +
 *    team-specific pit crew stop time.
 *  - Undercut/overcut value is estimated by comparing the total
 *    time lost to fresh-tyre pace gained in the following laps.
 */
@Service
public class StrategyEngineService {

    private static final double BASE_LAP_TIME_SECONDS = 90.0;

    /**
     * Computes the predicted total race time for a full strategy.
     *
     * @param stints          ordered stints covering the full race distance
     * @param circuit         circuit providing pit lane loss and lap count
     * @param teamPitStopTime team-specific average pit stop duration in seconds
     * @return predicted total race time in seconds
     */
    public double simulateTotalRaceTime(List<TyreStint> stints, Circuit circuit, Integer totalLaps, double teamPitStopTime) {
        if (totalLaps == null) {
            throw new IllegalStateException("Race has no total lap count set — cannot validate strategy coverage");
        }
        validateStintsCoverRace(stints, totalLaps);

        double totalTime = 0.0;
        for (int i = 0; i < stints.size(); i++) {
            TyreStint stint = stints.get(i);
            totalTime += simulateStintTime(stint);

            boolean isLastStint = i == stints.size() - 1;
            if (!isLastStint) {
                totalTime += circuit.getPitLaneTimeLossSeconds() + teamPitStopTime;
            }
        }
        return totalTime;
    }

    /**
     * Simulates the time taken to complete a single stint, applying
     * per-lap tyre degradation on top of the compound's base pace.
     */
    private double simulateStintTime(TyreStint stint) {
        TyreCompound compound = stint.getCompound();
        int stintLength = stint.getEndLap() - stint.getStartLap() + 1;

        double stintTime = 0.0;
        for (int lap = 1; lap <= stintLength; lap++) {
            double degradationPenalty = compound.getDegradationPerLapSeconds() * (lap - 1);
            stintTime += BASE_LAP_TIME_SECONDS + compound.getPaceDeltaSeconds() + degradationPenalty;
        }
        return stintTime;
    }

    /**
     * Estimates the net time value of pitting a given number of laps
     * earlier than a rival (undercut) or later (overcut, negative input).
     * Positive result = the earlier stop gains time.
     */
    public double estimateUndercutValue(int lapsEarlier, TyreCompound freshCompound, TyreCompound rivalCompound) {
        double freshTyreAdvantagePerLap =
                (rivalCompound.getPaceDeltaSeconds() - freshCompound.getPaceDeltaSeconds());
        return freshTyreAdvantagePerLap * lapsEarlier;
    }

    private void validateStintsCoverRace(List<TyreStint> stints, int totalLaps) {
        if (stints.isEmpty()) {
            throw new IllegalArgumentException("Strategy must contain at least one stint");
        }
        int coveredLaps = stints.get(stints.size() - 1).getEndLap();
        if (coveredLaps != totalLaps) {
            throw new IllegalArgumentException(
                    "Strategy stints must cover the full race distance: expected " + totalLaps
                            + " laps, got " + coveredLaps);
        }
    }
}
