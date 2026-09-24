import { createAction, props } from '@ngrx/store';
import { CircuitSummary } from '../../../core/models/race.model';
import { LeaderboardEntry } from '../../../core/models/leaderboard.model';

export const loadCircuits = createAction('[Leaderboard] Load Circuits');
export const loadCircuitsSuccess = createAction(
    '[Leaderboard] Load Circuits Success',
    props<{ circuits: CircuitSummary[] }>()
);

export const selectCircuit = createAction(
    '[Leaderboard] Select Circuit',
    props<{ circuitId: number }>()
);

export const loadLeaderboardSuccess = createAction(
    '[Leaderboard] Load Leaderboard Success',
    props<{ entries: LeaderboardEntry[] }>()
);

export const leaderboardEntryUpdated = createAction(
    '[Leaderboard] Entry Updated (WebSocket)',
    props<{ entry: LeaderboardEntry }>()
);