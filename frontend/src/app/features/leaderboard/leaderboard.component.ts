import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Store } from '@ngrx/store';
import * as LeaderboardActions from './store/leaderboard.actions';
import { selectCircuits, selectSelectedCircuitId, selectEntries, selectIsLoading } from './store/leaderboard.selectors';

@Component({
    selector: 'app-leaderboard',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './leaderboard.component.html',
    styleUrl: './leaderboard.component.scss'
})
export class LeaderboardComponent implements OnInit {
    private readonly store = inject(Store);

    readonly circuits = this.store.selectSignal(selectCircuits);
    readonly selectedCircuitId = this.store.selectSignal(selectSelectedCircuitId);
    readonly entries = this.store.selectSignal(selectEntries);
    readonly isLoading = this.store.selectSignal(selectIsLoading);

    ngOnInit(): void {
        this.store.dispatch(LeaderboardActions.loadCircuits());
    }

    onCircuitChange(circuitId: number): void {
        this.store.dispatch(LeaderboardActions.selectCircuit({ circuitId }));
    }
}