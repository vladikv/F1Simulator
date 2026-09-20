import { Component, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { LeaderboardApiService } from '../../core/services/leaderboard-api.service';
import { LeaderboardEntry } from '../../core/models/leaderboard.model';

@Component({
    selector: 'app-leaderboard',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './leaderboard.component.html',
    styleUrl: './leaderboard.component.scss'
})
export class LeaderboardComponent implements OnDestroy {
    private readonly api = inject(LeaderboardApiService);
    private liveSub?: Subscription;

    readonly entries = signal<LeaderboardEntry[]>([]);
    readonly isLoading = signal(true);

    constructor() {
        this.api.getLeaderboard().subscribe(list => {
            this.entries.set(list);
            this.isLoading.set(false);
        });

        this.liveSub = this.api.liveUpdates().subscribe(updated => {
            this.entries.update(list => {
                const exists = list.some(e => e.userId === updated.userId);
                const merged = exists
                    ? list.map(e => (e.userId === updated.userId ? updated : e))
                    : [...list, updated];
                return merged.sort((a, b) => b.ratingScore - a.ratingScore);
            });
        });
    }

    ngOnDestroy(): void {
        this.liveSub?.unsubscribe();
    }
}