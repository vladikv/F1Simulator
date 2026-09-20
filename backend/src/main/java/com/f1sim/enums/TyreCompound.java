package com.f1sim.enums;

/**
 * Tyre compound types used in F1, each with a base degradation rate
 * per lap (in seconds) and an initial pace advantage relative to the
 * hardest compound. These values feed the Strategy Engine's
 * degradation model.
 */
public enum TyreCompound {

    SOFT(0.045, -0.9, 18),
    MEDIUM(0.030, -0.4, 28),
    HARD(0.018, 0.0, 40),
    INTERMEDIATE(0.025, 2.5, 25),
    WET(0.015, 5.0, 30);

    private final double degradationPerLapSeconds;
    private final double paceDeltaSeconds;
    private final int typicalStintLengthLaps;

    TyreCompound(double degradationPerLapSeconds, double paceDeltaSeconds, int typicalStintLengthLaps) {
        this.degradationPerLapSeconds = degradationPerLapSeconds;
        this.paceDeltaSeconds = paceDeltaSeconds;
        this.typicalStintLengthLaps = typicalStintLengthLaps;
    }

    public double getDegradationPerLapSeconds() {
        return degradationPerLapSeconds;
    }

    public double getPaceDeltaSeconds() {
        return paceDeltaSeconds;
    }

    public int getTypicalStintLengthLaps() {
        return typicalStintLengthLaps;
    }

    public boolean isWetWeatherCompound() {
        return this == INTERMEDIATE || this == WET;
    }
}
