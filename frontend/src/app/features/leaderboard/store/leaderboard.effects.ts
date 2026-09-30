import { Injectable, inject } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { Store } from '@ngrx/store';
import { switchMap, withLatestFrom, filter } from 'rxjs/operators';
import { CircuitApiService } from '../../../core/services/circuit-api.service';
import { LeaderboardApiService } from '../../../core/services/leaderboard-api.service';
import { RaceApiService } from '../../../core/services/race-api.service';
import * as LeaderboardActions from './leaderboard.actions';
import { selectSelectedCircuitId, selectSelectedSeason } from './leaderboard.selectors';

@Injectable()
export class LeaderboardEffects {
    private readonly actions$ = inject(Actions);
    private readonly store = inject(Store);
    private readonly circuitApi = inject(CircuitApiService);
    private readonly leaderboardApi = inject(LeaderboardApiService);
    private readonly raceApi = inject(RaceApiService);

    loadCircuits$ = createEffect(() =>
        this.actions$.pipe(
            ofType(LeaderboardActions.loadCircuits),
            switchMap(() => this.circuitApi.getCircuits()),
            switchMap(circuits => [LeaderboardActions.loadCircuitsSuccess({ circuits })])
        )
    );

    loadSeasons$ = createEffect(() =>
        this.actions$.pipe(
            ofType(LeaderboardActions.loadCircuits),
            switchMap(() => this.raceApi.getSeasons()),
            switchMap(seasons => [LeaderboardActions.loadSeasonsSuccess({ seasons })])
        )
    );

    loadLeaderboardOnChange$ = createEffect(() =>
        this.actions$.pipe(
            ofType(
                LeaderboardActions.selectCircuit,
                LeaderboardActions.selectSeason,
                LeaderboardActions.loadCircuitsSuccess,
                LeaderboardActions.loadSeasonsSuccess
            ),
            withLatestFrom(
                this.store.select(selectSelectedCircuitId),
                this.store.select(selectSelectedSeason)
            ),
            filter(([, circuitId]) => circuitId !== null),
            switchMap(([, circuitId, season]) =>
                this.leaderboardApi.getLeaderboard(circuitId!, season ?? undefined)
            ),
            switchMap(entries => [LeaderboardActions.loadLeaderboardSuccess({ entries })])
        )
    );

    subscribeToLiveUpdates$ = createEffect(() =>
        this.actions$.pipe(
            ofType(LeaderboardActions.selectCircuit),
            switchMap(({ circuitId }) => this.leaderboardApi.liveUpdates(circuitId)),
            switchMap(entry => [LeaderboardActions.leaderboardEntryUpdated({ entry })])
        )
    );
}