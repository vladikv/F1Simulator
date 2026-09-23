import { Component, OnDestroy, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { LeaderboardApiService } from '../../core/services/leaderboard-api.service';
import { CircuitApiService } from '../../core/services/circuit-api.service';
import { LeaderboardEntry } from '../../core/models/leaderboard.model';
import { CircuitSummary } from '../../core/models/race.model';

@Component({
    selector: 'app-leaderboard',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './leaderboard.component.html',
    styleUrl: './leaderboard.component.scss'
})
export class LeaderboardComponent implements OnDestroy {
    private readonly api = inject(LeaderboardApiService);
    private readonly circuitApi = inject(CircuitApiService);
    private liveSub?: Subscription;

    readonly circuits = signal<CircuitSummary[]>([]);
    readonly selectedCircuitId = signal<number | null>(null);
    readonly entries = signal<LeaderboardEntry[]>([]);
    readonly isLoading = signal(true);

    constructor() {
        this.circuitApi.getCircuits().subscribe(list => {
            this.circuits.set(list);
            this.selectedCircuitId.set(list[0]?.id ?? null);
        });

        // Re-fetches the leaderboard and re-subscribes to live updates
        // every time the selected circuit changes.
        effect(() => {
            const circuitId = this.selectedCircuitId();
            if (circuitId === null) return;

            this.isLoading.set(true);
            this.liveSub?.unsubscribe();

            this.api.getLeaderboard(circuitId).subscribe(list => {
                this.entries.set(list);
                this.isLoading.set(false);
            });

            this.liveSub = this.api.liveUpdates(circuitId).subscribe(updated => {
                this.entries.update(list => {
                    const exists = list.some(e => e.userId === updated.userId);
                    const merged = exists
                        ? list.map(e => (e.userId === updated.userId ? updated : e))
                        : [...list, updated];
                    return merged.sort((a, b) => a.bestAbsDeltaSeconds - b.bestAbsDeltaSeconds);
                });
            });
        }, { allowSignalWrites: true });
    }

    ngOnDestroy(): void {
        this.liveSub?.unsubscribe();
    }
}