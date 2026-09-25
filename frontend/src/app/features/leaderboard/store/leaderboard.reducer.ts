import { createReducer, on } from '@ngrx/store';
import { CircuitSummary } from '../../../core/models/race.model';
import { LeaderboardEntry } from '../../../core/models/leaderboard.model';
import * as LeaderboardActions from './leaderboard.actions';

export interface LeaderboardState {
    circuits: CircuitSummary[];
    selectedCircuitId: number | null;
    entries: LeaderboardEntry[];
    isLoading: boolean;
}

export const initialState: LeaderboardState = {
    circuits: [],
    selectedCircuitId: null,
    entries: [],
    isLoading: true
};

export const leaderboardReducer = createReducer(
    initialState,

    on(LeaderboardActions.loadCircuitsSuccess, (state, { circuits }) => ({
        ...state,
        circuits,
        selectedCircuitId: circuits[0]?.id ?? null
    })),

    on(LeaderboardActions.selectCircuit, (state, { circuitId }) => ({
        ...state,
        selectedCircuitId: circuitId,
        isLoading: true
    })),

    on(LeaderboardActions.loadLeaderboardSuccess, (state, { entries }) => ({
        ...state,
        entries,
        isLoading: false
    })),

    on(LeaderboardActions.leaderboardEntryUpdated, (state, { entry }) => {
        const exists = state.entries.some(e => e.userId === entry.userId);
        const merged = exists
            ? state.entries.map(e => (e.userId === entry.userId ? entry : e))
            : [...state.entries, entry];

        return {
            ...state,
            entries: merged.sort((a, b) => a.bestAbsDeltaSeconds - b.bestAbsDeltaSeconds)
        };
    })
);