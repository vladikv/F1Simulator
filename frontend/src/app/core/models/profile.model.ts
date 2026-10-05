import { TyreCompound } from './strategy.model';

export interface StintSummary {
    compound: TyreCompound;
    startLap: number;
    endLap: number;
}

export interface ProfileSimulation {
    id: number;
    grandPrixName: string;
    season: number;
    driverCode: string;
    createdAt: string;
    predictedTotalTimeSeconds: number;
    deltaVsActualSeconds: number | null;
    stints: StintSummary[];
}