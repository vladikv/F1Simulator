package com.f1sim.service;

import com.f1sim.entity.Circuit;
import com.f1sim.entity.RaceWeatherWindow;
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
    private static final double DRY_TYRES_IN_RAIN_PENALTY_SECONDS = 8.0;
    private static final double WET_TYRES_ON_DRY_TRACK_PENALTY_SECONDS = 2.0;

    public double simulateTotalRaceTime(
            List<TyreStint> stints,
            Circuit circuit,
            Integer totalLaps,
            double teamPitStopTime,
            List<RaceWeatherWindow> weatherWindows
    ) {
        if (totalLaps == null) {
            throw new IllegalStateException("Race has no total lap count set — cannot validate strategy coverage");
        }
        validateStintsCoverRace(stints, totalLaps);

        double totalTime = 0.0;
        for (int i = 0; i < stints.size(); i++) {
            TyreStint stint = stints.get(i);
            totalTime += simulateStintTime(stint, weatherWindows);

            boolean isLastStint = i == stints.size() - 1;
            if (!isLastStint) {
                totalTime += circuit.getPitLaneTimeLossSeconds() + teamPitStopTime;
            }
        }
        return totalTime;
    }

    private double simulateStintTime(TyreStint stint, List<RaceWeatherWindow> weatherWindows) {
        TyreCompound compound = stint.getCompound();

        double stintTime = 0.0;
        for (int lap = stint.getStartLap(); lap <= stint.getEndLap(); lap++) {
            int lapIndexInStint = lap - stint.getStartLap();
            double degradationPenalty = compound.getDegradationPerLapSeconds() * lapIndexInStint;

            double weatherPenalty = weatherMismatchPenalty(compound, lap, weatherWindows);

            stintTime += BASE_LAP_TIME_SECONDS + compound.getPaceDeltaSeconds() + degradationPenalty + weatherPenalty;
        }
        return stintTime;
    }

    private double weatherMismatchPenalty(TyreCompound compound, int lap, List<RaceWeatherWindow> weatherWindows) {
        boolean isWetLap = weatherWindows.stream()
                .anyMatch(w -> lap >= w.getStartLap() && lap <= w.getEndLap());

        if (isWetLap && !compound.isWetWeatherCompound()) {
            return DRY_TYRES_IN_RAIN_PENALTY_SECONDS;
        }
        if (!isWetLap && compound.isWetWeatherCompound()) {
            return WET_TYRES_ON_DRY_TRACK_PENALTY_SECONDS;
        }
        return 0.0;
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
