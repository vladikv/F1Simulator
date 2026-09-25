import { createFeatureSelector, createSelector } from '@ngrx/store';
import { LeaderboardState } from './leaderboard.reducer';

export const selectLeaderboardState = createFeatureSelector<LeaderboardState>('leaderboard');

export const selectCircuits = createSelector(
    selectLeaderboardState,
    state => state.circuits
);

export const selectSelectedCircuitId = createSelector(
    selectLeaderboardState,
    state => state.selectedCircuitId
);

export const selectEntries = createSelector(
    selectLeaderboardState,
    state => state.entries
);

export const selectIsLoading = createSelector(
    selectLeaderboardState,
    state => state.isLoading
);