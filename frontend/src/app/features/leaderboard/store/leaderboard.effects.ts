import { Injectable, inject } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { Store } from '@ngrx/store';
import { switchMap, tap, withLatestFrom, filter } from 'rxjs/operators';
import { CircuitApiService } from '../../../core/services/circuit-api.service';
import { LeaderboardApiService } from '../../../core/services/leaderboard-api.service';
import * as LeaderboardActions from './leaderboard.actions';
import { selectSelectedCircuitId } from './leaderboard.selectors';

@Injectable()
export class LeaderboardEffects {
    private readonly actions$ = inject(Actions);
    private readonly store = inject(Store);
    private readonly circuitApi = inject(CircuitApiService);
    private readonly leaderboardApi = inject(LeaderboardApiService);

    // Listens for loadCircuits, calls the API, dispatches the success action
    // with whatever came back. createEffect + ofType is how an Effect
    // "subscribes" to one specific kind of Action.
    loadCircuits$ = createEffect(() =>
        this.actions$.pipe(
            ofType(LeaderboardActions.loadCircuits),
            switchMap(() => this.circuitApi.getCircuits()),
            switchMap(circuits => [LeaderboardActions.loadCircuitsSuccess({ circuits })])
        )
    );

    // Fires both right after circuits load (selectCircuit sets the default)
    // and every time the user picks a different track.
    loadLeaderboardOnCircuitChange$ = createEffect(() =>
        this.actions$.pipe(
            ofType(LeaderboardActions.selectCircuit, LeaderboardActions.loadCircuitsSuccess),
            withLatestFrom(this.store.select(selectSelectedCircuitId)),
            filter(([, circuitId]) => circuitId !== null),
            switchMap(([, circuitId]) => this.leaderboardApi.getLeaderboard(circuitId!)),
            switchMap(entries => [LeaderboardActions.loadLeaderboardSuccess({ entries })])
        )
    );

    // Opens the WebSocket connection for the currently selected circuit
    // and forwards every message into the store as an Action, instead of
    // the service pushing straight into a component's signal.
    subscribeToLiveUpdates$ = createEffect(() =>
        this.actions$.pipe(
            ofType(LeaderboardActions.selectCircuit),
            switchMap(({ circuitId }) => this.leaderboardApi.liveUpdates(circuitId)),
            switchMap(entry => [LeaderboardActions.leaderboardEntryUpdated({ entry })])
        )
    );
}