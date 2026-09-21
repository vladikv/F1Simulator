// Mirrors backend RaceSummaryDto 1-to-1 — same reasoning as strategy.model.ts.
export interface RaceSummary {
    id: number;
    grandPrixName: string;
    season: number;
    circuitName: string;
    country: string;
    raceDateTime: string; // LocalDateTime serializes as ISO string over JSON
    status: string;
    totalLaps: number | null;
}

export interface WeatherWindow {
    startLap: number;
    endLap: number;
}

export interface RaceDriver {
    id: number;
    fullName: string;
    driverCode: string;
    teamName: string;
    permanentNumber: number | null;
}